package de.unisaarland.loladrives.Fragments.RDE

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
import de.unisaarland.loladrives.MainActivity
import de.unisaarland.loladrives.OBDCommunication
import de.unisaarland.loladrives.OBDCommunication.*
import de.unisaarland.loladrives.R
import de.unisaarland.loladrives.Sinks.RDEValidator
import de.unisaarland.loladrives.Sources.GPSSource
import de.unisaarland.loladrives.Sources.OBDSource
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_r_d_e_settings.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import pcdfEvent.PCDFEvent
import pcdfEvent.events.GPSEvent
import pcdfEvent.events.obdEvents.OBDCommand
import pcdfEvent.events.obdEvents.OBDCommand.*

/**
 * A simple [Fragment] subclass.
 */
class RDESettingsFragment : Fragment() {
    private var distance = 83
    private var rdeValidator: RDEValidator? = null
    private lateinit var activity: MainActivity
    private var gpsInit = false

    private lateinit var source: OBDSource

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_r_d_e_settings, container, false)
    }

    @ExperimentalCoroutinesApi
    override fun onStart() {
        super.onStart()
        activity = requireActivity() as MainActivity
        activity.title_textview.text = getString(R.string.rde_settings)
        activity.backButton.setImageResource(R.drawable.back_arrow_icon)
        textViewDistance.text = "$distance km"

        distanceSeekBar.onSeekChangeListener = object : OnSeekChangeListener {
            override fun onSeeking(seekParams: SeekParams) {
                distance = seekParams.progress
                textViewDistance.text = "$distance km"
            }

            override fun onStartTrackingTouch(seekBar: IndicatorSeekBar) {}
            override fun onStopTrackingTouch(seekBar: IndicatorSeekBar) {}
        }

        // Init OBDSource used for NOx Warm-Up-check
        source = OBDSource(
                activity.mBluetoothSocket?.inputStream,
                activity.mBluetoothSocket?.outputStream,
                Channel(10000),
                mutableListOf(),
                activity.mUUID
        )

        activity.checkConnection()
        if (!activity.bluetoothConnected) {
            val toast = Toast.makeText(
                    context,
                    "No Bluetooth OBD-Device Connected",
                    Toast.LENGTH_LONG
            )
            toast.show()
            activity.progressBarBluetooth.visibility = View.INVISIBLE
            activity.onBackPressed()
        } else {
            if (!activity.tracking) {
                // Initialize GPS, start requesting s.t. GPS is ready when the RDE is started
                checkGPS()
                val rdeReady = initRDE()

                if (rdeReady.first == OKAY) {

                    startNOxWarmUp(rdeReady.second)

                    startImageButton.setOnClickListener {
                        activity.rdeFragment.distance = distance.toDouble()

                        activity.supportFragmentManager.beginTransaction().replace(
                                R.id.frame_layout,
                                activity.rdeFragment
                        ).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit()
                    }
                } else {
                    val toast = Toast.makeText(
                            context,
                            "Your car does not support all necessary sensors for an RDE Test, see the above list for an overview.",
                            Toast.LENGTH_LONG
                    )
                    toast.show()
                }
            } else {
                val toast = Toast.makeText(
                        context,
                        "Live Monitoring is already ongoing",
                        Toast.LENGTH_LONG
                )
                toast.show()
                activity.progressBarBluetooth.visibility = View.INVISIBLE
            }
        }
        activity.progressBarBluetooth.visibility = View.INVISIBLE
    }

    private fun checkGPS() {
        GlobalScope.launch {
            val channel = Channel<PCDFEvent>(1000)
            val gpsSource = GPSSource(channel, activity)
            GlobalScope.launch(Dispatchers.Main) { gpsSource.start() }
            for (i in channel) {
                if (i is GPSEvent) {
                    gpsInit = true
                    GlobalScope.launch(Dispatchers.Main) {
                        textViewGPSinit.visibility = View.INVISIBLE
                        imageViewGPS.visibility = View.VISIBLE
                    }
                    break
                }
            }
            GlobalScope.launch(Dispatchers.Main) { gpsSource.stop() }
            channel.close()
        }
    }

    @ExperimentalCoroutinesApi
    private fun initRDE() : Pair<OBDCommunication, List<OBDCommand>> {
        rdeValidator = RDEValidator(
                activity.eventDistributor.registerReceiver(),
                activity
        )
        val rdeReady: Pair<OBDCommunication, List<OBDCommand>> = runBlocking {
            try {
                rdeValidator!!.performSupportedPidsCheck()
            } catch (e: Exception) {
                println("OBD Error: $e")
                Pair(OBD_COMMUNICATION_ERROR, listOf())
            }
        }

        fillCarInfo(rdeReady)
        return rdeReady
    }

    private fun fillCarInfo(rdeReady: Pair<OBDCommunication, List<OBDCommand>>) {
        val result = rdeReady.first
        val suppList = rdeReady.second

        if (result == OBD_COMMUNICATION_ERROR) {
            val toast = Toast.makeText(
                    context,
                    getString(R.string.obd_comm_err),
                    Toast.LENGTH_LONG
            )
            toast.show()
            activity.onBackPressed()
        }

        if (suppList.contains(SPEED)) {
            imageViewSpeed.setImageResource(R.drawable.bt_connected)
        } else {
            imageViewSpeed.setImageResource(R.drawable.bt_not_connected)
        }

        if (suppList.contains(AMBIENT_AIR_TEMPERATURE)) {
            imageViewTemp.setImageResource(R.drawable.bt_connected)
        } else {
            imageViewTemp.setImageResource(R.drawable.bt_not_connected)
        }

        if (suppList.intersect(listOf(NOX_SENSOR, NOX_SENSOR_CORRECTED, NOX_SENSOR_ALTERNATIVE, NOX_SENSOR_CORRECTED_ALTERNATIVE)).isEmpty()) {
            textViewNOXSensorInit.text = getString(R.string.noNox)
            textViewNOXSensorInit.setTextColor(Color.RED)
        }

        var noxCalcString = "No MAF"

        if (suppList.contains(MAF_AIR_FLOW_RATE) || suppList.contains(MAF_AIR_FLOW_RATE_SENSOR)) {
            noxCalcString = "MAF"

            if (suppList.contains(ENGINE_FUEL_RATE) || suppList.contains(ENGINE_FUEL_RATE_MULTI)) {
                noxCalcString += ", Fuel Rate"
            } else {
                if (suppList.contains(FUEL_AIR_EQUIVALENCE_RATIO)) {
                    noxCalcString += ", FAE"
                }
            }
        } else {
            textViewNoxCalc2.setTextColor(Color.RED)
        }

        textViewNoxCalc2.text = noxCalcString
    }

    private fun startNOxWarmUp(suppPIDs: List<OBDCommand>) {
        val commands = mutableListOf(NOX_SENSOR, NOX_SENSOR_ALTERNATIVE, NOX_SENSOR_CORRECTED, NOX_SENSOR_CORRECTED_ALTERNATIVE).intersect(suppPIDs)


        val a = arrayListOf("Nox1", "Nox2")
        val adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_item, a)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinner.adapter = adapter
        spinner.invalidate()

        spinner.setOnItemClickListener { parent, view, position, id ->
            print("Position: " + position + " ID: "+ id)
        }
    }
}
