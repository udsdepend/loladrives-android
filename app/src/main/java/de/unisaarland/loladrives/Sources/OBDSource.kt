package de.unisaarland.loladrives.Sources

import de.unisaarland.loladrives.Constants
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import pcdfEvent.PCDFEvent
import pcdfEvent.events.ErrorEvent
import pcdfEvent.events.obdEvents.OBDCommand
import pcdfEvent.events.obdEvents.OBDCommand.*
import pcdfEvent.events.obdEvents.OBDEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.singleComponentEvents.FuelTypeEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.singleComponentEvents.SupportedPidsEvent
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.concurrent.locks.ReentrantLock

const val VERBOSITY_MODE = false

/**
 * Main class for communication with the OBD-Adapter.
 *
 * @property input InputStream to receive OBD data bits (mostly bluetooth).
 * @property output OutputStream to send OBD data bits (mostly bluetooth).
 * @property outputChannel Channel to send the PCDFEvents, made up from received OBD data to.
 * @property commandList List of OBD commands to be sent to the OutputStream (car).
 */
class OBDSource(
    private var input: InputStream?,
    private var output: OutputStream?,
    private val outputChannel: SendChannel<PCDFEvent>,
    private var commandList: MutableList<OBDCommand>,
    private val muuid: UUID
) {
    // Map which helps us to remember the number of lines an answer for a certain OBDCommand consists of (speed up).
    private var numberOfLines: MutableMap<OBDCommand, Int?> = mutableMapOf()

    // Supported PIDs of the connected car.
    private var supportedCommands: MutableList<OBDCommand> = mutableListOf()

    // Subset of the commandList which is unsupported.
    private var notSupported: MutableList<OBDCommand> = mutableListOf()
    private var running = false

    //The used CAN protocol, ISO 15765-4 (SP6) by default, ISO 15765-4 (SP7) used e.g. by Daimler
    private var protocol = Protocol.SP6
    private val headerLength: Int
        get() {
            return when (protocol) {
                Protocol.SP6 -> 3
                Protocol.SP7 -> 8
            }
        }

    private var ioLock = ReentrantLock()

    init {
        for (command in OBDCommand.values()) {
            numberOfLines[command] = null
        }
    }

    /**
     * Start communication with the OBD-Adapter. Performs commands selected for tracking in the current profile.
     */
    suspend fun start() {
        running = true

        // Track events that will not change over time just a single time.
        val singleTimeEvents = commandList.intersect(Constants.SINGLE_TIME_EVENTS).toList()
        commandList = commandList.minus(singleTimeEvents).toMutableList()
        performCommands(singleTimeEvents)

        // Perform commands selected for tracking in a cycle.
        while (running) {
            performCommands(commandList)
        }
    }

    /**
     * Stop communication with the OBD-Adapter.
     */
    fun stop() {
        running = false
    }

    /**
     * Sends commands to the OBD-Adapter and responses to the [outputChannel] as PCDFEvent.
     * If something goes wrong, we send an Error-Event.
     *
     * @param commandList List of OBD-Commands to send to the OBD-Adapter.
     */
    private suspend fun performCommands(commandList: List<OBDCommand>) {
        for (command in commandList) {
            try {
                sendOBDCommand(command)
                val responseEvents = receiveOBD(command)
                for (event in responseEvents) {
                    if (event != null)
                        outputChannel.send(event)
                }
            } catch (e: Exception) {
                outputChannel.send(
                    ErrorEvent(
                        Constants.APP_ID,
                        System.nanoTime(),
                        e.toString()
                    )
                )
            }
        }
    }

    /**
     * Sends OBD Command to given OutputStream / OBD-Adapter.
     * @param command: OBD Command to be sent to the ELM.
     */
    private fun sendOBDCommand(command: OBDCommand) {
        var input = "0"
        input += command.mode.toString(16)
        input += " "

        if (command.pid < 16) {
            input += "0"
        }

        input += command.pid.toString(16).toUpperCase(Locale.ROOT)
        // If we already know how much lines the response will consist of, we append this number to speed things up.
        if (numberOfLines[command] != null) {
            input += " "
            input += numberOfLines[command]!!.toString(16)
        }

        input += "\r"
        try {
            if (VERBOSITY_MODE)
                println("SENDING:\n$input")
            ioLock.lock()
            output?.write((input.toByteArray()))
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            ioLock.unlock()
        }
    }

    /**
     * Receives OBD response from the given InputStream / OBD-Adapter.
     *
     * @param command OBDCommand to receive a response for (so we can associate the received number of lines).
     * @return Response as PCDFEvents.
     */
    private fun receiveOBD(command: OBDCommand): MutableList<PCDFEvent?> {
        var response = ""
        try {
            if (input != null) {
                ioLock.lock()
                var nextByte = input!!.read().toByte()
                while (nextByte != '>'.toByte()) {
                    response += nextByte.toChar()
                    nextByte = input!!.read().toByte()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            ioLock.unlock()
        }

        // BMW sends \r\r at the end despite AT L0.
        if (response.endsWith("\r\r")) {
            response = response.substring(0, response.length - 2)
        }

        if (VERBOSITY_MODE)
            println("RECEIVING:\n$response")
        val lines = response.split("\r")
        return if (lines.size == 1) {
            // Single Frame Response
            if (numberOfLines[command] == null) {
                numberOfLines[command] = 1
            }
            mutableListOf(parseOBD(response.replace(" ", ""), command))
        } else {
            // Multi Line Response
            if (numberOfLines[command] == null) {
                // Remember how much lines the response consisted of, to speed things up next time.
                numberOfLines[command] = lines.size
            }
            receiveOBDMultiLine(lines.toMutableList(), command)
        }
    }

    /**
     * Handles OBD multi line responses (e.g. VIN).
     *
     * @param lines Received lines.
     * @param command OBDCommand to receive a response for.
     * @return Response as PCDFEvents.
     */
    private fun receiveOBDMultiLine(
        lines: MutableList<String>,
        command: OBDCommand
    ): MutableList<PCDFEvent?> {
        val list: MutableList<PCDFEvent?> = mutableListOf()
        val iterator = lines.iterator()
        while (iterator.hasNext()) {
            var line = iterator.next()
            line = line.replace(" ", "")
            if (line.isEmpty()) {
                iterator.remove()
                continue
            }
            if (!line.matches("^[a-fA-F0-9]+$".toRegex())) {
                list.add(parseOBD(line, command))
                iterator.remove()
                continue
            }
            val substring = line.substring(headerLength, headerLength + 2)
            if (substring.toInt() <= 8) {
                list.add(parseOBD(line, command))
                iterator.remove()
            }
        }

        if (lines.isNotEmpty()) {
            val eventSet = receiveMultiFrame(lines, command)
            for (event in eventSet) {
                list.add(event)
            }
        }
        return list
    }

    /**
     * Handles OBD multi frame responses.
     *
     * @param lines Received lines.
     * @param command OBDCommand to receive a response for.
     * @return Response as PCDFEvents.
     */
    private fun receiveMultiFrame(
        lines: MutableList<String>,
        command: OBDCommand
    ): MutableList<PCDFEvent?> {
        val headers = mutableSetOf<String>()
        // Add first header.
        headers.add(lines[0].substring(0, headerLength))

        // Add each new header to the set to get all multi frames
        for (line in lines) {
            // var cline = line.replace(" ","")
            if (!(headers.contains(line.substring(0, headerLength)))) {
                headers.add(line.substring(0, headerLength))
            }
        }
        val events = mutableListOf<PCDFEvent?>()
        for (header in headers) {
            val currentResponse = Array(lines.size) { "" }
            var count = 0
            for (line in lines) {
                var l = line.replace(" ", "")
                if (l.startsWith(header)) {
                    count++
                    l = l.substring(headerLength)
                    val pci = l.substring(1, 2).toInt(16)
                    l = l.substring(2)
                    currentResponse[pci] = l
                }
            }
            var response = header

            for (element in currentResponse) {
                response += element
            }
            events.add(parseOBD(response, command))
        }
        return events
    }

    /**
     * Parses OBD response to a list of PCDFEvents in persistent format.
     *
     * @param obdResponse Response to be parsed.
     * @param command Command the response was received for.
     * @return Parsed [PCDFEvent]
     */
    private fun parseOBD(obdResponse: String, command: OBDCommand): PCDFEvent? {
        var response = obdResponse.replace("\r", "")
        if (response.matches("^[a-fA-F0-9]+$".toRegex())) {
            // remove header
            val header = response.substring(0, headerLength)
            response = response.substring(headerLength)
            val pciString = response.substring(0, 2)
            val remainingBytes = pciString.toInt(16) * 2
            response = response.substring(2)
            response = response.substring(0, remainingBytes)

            // check wether the command is a supported pid request
            val pid = response.substring(2, 4).toInt(16)
            return if (pid % 32 == 0) {
                parseSupportedPids(response, header)
            } else {
                OBDEvent(
                    "ECU-$header",
                    System.nanoTime(),
                    response
                )
            }
        } else {
            if (response.isNotEmpty()) {
                return ErrorEvent(
                    Constants.APP_ID,
                    System.nanoTime(),
                    "OBD Communication Error: $command $response"
                )
            }
            return null
        }
    }

    /**
     * Convenience function to receive the connected cars VIN (Vehicle Identification Number).
     * The response is sent to the [outputChannel].
     */
    suspend fun getVIN() {
        sendOBDCommand(VIN)
        val responseEvents = receiveOBD(VIN)
        for (event in responseEvents) {
            if (event != null)
                outputChannel.send(event)
        }
    }

    /**
     * Convenience function to receive the connected vehicles fuel type.
     * The response is sent to the [outputChannel] and returned.
     */
    suspend fun getFuelType(): FuelTypeEvent? {
        sendOBDCommand(FUEL_TYPE)
        val responseEvents = receiveOBD(FUEL_TYPE)
        for (event in responseEvents) {
            if (event != null && event is OBDEvent) {
                val data = event.toIntermediate()
                if (data is FuelTypeEvent) {
                    outputChannel.send(event)
                    return data
                }
            }
        }
        return null
    }

    /**
     * Convenience function to do  a full supported PIDs request (Modes 1 and 9) to get all the supported PIDs of the
     * connected vehicle.
     * The results are sent to the [outputChannel] and used for determining the supported and selected PIDs of the [commandList].
     */
    suspend fun performSupportedPidsCheck() {
        // Do the supported PIDs requests.
        getSupportedPids(1)
        getSupportedPids(9)

        // Find the intersection between supported and selected PIDs, so we do not monitor PIDs which are not supported.
        val supportedAndSelected = commandList.intersect(supportedCommands).toMutableList()
        notSupported = commandList.minus(supportedAndSelected).toMutableList()

        if (notSupported.isNotEmpty()) {
            outputChannel.send(
                ErrorEvent(
                    Constants.APP_ID,
                    System.nanoTime(),
                    "The following OBD-Commands selected for tracking were not supported: " +
                            notSupported.joinToString()
                )
            )
        }
        commandList = supportedAndSelected
    }

    /**
     * Performs a supported PIDs request.
     *
     * @param mode Mode for which to request the supported PIDs (1 or 9).
     * @return List of supported PIDs.
     */
    suspend fun getSupportedPids(mode: Int): List<Int> {
        val suppPids: MutableList<Int> = LinkedList()
        val cmds: MutableList<OBDCommand> = mutableListOf()
        val responseEvents = mutableListOf<PCDFEvent?>()

        when (mode) {
            1 -> {
                cmds.addAll(
                    listOf(
                        SUPPORTED_PIDS_1_00,
                        SUPPORTED_PIDS_1_32,
                        SUPPORTED_PIDS_1_64,
                        SUPPORTED_PIDS_1_96,
                        SUPPORTED_PIDS_1_128,
                        SUPPORTED_PIDS_1_160,
                        SUPPORTED_PIDS_1_192
                    )
                )
            }
            9 -> {
                cmds.add(SUPPORTED_PIDS_9_00)
            }
        }

        for (
        cmd in cmds
        ) {
            sendOBDCommand(cmd)
            delay(50)
            responseEvents.addAll(receiveOBD(cmd))
        }

        for (event in responseEvents) {
            if (event != null && event is OBDEvent) {
                val data = event.toIntermediate()
                if (data is SupportedPidsEvent) {
                    outputChannel.send(event)
                    suppPids.addAll(data.supportedPids)
                }
            }
        }
        return suppPids
    }

    /**
     * Parses a supported PIDs request, adds supported PIDs to supportedCommands list.
     *
     * @param response: supported PIDs response to be parsed.
     * @return Supported PIDs in a persistent PCDFEvent.
     */
    private fun parseSupportedPids(response: String, header: String): PCDFEvent? {
        val mode = response.substring(0, 2).toInt() - 40
        val pid = response.substring(2, 4).toInt(16)
        var data = response.substring(4)

        // Convert data to bit-string.
        data = data.toLong(16).toString(2)

        // Fill up bit string with zeros in case its to short (has to consist of exactly 32 bits).
        while (data.length < 32) {
            data = "0$data"
        }
        // A 1 in the received bit string indicates that the corresponding PID is supported.
        // TODO: replace this through the functionality in the core.
        for (i in 1..31) {
            // 49 == 1 (char)
            if (data[i - 1].toInt() == 49) {
                val command = OBDCommand.getCommand(mode, i + pid)
                if (command != null) {
                    supportedCommands.add(command)
                }
            }
        }

        return OBDEvent(
            "ECU-$header",
            System.nanoTime(),
            response
        )
    }

    /**
     * Sends ELM327 system command to the given OutputStream / OBD-Adapter.
     *
     * @param command : command to be sent, without AT.
     */
    private fun sendELM(command: String) {
        val input = "AT $command\r"
        try {
            ioLock.lock()
            output?.write((input.toByteArray()))
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            ioLock.unlock()
        }
    }

    /**
     * Receives the response to a ELM327 system command.
     */
    private fun receiveELM(): String {
        var response = ""
        try {
            if (input != null) {
                ioLock.lock()
                var nextByte = input!!.read().toByte()
                while (nextByte != '>'.toByte()) {
                    response += nextByte.toChar()
                    nextByte = input!!.read().toByte()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            ioLock.unlock()
        }
        return response.replace(" ", "").replace("\r", "")
    }

    /**
     * Initializes the ELM327 Bluetooth adapter.
     */
    fun initELM() {
        var response = ""

        try {
            ioLock.lock()
            // Reset ELM
            // TODO: change this here since it may make the app freeze
            if (VERBOSITY_MODE) {
                println("Init ELM: reset")
            }
            //while (!response.contains("ELM")) {
            while (response.length == 0) {
                sendELM("Z")
                response = receiveELM()
            }
            println("Response: " + response)
            response = ""

            // Disable Echo
            if (VERBOSITY_MODE) {
                println("Init ELM: disable echo")
            }
            while (!response.contains("OK")) {
                sendELM("E0")
                response = receiveELM()
            }
            println("Response: " + response)
            response = ""

            // disable linefeed, no carriage return characters after every response
            if (VERBOSITY_MODE) {
                println("Init ELM: disable linefeed")
            }
            while (!response.contains("OK")) {
                sendELM("L0")
                response = receiveELM()
            }
            println("Response: " + response)
            response = ""

            // enable headers
            if (VERBOSITY_MODE) {
                println("Init ELM: enable headers")
            }
            while (!response.contains("OK")) {
                sendELM("H1")
                response = receiveELM()
            }

        } finally {
            ioLock.unlock()
        }
    }

    fun initProtocol(): Boolean {
        var validProtocol = checkProtocol(Protocol.SP6)

        if (validProtocol) {
            return true
        }

        validProtocol = checkProtocol(Protocol.SP7)
        return validProtocol
    }

    private fun elmInitProtocol(protocol: Protocol) {
        // set protocol to given CAN protocol
        var response = ""
        while (!response.contains("OK")) {
            sendELM(protocol.command)
            response = receiveELM()
        }
    }

    private fun checkProtocol(protocol: Protocol): Boolean {
        this.protocol = protocol
        elmInitProtocol(protocol)

        // Send 0x01 00 supported PIDs command according to ISO 15765-4 standard's
        // CAN identifier verification procedure
        var validResponse = false

        for (i in 1..5) {
            sendOBDCommand(SUPPORTED_PIDS_1_00)
            val response = receiveOBD(SUPPORTED_PIDS_1_00)
            validResponse = response.fold(
                false,
                { acc, e -> acc or (e != null && e !is ErrorEvent) }) or validResponse
            if (validResponse) {
                break
            }
        }

        return validResponse
    }

    /**
     * Updates the used InputStream and OutputStream after a connection loss.
     */
    fun changeIOStreams(newInput: InputStream, newOutput: OutputStream) {
        try {
            ioLock.lock()
            input = newInput
            output = newOutput
        } finally {
            ioLock.unlock()
        }
    }

    enum class Protocol(val command: String) {
        SP6("SP6"), SP7("SP7")
    }
}
