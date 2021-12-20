package de.unisaarland.loladrives.Sinks

import android.annotation.SuppressLint
import android.widget.TextView
import de.unisaarland.loladrives.Fragments.TrackingFragment.TrackingFragment
import kotlinx.coroutines.channels.ReceiveChannel
import pcdfEvent.PCDFEvent
import pcdfEvent.events.obdEvents.OBDCommand
import pcdfEvent.events.obdEvents.OBDEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.multiComponentEvents.*
import pcdfEvent.events.obdEvents.obdIntermediateEvents.singleComponentEvents.*

/**
 * UI class for the [TrackingFragment].
 * @property inputChannel Channel over which we receive the [PCDFEvent]s to be displayed in the UI.
 * @property labelMap Maps the given commands on to the corresponding label in the UI, so we know which received event
 * should update which TextView.
 * @property fragment The current [TrackingFragment] we are updating.
 */
class TrackingUIUpdater(
    private val inputChannel: ReceiveChannel<PCDFEvent>,
    private val labelMap: Map<OBDCommand, TextView>,
    val fragment: TrackingFragment
) {
    /**
     * Suspending function which receives (blocking) events over the [inputChannel] and updates the UI according to the
     * [labelMap].
     */
    @SuppressLint("SetTextI18n")
    suspend fun start() {
        for (event in inputChannel) {
            // We only display OBD-Events
            if (event is OBDEvent) {
                try {
                    val iEvent = event.toIntermediate()

                    // Get matching TextView in GridView and update its text.
                    val textView = labelMap[OBDCommand.getCommand(iEvent.mode, iEvent.pid)]
                    when (iEvent) {
                        is RPMEvent -> textView?.text = iEvent.rpm.format(0)
                        is SpeedEvent -> textView?.text = iEvent.speed.toString()
                        is VINEvent -> textView?.text = iEvent.vin
                        is MaximumAirFowRateEvent -> textView?.text = iEvent.rate.toString()
                        is MAFAirFlowRateEvent -> textView?.text = iEvent.rate.format(2)
                        is MAFSensorEvent -> textView?.text = iEvent.mafSensorA.format(2)
                        is IntakeAirTemperatureEvent -> textView?.text =
                            iEvent.temperature.toString()
                        is FuelTypeEvent -> textView?.text = iEvent.fueltype
                        is FuelRateEvent -> textView?.text = iEvent.engineFuelRate.format(2)
                        is FuelRateMultiEvent -> textView?.text = when {
                            iEvent.vehicleFuelRate != -1.0 -> iEvent.vehicleFuelRate.format(2)
                            iEvent.engineFuelRate != -1.0 -> iEvent.engineFuelRate.format(2)
                            else -> "-"
                        }
                        is NOXSensorEvent -> textView?.text = formatNox(
                            iEvent.sensor1_1,
                            iEvent.sensor1_2,
                            iEvent.sensor2_1,
                            iEvent.sensor2_2
                        )
                        is NOXSensorCorrectedEvent -> textView?.text = formatNox(
                            iEvent.sensor1_1,
                            iEvent.sensor1_2,
                            iEvent.sensor2_1,
                            iEvent.sensor2_2
                        )
                        is NOXSensorAlternativeEvent -> textView?.text = formatNox(
                            iEvent.sensor1_1,
                            iEvent.sensor1_2,
                            iEvent.sensor2_1,
                            iEvent.sensor2_2
                        )
                        is NOXSensorCorrectedAlternativeEvent -> textView?.text = formatNox(
                            iEvent.sensor1_1,
                            iEvent.sensor1_2,
                            iEvent.sensor2_1,
                            iEvent.sensor2_2
                        )
                        is FuelTankLevelInputEvent -> textView?.text = iEvent.level.format(2)
                        is FuelAirEquivalenceRatioEvent -> textView?.text = iEvent.ratio.format(2)
                        is EngineOilTemperatureEvent -> textView?.text =
                            iEvent.temperature.toString()
                        is EngineExhaustFlowRateEvent -> textView?.text = iEvent.rate.format(2)
                        is EngineCoolantTemperatureEvent -> textView?.text =
                            iEvent.temperature.toString()
                        is EGRErrorEvent -> textView?.text = iEvent.error.format(2)
                        is CommandedEGREvent -> textView?.text = iEvent.commandedEGR.format(2)
                        is AmbientAirTemperatureEvent -> textView?.text =
                            iEvent.temperature.toString()
                        is CatalystTemperature1_1Event -> textView?.text =
                            iEvent.temperature.format(2)
                        is CatalystTemperature1_2Event -> textView?.text =
                            iEvent.temperature.format(2)
                        is CatalystTemperature2_1Event -> textView?.text =
                            iEvent.temperature.format(2)
                        is CatalystTemperature2_2Event -> textView?.text =
                            iEvent.temperature.format(2)
                        is ParticularMatterEvent ->
                            textView?.text = iEvent.sensor1_1.format(2) + " \r " +
                                    (iEvent.sensor2_1.format(2))
                        else -> textView?.text = iEvent.toString()
                    }
                    fragment.adapter.notifyDataSetChanged()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun Double.format(digits: Int) = "%.${digits}f".format(this).replace(",", ".")

    private fun formatNox(sensor1_1: Int, sensor1_2: Int, sensor2_1: Int, sensor2_2: Int): String {
        return if (sensor1_1 != -1) {
            " Sensor 1: $sensor1_1 "
        } else {
            ""
        } +
                if (sensor1_2 != -1) {
                    " Sensor 2: $sensor1_2 "
                } else {
                    ""
                } +
                if (sensor2_1 != -1) {
                    " Sensor 3: $sensor2_1 "
                } else {
                    ""
                } +
                if (sensor2_2 != -1) {
                    " Sensor 4: $sensor2_2 "
                } else {
                    ""
                }
    }
}
