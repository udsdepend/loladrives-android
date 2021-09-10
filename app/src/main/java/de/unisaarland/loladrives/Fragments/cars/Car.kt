/*
 * Copyright (c) 2021.
 */

package de.unisaarland.loladrives.Fragments.cars

import de.unisaarland.loladrives.Fragments.cars.Car.Manufacturer.*
import pcdfEvent.events.obdEvents.OBDEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.singleComponentEvents.FuelTypeEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.singleComponentEvents.SupportedPidsEvent
import serialization.Serializer
import java.io.File

class Car(
    val vin: String
) {
    val serializer = Serializer()
    var files = mutableListOf<File>()

    var name: String = "None"
    var fuelType: String = "None"
    lateinit var manufacturer: Manufacturer
    var supportedPids = mutableListOf<Int>()

    init {
        readManufacturer()
    }

    fun generateBasicData() {
        for (file in files) {
            var c = 20
            file.forEachLine { l ->
                if (c == 0) {
                    return@forEachLine
                }
                val event = serializer.parseToPattern(l).toEvent()
                if (event is OBDEvent) {
                    when (val iEvent = event.toIntermediate()) {
                        is FuelTypeEvent -> fuelType = iEvent.fueltype
                        is SupportedPidsEvent -> iEvent.supportedPids.forEach {
                            if (!supportedPids.contains(it)) {
                                supportedPids.add(it)
                            }
                        }
                    }
                }
                c--
            }
        }
    }

    private fun readManufacturer() {
        manufacturer = when (vin.substring(0, 3)) {
            "WBA", "WBS", "WBW", "WBY", "4US" -> BMW

            "AAV", "LFV", "LSV", "MEX", "VWV", "WVG",
            "WVW", "WV1", "WV2", "WV3", "XW8", "YBW",
            "1VW", "2V4", "2V8", "3VW", "8AW", "9BW" -> VW

            "AHT", "LTV", "MBJ", "MHF", "MR0", "NMT", "SB1",
            "TW1", "VNK", "6T1", "8AJ", "93R", "9BR" -> TOYOTA

            "MDH", "MNT", "SJN", "VSK", "VWA", "5N1", "94D" -> NISSAN

            "TRU", "WAU", "WA1", "WUA", "93U", "93V" -> AUDI

            "U5Y", "U6Y", "XWE" -> KIA

            "WP0", "WP1" -> PORSCHE

            else -> {
                when (vin.substring(0, 2)) {
                    "1N", "3N", "JN" -> NISSAN

                    "JT", "2T" -> TOYOTA

                    "KN" -> KIA
                    else -> NOT_MAPPED
                }
            }

        }
    }

    enum class Manufacturer {
        BMW, VW, NISSAN, AUDI, PORSCHE, KIA, TOYOTA, NOT_MAPPED
    }
}