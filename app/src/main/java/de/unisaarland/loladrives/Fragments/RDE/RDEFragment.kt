package de.unisaarland.loladrives.Fragments.RDE

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import de.unisaarland.loladrives.MainActivity
import de.unisaarland.loladrives.OBDCommunication.*
import de.unisaarland.loladrives.R
import de.unisaarland.loladrives.profiles.RDECommand
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_r_d_e.*
import kotlinx.coroutines.*
import org.rdeapp.pcdftester.Sinks.PromptHandler
import org.rdeapp.pcdftester.Sinks.RDEUIUpdater
import org.rdeapp.pcdftester.Sinks.RDEValidator
import org.rdeapp.pcdftester.Sinks.VERBOSITY_MODE
import java.io.File

/**
 * A simple [Fragment] subclass.
 */
class RDEFragment : Fragment() {
    var distance = 83.0
    lateinit var promptHandler: PromptHandler
    lateinit var rdeValidator: RDEValidator
    private var uiUpdaterJob: Job? = null
    private lateinit var activity: MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_r_d_e, container, false)
    }

    @ExperimentalCoroutinesApi
    override fun onStart() {
        activity = requireActivity() as MainActivity
        activity.title_textview.text = getString(R.string.rde)
        activity.backButton.setImageResource(R.drawable.back_arrow_icon)

        initIntervalMarkers(16.0 / distance)
        if (activity.tracking) {
            startUiUpdating()
            stopRDEButton.visibility = View.VISIBLE
        } else {
            checkAndStart()
        }

        stopRDEButton.setOnClickListener {
            stop()
        }

        super.onStart()
    }

    @ExperimentalCoroutinesApi
    private fun checkAndStart() {
        if (!activity.checkPermissionsForeground()) {
            MainActivity.InfoDialog("no_location_permission.html", null).show(
                activity.supportFragmentManager,
                null
            )
            activity.onBackPressed()
            return
        } else if (!activity.bluetoothConnected) {
            val toast = Toast.makeText(
                context,
                "You are not connected to any bluetooth device",
                Toast.LENGTH_LONG
            )
            toast.show()
        } else {
            promptHandler = PromptHandler(this)
            rdeValidator = RDEValidator(
                activity.eventDistributor.registerReceiver(),
                activity
            )

            if (activity.tracking) {
                val toast = Toast.makeText(
                    context,
                    "Already performing RDE Track",
                    Toast.LENGTH_LONG
                )
                toast.show()
            } else {
                GlobalScope.launch(Dispatchers.Main) {
                    // Perform supported Pids check
                    val rdeReady = try {
                        rdeValidator.performSupportedPidsCheck()
                    } catch (e: Exception) {
                        println("OBD Error: $e")
                        OBD_COMMUNICATION_ERROR
                    }
                    when (rdeReady) {
                        OKAY -> {
                            // Init the matching RTLola spec
                            println("Init spec")
                            rdeValidator.initSpec()
                            // Load RDE Profile with supported Pids
                            val commands = mutableListOf<RDECommand>()
                            rdeValidator.rdeProfile.forEach { i -> commands.add(RDECommand.toRDECommand(i)) }
                            rdeValidator.extendedLoggingProfile.forEach { i -> commands.add(RDECommand.toRDECommand(i)) }
                            activity.selectedProfile = Pair(
                                "rde_profile",
                                commands.toTypedArray()
                            )
                            // Start Tracking
                            activity.startTracking(true)
                            activity.rdeOnGoing = true

                            // Start RDE Validation
                            GlobalScope.launch { rdeValidator.startRDETrack() }

                            // Start UI Updating
                            startUiUpdating()

                            // Show stop button
                            stopRDEButton.visibility = View.VISIBLE
                        }
                        INSUFFICIENT_SENSORS, UNSUPPORTED_PROTOCOL -> {
                            MainActivity.InfoDialog("unsupported_car.html", null).show(
                                activity.supportFragmentManager,
                                null
                            )
                            activity.onBackPressed()
                        }
                        OBD_COMMUNICATION_ERROR -> {
                            val toast = Toast.makeText(
                                context,
                                getString(R.string.obd_comm_err),
                                Toast.LENGTH_LONG
                            )
                            toast.show()

                            activity.supportFragmentManager.beginTransaction().replace(
                                R.id.frame_layout,
                                activity.rdeSettingsFragment
                            ).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit()
                        }
                    }
                }
            }
        }
    }

    fun stop() {
        if (this.isVisible) {
            stopRDEButton.visibility = View.INVISIBLE
        }
        runBlocking { activity.stopTracking() }
        activity.rdeFragment = RDEFragment()
        activity.setupDefaultProfile()
        showRDEView()
    }

    fun pause() {
        if (this::rdeValidator.isInitialized) rdeValidator.isPaused = true
    }

    fun resume() {
        if (this::rdeValidator.isInitialized) rdeValidator.isPaused = false
    }

    private fun showRDEView() {
        val letDirectory = File(activity.getExternalFilesDir(null), "pcdfdata")
        val directory = File(letDirectory, activity.currentFile)
        if (directory.isDirectory) {
            val file = File(directory, "${directory.nameWithoutExtension}.ppcdf")
            if (file.exists()) {
                activity.openDetailView(file, true)
            }
        }
    }

    fun initIntervalMarkers(
        lowerBoundary: Double
    ) {
        val width = 0.78
        val offset = 0.11

        val max = 0.5

        val uLow = maxOf(0.29, lowerBoundary) / max
        val uHigh = maxOf(0.44, lowerBoundary) / max
        val rmLow = maxOf(0.23, lowerBoundary) / max
        val rmHigh = maxOf(0.43, lowerBoundary) / max

        gl_u_low.setGuidelinePercent((width * uLow + offset).toFloat())
        gl_u_high.setGuidelinePercent((width * uHigh + offset).toFloat())
        gl_rm_low.setGuidelinePercent((width * rmLow + offset).toFloat())
        gl_rm_high.setGuidelinePercent((width * rmHigh + offset).toFloat())

        val maxNOX = 0.2
        val noxPermitted1 = 0.168
        val noxPermitted2 = 0.12

        val percentage1 = noxPermitted1 / maxNOX
        val percentage2 = noxPermitted2 / maxNOX

        guidelineNOXPermittedMarker168.setGuidelinePercent((width * percentage1 + offset).toFloat())
        guidelineNOXPermittedMarker120.setGuidelinePercent((width * percentage2 + offset).toFloat())
    }

    @ExperimentalCoroutinesApi
    private fun startUiUpdating() {
        val rdeUIUpdater = RDEUIUpdater(rdeValidator.outputChannel.openSubscription(), this)

        uiUpdaterJob = GlobalScope.launch(Dispatchers.Main) {
            rdeUIUpdater.start()
        }
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
        initIntervalMarkers(16.0 / distance)

        if (activity.tracking) {
            if (uiUpdaterJob?.isActive != true) {
                startUiUpdating()
            }
        }

        super.onResume()
    }
}
