/*
 * Copyright (c) 2021.
 */

package de.unisaarland.loladrives.events

import kotlinx.coroutines.channels.Channel
import pcdfEvent.PCDFEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.multiComponentEvents.*
import pcdfEvent.events.obdEvents.obdIntermediateEvents.reducedComponentEvents.FuelRateReducedEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.reducedComponentEvents.NOXReducedEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.singleComponentEvents.FuelRateEvent

/**
 * Reduces received events to reduced events (e.g. [NOXReducedEvent]) and automatically performs
 * calculations for one final data value.
 */
class MultiSensorReducer(
    private val inputChannel: Channel<PCDFEvent>
) {
    val outputChannel = Channel<PCDFEvent>(10000)

    fun stop() {
        inputChannel.cancel()
        outputChannel.close()
    }

    suspend fun start() {
        for (event in inputChannel) {
            outputChannel.trySend(reduce(event))
        }
    }

    fun reduce(event: PCDFEvent): PCDFEvent {
        return when (event) {
            //Reduce NOx events
            is NOXSensorEvent -> {
                NOXReducedEvent(
                    event.source,
                    event.timestamp,
                    event.bytes,
                    event.pid,
                    event.mode,
                    event.sensor1_1,
                    event.sensor1_2,
                    event.sensor2_1,
                    event.sensor2_2,
                )
            }
            is NOXSensorCorrectedEvent -> {
                NOXReducedEvent(
                    event.source,
                    event.timestamp,
                    event.bytes,
                    event.pid,
                    event.mode,
                    event.sensor1_1,
                    event.sensor1_2,
                    event.sensor2_1,
                    event.sensor2_2,
                )
            }
            is NOXSensorAlternativeEvent -> {
                NOXReducedEvent(
                    event.source,
                    event.timestamp,
                    event.bytes,
                    event.pid,
                    event.mode,
                    event.sensor1_1,
                    event.sensor1_2,
                    event.sensor2_1,
                    event.sensor2_2,
                )
            }
            is NOXSensorCorrectedAlternativeEvent -> {
                NOXReducedEvent(
                    event.source,
                    event.timestamp,
                    event.bytes,
                    event.pid,
                    event.mode,
                    event.sensor1_1,
                    event.sensor1_2,
                    event.sensor2_1,
                    event.sensor2_2,
                )
            }

            //Reduce FuelRate events
            is FuelRateEvent -> {
                FuelRateReducedEvent(
                    event.source,
                    event.timestamp,
                    event.bytes,
                    event.pid,
                    event.mode,
                    event.engineFuelRate,
                    -1.0,
                )
            }
            is FuelRateMultiEvent -> {
                FuelRateReducedEvent(
                    event.source,
                    event.timestamp,
                    event.bytes,
                    event.pid,
                    event.mode,
                    if (event.engineFuelRate != -1.0) {
                        event.engineFuelRate / 832.0 * 3600.0
                    } else {
                        -1.0
                    },
                    if (event.vehicleFuelRate != -1.0) {
                        event.vehicleFuelRate / 832.0 * 3600.0
                    } else {
                        -1.0
                    }
                )
            }
            else -> {
                event
            }
        }
    }
}

enum class ReducibleEvents {
    NOX, FUEL_RATE
}
