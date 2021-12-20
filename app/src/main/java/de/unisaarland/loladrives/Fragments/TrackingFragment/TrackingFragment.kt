package de.unisaarland.loladrives.Fragments.TrackingFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import de.unisaarland.loladrives.MainActivity
import de.unisaarland.loladrives.R
import de.unisaarland.loladrives.Sinks.TrackingUIUpdater
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_tracking.*
import kotlinx.coroutines.*
import pcdfEvent.events.obdEvents.OBDCommand

/**
 * A simple [Fragment] subclass.
 */
class TrackingFragment : Fragment() {
    lateinit var adapter: TrackingGridAdapter
    var commandTextViews: MutableMap<OBDCommand, TextView> = mutableMapOf()
    lateinit var activity: MainActivity
    private var uiUpdaterJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tracking, container, false)
    }

    @ExperimentalCoroutinesApi
    override fun onStart() {
        activity = requireActivity() as MainActivity
        activity.title_textview.text = getString(R.string.live_monitoring)
        activity.backButton.setImageResource(R.drawable.back_arrow_icon)
        // Initialize GridView with selected events to be tracked
        trackingGridView.numColumns = 1
        adapter = TrackingGridAdapter(
            activity,
            activity.selectedProfile.second.map { it.command }.toTypedArray(),
            this
        )
        trackingGridView.adapter = adapter
        if (activity.tracking) {
            if (activity.rdeOnGoing) {
                stopTrackingButton.isEnabled = false
                stopTrackingButton.visibility = View.VISIBLE
            } else {
                stopTrackingButton.isEnabled = true
                stopTrackingButton.visibility = View.VISIBLE
            }
            startTrackingButton.isEnabled = false
            startTrackingButton.visibility = View.INVISIBLE
            startUiUpdating()
        } else {
            initAndStart()
        }

        startTrackingButton.setOnClickListener {
            initAndStart()
        }

        stopTrackingButton.setOnClickListener {
            if (activity.tracking) {
                stopTracking()
            }
        }

        super.onStart()
    }

    private fun initAndStart() {
        if (!(requireActivity() as MainActivity).bluetoothConnected) {
            val toast = Toast.makeText(
                context,
                "You are not connected to any bluetooth device",
                Toast.LENGTH_LONG
            )
            toast.show()
        } else {
            if (!activity.tracking) {
                startTrackingButton.isEnabled = false
                startTrackingButton.visibility = View.INVISIBLE
                stopTrackingButton.isEnabled = true
                stopTrackingButton.visibility = View.VISIBLE
                try {
                    startTracking()
                } catch (e: Exception) {
                    val toast = Toast.makeText(
                        context,
                        "LolaDrives had problems communicating with your vehicle, make sure your engine is on" +
                                " and please try again",
                        Toast.LENGTH_LONG
                    )
                    toast.show()
                    stopTracking()
                }
            }
        }
    }

    private fun startTracking() {
        if ((requireActivity() as MainActivity).tracking) {
            val toast = Toast.makeText(
                context,
                "Already running an RDE Track",
                Toast.LENGTH_LONG
            )
            toast.show()
        } else {
            activity.startTracking(false)
            if (this.isVisible) {
                startUiUpdating()
            }
        }
    }

    private fun startUiUpdating() {
        val uiupdater = TrackingUIUpdater(
            activity.eventDistributor.registerReceiver(),
            commandTextViews,
            this
        )
        uiUpdaterJob = GlobalScope.launch(Dispatchers.Main) {
            uiupdater.start()
        }
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    fun stopTracking() {
        if ((activity).tracking) {
            stopTrackingButton.isEnabled = false
            stopTrackingButton.visibility = View.INVISIBLE
            startTrackingButton.isEnabled = true
            startTrackingButton.visibility = View.VISIBLE
            runBlocking { (activity).stopTracking() }
        }
    }

    override fun onStop() {
        uiUpdaterJob?.cancel()
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onStop()
    }

    override fun onPause() {
        uiUpdaterJob?.cancel()
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onPause()
    }

    @ExperimentalCoroutinesApi
    override fun onResume() {
        if (activity.tracking) {
            if (uiUpdaterJob?.isActive != true) startUiUpdating()
            startTrackingButton.isEnabled = false
            startTrackingButton.visibility = View.INVISIBLE
            if (activity.rdeOnGoing) {
                stopTrackingButton.isEnabled = false
                stopTrackingButton.visibility = View.VISIBLE
            } else {
                stopTrackingButton.isEnabled = true
                stopTrackingButton.visibility = View.VISIBLE
            }
        }
        super.onResume()
    }
}
