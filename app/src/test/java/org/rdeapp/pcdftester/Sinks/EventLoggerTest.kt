package org.rdeapp.pcdftester.Sinks

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before

import org.junit.Test
import pcdfEvent.EventType
import pcdfEvent.PCDFEvent
import java.io.File

class EventLoggerTest {
    private var inputChannel: ReceiveChannel<PCDFEvent> = Channel<PCDFEvent>(10000)
    private lateinit var eventLogger: EventLogger
    private  lateinit var file: File
    private lateinit var obdEvent: PCDFEvent

    @Before
    fun setUp() {
        file = File("test.json")
        obdEvent = PCDFEvent("OBDTest", EventType.OBD_RESPONSE, System.nanoTime())
    }
    @After
    fun tearDown() {
        file.deleteRecursively()
    }

    @Test
    fun testStartLoggingIntermediate()  {
        GlobalScope.launch {
            eventLogger.startLogging(file, true)
        }.invokeOnCompletion {
            val contents = file.readText()
            assertEquals(true, contents.contains("INTERMEDIATE"))
        }
    }

    @Test
    fun testStartLoggingPersistent()  {
        GlobalScope.launch {
            eventLogger.startLogging(file, false)
        }.invokeOnCompletion {
            val contents = file.readText()
            assert(contents.contains("PERSISTENT"))
        }
    }
}
