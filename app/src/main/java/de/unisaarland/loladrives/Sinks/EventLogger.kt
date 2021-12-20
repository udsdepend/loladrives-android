package de.unisaarland.loladrives.Sinks

import de.unisaarland.loladrives.Constants
import kotlinx.coroutines.channels.ReceiveChannel
import pcdfEvent.PCDFEvent
import pcdfEvent.events.MetaEvent
import pcdfEvent.events.obdEvents.OBDEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.OBDIntermediateEvent
import serialization.Serializer
import java.io.File

/**
 * Class for logging PCDFEvents to a JSON file.
 * @property inputChannel  Channel over which we receive the [PCDFEvent]s to be logged.
 */
class EventLogger(private val inputChannel: ReceiveChannel<PCDFEvent>) {
    private val serializer = Serializer()
    // TODO: Find real uuid and make UUID App constant

    /**
     * Receives PCDFEvents over the [inputChannel], serializes and logs them to a given file.
     * @param file File to log to.
     * @param intermediate If the events should be logged in the intermediate PCD-Format (with parsed OBD-Bits).
     */
    suspend fun startLogging(file: File, intermediate: Boolean) {
        // Write Meta-Event to begin file.
        file.appendText(
            serializer.generateFromPattern(
                if (intermediate) {
                    MetaEvent(Constants.APP_ID, System.nanoTime(), "INTERMEDIATE", null, "1.0.0")
                } else {
                    MetaEvent(Constants.APP_ID, System.nanoTime(), "PERSISTENT", "1.0.0", null)
                }.getPattern()
            ) + "\r"
        )

        for (event in inputChannel) {
            if (intermediate) {
                if (event is OBDEvent) {
                    try {
                        val str = serializer.generateFromPattern(
                            event.toIntermediate().getPattern()
                        )
                        file.appendText((str + "\r"))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    continue
                }
            } else {
                if (event is OBDIntermediateEvent) {
                    try {
                        val str = serializer.generateFromPattern(
                            OBDEvent(event.source, event.timestamp, event.bytes).getPattern()
                        )
                        file.appendText((str + "\r"))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    continue
                }
            }
            val str = serializer.generateFromPattern(event.getPattern())
            file.appendText(str + "\r")
        }
    }
}
