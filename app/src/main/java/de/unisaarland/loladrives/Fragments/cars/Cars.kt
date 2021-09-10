/*
 * Copyright (c) 2021.
 */

package de.unisaarland.loladrives.Fragments.cars

import de.unisaarland.loladrives.MainActivity
import pcdfEvent.events.obdEvents.OBDEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.singleComponentEvents.VINEvent
import serialization.Serializer
import java.io.File

object Cars {

    val carMap: MutableMap<String, Car> = mutableMapOf()
    val carVins: List<String>
        get() {
            return carMap.keys.toList()
        }

    private val serializer = Serializer()

    fun getCarsFromHistory(activity: MainActivity) {
        val path = activity.getExternalFilesDir(null)
        val letDirectory = File(path, "pcdfdata")
        letDirectory.mkdirs()

        val filesArray = letDirectory.listFiles()
        val files = mutableListOf<File>()

        filesArray?.forEach { directory ->
            if (directory.isDirectory) {
                directory.listFiles()?.forEach {
                    if (it.name.contains("ppcdf")) {
                        files.add(it)
                    }
                }
            }
        }

        for (file in files) {
            file.forEachLine { l ->
                val event = serializer.parseToPattern(l).toEvent()
                if (event is OBDEvent) {
                    val iEvent = event.toIntermediate()
                    if (iEvent is VINEvent) {
                        val vin = iEvent.vin
                        if (carMap.containsKey(vin)) {
                            if (carMap[vin]?.files?.contains(file) == false) {
                                carMap[vin]?.files?.add(file)
                            }
                        } else {
                            carMap[vin] = Car(vin)
                            carMap[vin]?.files?.add(file)
                        }
                        return@forEachLine
                    }
                }
            }
        }

        for (car in carMap.values) {
            car.generateBasicData()
        }

    }

}