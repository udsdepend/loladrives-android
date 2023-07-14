package de.unisaarland.loladrives.events

import kotlinx.coroutines.ExperimentalCoroutinesApi
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
        // TODO: Implement with suspend function
    }

    @Test
    fun testRegisterReceiver() {
        val newChannel = eventDistributor.registerReceiver()
        val outputChannels = eventDistributor.getOutputChannels()
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