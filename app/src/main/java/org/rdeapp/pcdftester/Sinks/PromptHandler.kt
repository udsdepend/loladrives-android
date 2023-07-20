package org.rdeapp.pcdftester.Sinks

import android.graphics.Color
import android.os.Build
import android.speech.tts.TextToSpeech
import android.widget.Toast
import de.unisaarland.loladrives.Fragments.RDE.RDEFragment
import kotlinx.android.synthetic.main.fragment_r_d_e.textViewRDEPrompt
import java.util.Locale

class PromptHandler (
    private val fragment: RDEFragment
    ) : TextToSpeech.OnInitListener{

    private var tts: TextToSpeech? = TextToSpeech(fragment.requireActivity(), this)
    private var expectedDistance = fragment.distance

    private var motorwayComplete: Boolean = false
    private var ruralComplete: Boolean = false
    private var urbanComplete: Boolean = false
    private var motorwaySufficient: Boolean = false
    private var ruralSufficient: Boolean = false
    private var urbanSufficient: Boolean = false
    private var motorwayInsufficient: Boolean = false
    private var ruralInsufficient: Boolean = false
    private var urbanInsufficient: Boolean = false


    /**
     * Update the prompt for improving the driving style according to the received RTLola results.
     */
    fun handlePrompt(
        totalDistance: Double,
        totalTime: Double,
    ) {
        // Cases where the RDE test is invalid
        if (urbanComplete || ruralComplete || motorwayComplete || totalTime > 120) {
            fragment.textViewRDEPrompt.text = "This RDE test will be invalid, you may want to restart it."
            fragment.textViewRDEPrompt.setTextColor(Color.RED)
            // TODO: suggest to stop the test and start a new one
        }

        val currentSpeed = fragment.rdeValidator.currentSpeed
        var speedChange: Double = 0.0
        var drivingStyleText: String = ""

        // Cases where the RDE test is still valid, but the driver should improve
        if (totalDistance > expectedDistance/2) {
            if (urbanComplete){
                if (ruralInsufficient) {
                    if (motorwayComplete) {
                        // Rural has not passed yet
                        drivingStyleText = "for more rural driving"
                        speedChange = computeSpeedChange(currentSpeed, 60, 90)
                    } else {
                        // Rural and Motorway have not passed yet
                        drivingStyleText = "for more rural and motorway driving"
                        speedChange = computeSpeedChange(currentSpeed, 60, 160)
                    }
                } else if (motorwayInsufficient) {
                    // Motorway has not passed yet
                    drivingStyleText = "for more motorway driving"
                    speedChange = computeSpeedChange(currentSpeed, 90, 160)
                }
            } else {
                if (urbanInsufficient) {
                    if (ruralComplete) {
                        if (motorwayComplete) {
                            speedChange = computeSpeedChange(currentSpeed, 0, 60)
                        }
                    }
                }
                if (motorwayInsufficient) {
                    if (ruralComplete) {
                        // Motorway has not passed yet
                        drivingStyleText = "for more motorway driving"
                        speedChange = computeSpeedChange(currentSpeed, 90, 160)
                    } else {
                        // Urban, Rural and Motorway have not passed yet
                        drivingStyleText = "for more rural and motorway driving"
                        speedChange = computeSpeedChange(currentSpeed, 0, 160)
                    }
                } else if (ruralInsufficient) {
                    // Rural has not passed yet, urban is not complete
                    drivingStyleText = "for more urban and rural driving"
                    speedChange = computeSpeedChange(currentSpeed, 0, 90)
                }
            }

            if (speedChange > 0) {
                fragment.textViewRDEPrompt.text = "Aim for a higher driving speed $drivingStyleText"
                fragment.textViewRDEPrompt.setTextColor(Color.GREEN)
            } else if (speedChange < 0) {
                fragment.textViewRDEPrompt.text = "Aim for a lower driving speed $drivingStyleText"
                fragment.textViewRDEPrompt.setTextColor(Color.RED)
            } else {
                fragment.textViewRDEPrompt.text = "Your driving style is good"
                fragment.textViewRDEPrompt.setTextColor(Color.BLACK)
            }
            speak()
        } else {
            // TODO announce if a driving style has been complete
        }
    }

    /**
     * Check the progress of Urban, Rural and Motorway driving and update corresponding booleans.
     */
    fun checkProgress(
        urbanDistance: Double,
        ruralDistance: Double,
        motorwayDistance: Double
    ){
        val urbanProportion = urbanDistance / expectedDistance
        val ruralProportion = ruralDistance / expectedDistance
        val motorwayProportion = motorwayDistance / expectedDistance

        motorwayComplete = motorwayProportion > 0.43
        ruralComplete = ruralProportion > 0.43
        urbanComplete = urbanProportion > 0.44

        motorwaySufficient = motorwayProportion > 0.23
        ruralSufficient = ruralProportion > 0.23
        urbanSufficient = urbanProportion > 0.29

        motorwayInsufficient = motorwayProportion < 0.18
        ruralInsufficient = ruralProportion < 0.18
        urbanInsufficient = urbanProportion < 0.23
    }


    /**
     * Calculate the acceleration and deceleration of the car.
     */
    private fun computeSpeedChange (
        currentSpeed: Double,
        lowerThreshold: Int,
        upperThreshold: Int,
    ): Double {
        val speedChange: Double
        if (currentSpeed < lowerThreshold) {
            speedChange = lowerThreshold - currentSpeed
        } else if (currentSpeed > upperThreshold) {
            speedChange = currentSpeed - upperThreshold
        } else {
            speedChange = 0.0
        }
        return speedChange
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts!!.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(fragment.requireActivity(),"The Language not supported!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun speak() {
        val text = fragment.textViewRDEPrompt.text.toString()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts!!.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ID")
        } else {
            Toast.makeText(fragment.requireActivity(), "This SDK version does not support Text To Speech.", Toast.LENGTH_LONG).show()
        }
    }

    // TODO check if onDestroy() is needed through fragment.requireActivity()

}