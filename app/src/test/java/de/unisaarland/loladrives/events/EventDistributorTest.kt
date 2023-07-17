package de.unisaarland.loladrives.events

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class EventDistributorTest {
    val eventDistributor: EventDistributor = EventDistributor()

    @Before
    fun setUp() {
    }

    @Test
    fun testStop() {
        eventDistributor.stop()
        assertTrue(eventDistributor.inputChannel.isClosedForSend)
        val outputChannels = eventDistributor.getOutputChannels()
        assertTrue(outputChannels.isEmpty())
    }

    @Test
    fun testStart() {
        eventDistributor.registerReceiver()
        GlobalScope.launch {
            eventDistributor.start()
        }
        assertFalse(eventDistributor.inputChannel.isClosedForSend)
        val outputChannels = eventDistributor.getOutputChannels()
        assertFalse(outputChannels.isEmpty())
    }

    @Test
    fun testRegisterReceiver() {
        val newChannel = eventDistributor.registerReceiver()
        val outputChannels = eventDistributor.getOutputChannels()
        assertFalse(outputChannels.isEmpty())
        assertEquals(newChannel, outputChannels.last())
    }

    @Test
    fun testUnregisterReceiver() {
        val newChannel = eventDistributor.registerReceiver()
        val outputChannels = eventDistributor.getOutputChannels()
        assertEquals(newChannel, outputChannels.last())
        eventDistributor.unregisterReceiver(newChannel)
        assertFalse(outputChannels.contains(newChannel))
    }
}