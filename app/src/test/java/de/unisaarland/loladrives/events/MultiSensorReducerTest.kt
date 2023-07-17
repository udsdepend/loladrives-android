package de.unisaarland.loladrives.events

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.junit.Test
import org.junit.Assert.*
import pcdfEvent.EventType
import pcdfEvent.PCDFEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.multiComponentEvents.FuelRateMultiEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.multiComponentEvents.NOXSensorAlternativeEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.multiComponentEvents.NOXSensorCorrectedAlternativeEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.multiComponentEvents.NOXSensorCorrectedEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.multiComponentEvents.NOXSensorEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.reducedComponentEvents.FuelRateReducedEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.reducedComponentEvents.NOXReducedEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.singleComponentEvents.FuelRateEvent

class MultiSensorReducerTest {
    private val inputChannel: Channel<PCDFEvent> = Channel<PCDFEvent>(10000)
    val multiSensorReducer: MultiSensorReducer = MultiSensorReducer(inputChannel)

    /**
     * Test that the input and output channels are open after calling start().
     */
    @Test
    fun testStart() {
        GlobalScope.launch {
            multiSensorReducer.start()
        }
        assertFalse(inputChannel.isClosedForSend)
        assertFalse(inputChannel.isClosedForReceive)
        assertFalse(multiSensorReducer.outputChannel.isClosedForReceive)
        assertFalse(multiSensorReducer.outputChannel.isClosedForSend)
    }

    /**
     * Test that the input and output channels are closed after calling stop().
     */
    @Test
    fun testStop() {
        multiSensorReducer.stop()
        assertTrue(inputChannel.isClosedForSend)
        assertTrue(inputChannel.isClosedForReceive)
        assertTrue(multiSensorReducer.outputChannel.isClosedForSend)
        assertTrue(multiSensorReducer.outputChannel.isClosedForReceive)
    }

    /**
     * Test that the correct reduced event is returned when input is a NOx Sensor Event.
     */
    @Test
    fun testReduceNOxSensorEvent() {
        val noxSensorEvent = NOXSensorEvent(
            "SENSOR", System.nanoTime(), "0", 1, 1, -1, -1, -1, -1
        )
        val reducedEvent = multiSensorReducer.reduce(noxSensorEvent)
        assertTrue(reducedEvent is NOXReducedEvent)
    }

    /**
     * Test that the correct reduced event is returned when input is a NOx Sensor Corrected Event.
     */
    @Test
    fun testReduceNOxSensorCorrectedEvent() {
        val noxSensorCorrectedEvent = NOXSensorCorrectedEvent(
            "SENSOR", System.nanoTime(), "0", 1, 1, -1, -1, -1, -1
        )
        val reducedEvent = multiSensorReducer.reduce(noxSensorCorrectedEvent)
        assertTrue(reducedEvent is NOXReducedEvent)
    }

    /**
     * Test that the correct reduced event is returned when input is a NOx Sensor Alternative Event.
     */
    @Test
    fun testReduceNOxSensorAlternativeEvent() {
        val noxSensorAlternativeEvent = NOXSensorAlternativeEvent(
            "SENSOR", System.nanoTime(), "0", 1, 1, -1, -1, -1, -1
        )
        val reducedEvent = multiSensorReducer.reduce(noxSensorAlternativeEvent)
        assertTrue(reducedEvent is NOXReducedEvent)
    }

    /**
     * Test that the correct reduced event is returned when input is a NOx Sensor Corrected Alternative Event.
     */
    @Test
    fun testReduceNOxSensorCorrectedAlternativeEvent() {
        val noxSensorCorrectedAlternativeEvent = NOXSensorCorrectedAlternativeEvent(
            "SENSOR", System.nanoTime(), "0", 1, 1, -1, -1, -1, -1
        )
        val reducedEvent = multiSensorReducer.reduce(noxSensorCorrectedAlternativeEvent)
        assertTrue(reducedEvent is NOXReducedEvent)
    }

    /**
     * Test that the correct reduced event is returned when input is a Fuel Rate Event.
     */
    @Test
    fun testReduceFuelRateEvent() {
        val fuelRateEvent = FuelRateEvent(
            "SENSOR", System.nanoTime(), "0", 1, 1, 0.0)
        val reducedEvent = multiSensorReducer.reduce(fuelRateEvent)
        assertTrue(reducedEvent is FuelRateReducedEvent)
    }

    /**
     * Test that the correct reduced event is returned when input is a Fuel Rate Multi Event.
     */
    @Test
    fun testReduceFuelRateMultiEvent() {
        val fuelRateMultiEvent = FuelRateMultiEvent(
            "SENSOR", System.nanoTime(), "0", 1, 1, 0.0, 0.0
        )
        val reducedEvent = multiSensorReducer.reduce(fuelRateMultiEvent)
        assertTrue(reducedEvent is FuelRateReducedEvent)
    }

    /**
     * Test that the event is not reduced when input is non of the specified Event types.
     */
    @Test
    fun testReduceOtherEvent() {
        val otherEvent = PCDFEvent("SENSOR", EventType.OBD_RESPONSE, System.nanoTime())
        val reducedEvent = multiSensorReducer.reduce(otherEvent)
        assertEquals(otherEvent, reducedEvent)
    }

}