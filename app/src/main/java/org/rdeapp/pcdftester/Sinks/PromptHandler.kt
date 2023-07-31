package org.rdeapp.pcdftester.Sinks

import android.graphics.Color
import android.os.Build
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.fragment.app.FragmentTransaction
import de.unisaarland.loladrives.Fragments.RDE.RDEFragment
import de.unisaarland.loladrives.MainActivity
import de.unisaarland.loladrives.R
import kotlinx.android.synthetic.main.fragment_r_d_e.textViewAnalysis
import kotlinx.android.synthetic.main.fragment_r_d_e.textViewRDEPrompt
import java.util.Locale

/**
 * Class for handling the prompt for improving the driving style.
 * @property fragment The RDEFragment in which the prompt is displayed.
 */
class PromptHandler (
    private val fragment: RDEFragment
) : TextToSpeech.OnInitListener{

    private var tts: TextToSpeech? = TextToSpeech(fragment.requireActivity(), this)
    private var expectedDistance = fragment.distance
    private var trajectoryAnalyser = fragment.trajectoryAnalyser
    private var speedChange: Double = 0.0
    private var drivingStyleText: String = ""
    private var desiredDrivingMode: DrivingMode = DrivingMode.URBAN
    private var sufficientDrivingMode: DrivingMode? = null
    private var currentText: String = ""
    private var promptType: PromptTypes? = null

    /**
     * Update the prompt for improving the driving style according to the received RTLola results.
     */
    fun handlePrompt(totalDistance: Double) {
        // Check if the RDE test is still valid
        handleInvalidRDE()

        // Cases where the RDE test is still valid, but the driver should improve
        if (totalDistance > expectedDistance/3) {
            analyseTrajectory(totalDistance)
        } else {
            val sufficientDrivingMode = trajectoryAnalyser.checkSufficient()
            if (sufficientDrivingMode != null) {
                promptType = PromptTypes.SUFFICIENCY
            }
        }

        generatePrompt()
        currentText = fragment.textViewRDEPrompt.text.toString()
    }

    /**
     * Check if the RDE test is invalid.
     * If so, announce it to the driver, and move to the RDE settings fragment.
     */
    private fun handleInvalidRDE() {
        if (trajectoryAnalyser.checkInvalid()) {
            fragment.textViewRDEPrompt.text = "This RDE test is invalid, and will be stopped now."
            fragment.textViewRDEPrompt.setTextColor(Color.RED)

            // Only speak if the text has changed
            if (currentText != fragment.textViewRDEPrompt.text.toString()) {
                speak()
            }

            Toast.makeText(fragment.requireActivity(),"Exiting...", Toast.LENGTH_LONG).show()

            fragment.requireActivity().supportFragmentManager.beginTransaction().replace(
                R.id.frame_layout,
                (fragment.requireActivity() as MainActivity).rdeSettingsFragment
            ).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit()
        }
    }

    /**
     * Get analysis trajectory using functions from the trajectoryAnalyser class.
     * Set the desired driving mode and speed change.
     * @param totalDistance The total distance travelled so far.
     */
    private fun analyseTrajectory(totalDistance: Double) {
        desiredDrivingMode = trajectoryAnalyser.setDesiredDrivingMode() // set the desired driving mode accrued to the sufficient driving modes so far
        speedChange = trajectoryAnalyser.computeSpeedChange() // get the speed change needed to improve the driving style
    }

    /**
     * Generate the prompt according to the prompt type set from the analysis done on the trajectory.
     * TODO: Add prompt functions for average urban speed, stopping percentage, high speed percentage, very high speed percentage
     */
    private fun generatePrompt() {
        when (promptType) {
            PromptTypes.SUFFICIENCY -> {
                setSufficientPrompt(sufficientDrivingMode!!)
            }
            PromptTypes.DRIVINGSTYLE -> {
                setDrivingStyleText()
                setDrivingStylePrompt(drivingStyleText)
                setDrivingStyleAnalysis(trajectoryAnalyser.computeDuration())
            }
            PromptTypes.AVERAGEURBANSPEED -> {
                fragment.textViewRDEPrompt.text = "Aim for an average speed between 15km/h to 40km/h, if it is safe to do so"
                fragment.textViewRDEPrompt.setTextColor(Color.GREEN)
            }
            PromptTypes.STOPPINGPERCENTAGE -> {
                fragment.textViewRDEPrompt.text = "Aim for a stopping percentage of 10% to 15%, if it is safe to do so"
                fragment.textViewRDEPrompt.setTextColor(Color.GREEN)
            }
            PromptTypes.HIGHSPEEDPERCENTAGE -> {
                fragment.textViewRDEPrompt.text = "Aim for a high speed percentage of 10% to 15%, if it is safe to do so"
                fragment.textViewRDEPrompt.setTextColor(Color.GREEN)
            }
            PromptTypes.VERYHIGHSPEEDPERCENTAGE -> {
                fragment.textViewRDEPrompt.text = "Aim for a very high speed percentage of 3% or less, if it is safe to do so"
                fragment.textViewRDEPrompt.setTextColor(Color.GREEN)
            }
        }

        // Only speak if the text has changed
        if (currentText != fragment.textViewRDEPrompt.text.toString()) {
            speak()
        }
        currentText = fragment.textViewRDEPrompt.text.toString()
    }

    /**
     * Set the prompt for sufficient driving style.
     * @param sufficientDrivingMode The driving mode for which the driving style is sufficient.
     */
    private fun setSufficientPrompt(sufficientDrivingMode: DrivingMode) {
        when (sufficientDrivingMode) {
            DrivingMode.URBAN -> {
                fragment.textViewRDEPrompt.text = "Your driving style is sufficient for urban driving"
                fragment.textViewRDEPrompt.setTextColor(Color.BLACK)
            }
            DrivingMode.RURAL -> {
                fragment.textViewRDEPrompt.text = "Your driving style is sufficient for rural driving"
                fragment.textViewRDEPrompt.setTextColor(Color.BLACK)
            }
            DrivingMode.MOTORWAY -> {
                fragment.textViewRDEPrompt.text = "Your driving style is sufficient for motorway driving"
                fragment.textViewRDEPrompt.setTextColor(Color.BLACK)
            }
        }
    }

    /**
     * Set the text for the prompt TextView according to the driving mode and speed change.
     */
    private fun setDrivingStylePrompt(drivingStyleText: String) {
        // Calculate the speed change needed to improve the driving style
        if (speedChange > 0) {
            fragment.textViewRDEPrompt.text = "Aim for a higher driving speed, if it is safe to do so, $drivingStyleText"
            fragment.textViewRDEPrompt.setTextColor(Color.GREEN)
        } else if (speedChange < 0) {
            fragment.textViewRDEPrompt.text = "Aim for a lower driving speed, if it is safe to do so, $drivingStyleText"
            fragment.textViewRDEPrompt.setTextColor(Color.RED)
        } else {
            fragment.textViewRDEPrompt.text = "Your driving style is good"
            fragment.textViewRDEPrompt.setTextColor(Color.BLACK)
        }
    }

    /**
     * Set the text for the prompt TextView according to the driving mode and speed change.
     */
    private fun setDrivingStyleText() {
        drivingStyleText = when (desiredDrivingMode) {
            DrivingMode.URBAN -> "for more urban driving"
            DrivingMode.RURAL -> "for more rural driving"
            DrivingMode.MOTORWAY -> "for more motorway driving"
        }
    }

    /**
     * Set analysis textview for the driving style prompt.
     * @param duration The duration for which the driver should drive at the desired driving mode.
     */
    private fun setDrivingStyleAnalysis(duration: Double) {
        when (desiredDrivingMode) {
            DrivingMode.URBAN -> {
                fragment.textViewAnalysis.text = "Drive at an average speed of 30 km/h for $duration minutes"
            }
            DrivingMode.RURAL -> {
                fragment.textViewAnalysis.text = "Drive at an average speed of 75 km/h for $duration minutes"
            }
            DrivingMode.MOTORWAY -> {
                fragment.textViewAnalysis.text = "Drive at an average speed of 115 km/h for $duration minutes"
            }
        }

    }

    /**
     * Initialise the Text To Speech engine.
     */
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts!!.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(fragment.requireActivity(),"The Language not supported!", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Speak the text in the RDE prompt TextView.
     */
    private fun speak() {
        val text = fragment.textViewRDEPrompt.text.toString()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts!!.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ID")
        } else {
            Toast.makeText(fragment.requireActivity(), "This SDK version does not support Text To Speech.", Toast.LENGTH_LONG).show()
        }
    }

}