/*
 * Copyright (c) 2021.
 */
package de.unisaarland.loladrives.cache

import de.unisaarland.loladrives.MainActivity
import de.unisaarland.loladrives.Sinks.RDEValidator
import de.unisaarland.pcdfanalyser.analysers.Analyser
import de.unisaarland.pcdfanalyser.eventStream.EventStream

class NOxAnalyser(eventStream: EventStream, activity: MainActivity) :
    Analyser<Double?>(eventStream) {
    private val validator = RDEValidator(null, activity)
    var prepared = false
    var analysisResults: DoubleArray? = null

    override fun analyse(): Double? {
        prepare()
        return analysisResults?.get(16)
    }

    override fun analysisIsAvailable(): Boolean {
        prepare()
        return analysisResults != null
    }

    private fun prepare() {
        if (!prepared) {
            try {
                analysisResults = getResults()
            } catch (e: Exception) {
                return
            }
            prepared = true
        }
    }

    private fun getResults(): DoubleArray {
        return validator.monitorOffline(eventStream.iterator())
    }
}