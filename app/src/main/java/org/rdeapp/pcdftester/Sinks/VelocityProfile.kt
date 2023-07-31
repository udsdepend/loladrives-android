package org.rdeapp.pcdftester.Sinks

import java.util.Calendar

class VelocityProfile {
    private var currentSpeed: Double = 0.0
    private var previousSpeed: Double = 0.0
    private var velStop: Long = 0L
    private var vel100plus: Long = 0L
    private var vel145plus: Long = 0L
    private var lastUpdated: Long = 0L

    /**
     * Update the velocity profile with a new speed.
     */
    fun updateVelocityProfile(speed: Double) {
        currentSpeed = speed
        val currentTime = Calendar.getInstance().timeInMillis

        // check if the current speed is in a certain range
        setStop(currentTime)
        setHighSpeed(currentTime)
        setVeryHighSpeed(currentTime)

        // update the timestamp of the last update
        setLastUpdated(currentTime)
    }

    /**
     * Set the time spent stopping (at 0 km/h).
     */
    private fun setStop(currentTime: Long) {
        velStop = if (currentSpeed == 0.0 && previousSpeed == 0.0) {
            velStop + (currentTime - lastUpdated)
        } else {
            velStop
        }
    }

    /**
     * Set the time spent at 145 km/h or more.
     */
    private fun setVeryHighSpeed(currentTime: Long) {
        vel145plus = if (currentSpeed > 145 && previousSpeed > 145) {
            vel145plus + (currentTime - lastUpdated)
        } else {
            vel145plus
        }
    }

    /**
     * Set the time spent at 100 km/h or more.
     */
    private fun setHighSpeed(currentTime: Long) {
        vel100plus = if (currentSpeed > 100 && previousSpeed > 100) {
            vel100plus + (currentTime - lastUpdated)
        } else {
            vel100plus
        }
    }

    /**
     * Set the timestamp of the last update.
     */
    private fun setLastUpdated(currentTime: Long) {
        lastUpdated = currentTime
    }

    /**
     * Get the time spent stopping (at 0 km/h).
     */
    fun getStoppingTime(): Long {
        return velStop
    }

    /**
     * Get the time spent at 100 km/h or more.
     */
    fun getHighSpeed(): Long {
        return vel100plus
    }

    /**
     * Get the time spent at 145 km/h or more.
     */
    fun getVeryHighSpeed(): Long {
        return vel145plus
    }

}