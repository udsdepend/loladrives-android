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
    private var currentText: String = ""

    /**
     * Update the prompt for improving the driving style according to the received RTLola results.
     */
    fun handlePrompt(
        totalDistance: Double
        ) {
        // Check if the RDE test is still valid
        handleInvalidRDE()

        // Cases where the RDE test is still valid, but the driver should improve
        if (totalDistance > expectedDistance/3) {
            desiredDrivingMode = trajectoryAnalyser.setDesiredDrivingMode()
            setDrivingStyleText() // Set the text for the prompt according to the desired driving mode

            // Calculate the speed change and duration for the desired driving mode
            trajectoryAnalyser.computeSpeedChange()
            trajectoryAnalyser.computeDuration()

            setPromptText(drivingStyleText)

            // Only speak if the text has changed
            if (currentText != fragment.textViewRDEPrompt.text.toString()) {
                speak()
            }
        } else {
            // Only 1 driving style can be sufficient in the first half of the test.
            fragment.textViewRDEPrompt.text = trajectoryAnalyser.checkSufficient()
            fragment.textViewRDEPrompt.setTextColor(Color.BLACK)
        }
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
     * Set the text for the prompt TextView according to the driving mode and speed change.
     */
    private fun setPromptText(drivingStyleText: String) {
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
    fun setDrivingStyleText() {
        drivingStyleText = when (desiredDrivingMode) {
            DrivingMode.URBAN -> "for more urban driving"
            DrivingMode.RURAL -> "for more rural driving"
            DrivingMode.MOTORWAY -> "for more motorway driving"
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