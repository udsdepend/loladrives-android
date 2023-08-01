package org.rdeapp.pcdftester.Sinks

class TrajectoryAnalyser(
    private val expectedDistance: Double,
    private val velocityProfile: VelocityProfile
) {
    private var motorwayComplete: Boolean = false
    private var ruralComplete: Boolean = false
    private var urbanComplete: Boolean = false
    private var motorwaySufficient: Boolean = false
    private var ruralSufficient: Boolean = false
    private var urbanSufficient: Boolean = false

    private var urbanProportion: Double = 0.0
    private var ruralProportion: Double = 0.0
    private var motorwayProportion: Double = 0.0
    private var sufficientModes = mutableListOf<DrivingMode>()

    private var desiredDrivingMode: DrivingMode = DrivingMode.URBAN
    private var totalTime: Double = 0.0
    private var currentSpeed: Double = 0.0
    private var averageUrbanSpeed: Double = 0.0

    private var isInvalid: Boolean = false

    /**
     * Check the progress of Urban, Rural and Motorway driving and update corresponding booleans.
     */
    fun updateProgress(
        urbanDistance: Double,
        ruralDistance: Double,
        motorwayDistance: Double,
        totalTime: Double,
        currentSpeed: Double,
        averageUrbanSpeed: Double
    ){
        this.totalTime = totalTime
        this.currentSpeed = currentSpeed
        this.averageUrbanSpeed = averageUrbanSpeed

        velocityProfile.updateVelocityProfile(currentSpeed)

        // check the progress of the driving modes
        urbanProportion = urbanDistance / 1000 / expectedDistance
        ruralProportion = ruralDistance / 1000 / expectedDistance
        motorwayProportion = motorwayDistance / 1000 / expectedDistance

        motorwayComplete = motorwayProportion > 0.43
        ruralComplete = ruralProportion > 0.43
        urbanComplete = urbanProportion > 0.44

        motorwaySufficient = motorwayProportion >= 0.18
        ruralSufficient = ruralProportion >= 0.18
        urbanSufficient = urbanProportion >= 0.23
    }

    /**
     * Check if any of the driving modes are complete.
     */
    fun checkInvalid() : Boolean {
        return isInvalid || totalTime > 120
    }

    /**
     * Set the desired driving mode according to the proportions of urban, rural and motorway driving,
     * the current driving mode and the previously desired driving mode.
     */
    fun setDesiredDrivingMode(): DrivingMode {
        when {
            urbanSufficient && !ruralSufficient && !motorwaySufficient -> {
                desiredDrivingMode = chooseNextDrivingMode(DrivingMode.RURAL, DrivingMode.MOTORWAY)
            }
            !urbanSufficient && ruralSufficient && !motorwaySufficient -> {
                desiredDrivingMode = chooseNextDrivingMode(DrivingMode.URBAN, DrivingMode.MOTORWAY)
            }
            !urbanSufficient && !ruralSufficient && motorwaySufficient -> {
                desiredDrivingMode = chooseNextDrivingMode(DrivingMode.URBAN, DrivingMode.RURAL)
            }
            !motorwaySufficient -> {
                desiredDrivingMode = DrivingMode.MOTORWAY
            }
            !ruralSufficient -> {
                desiredDrivingMode = DrivingMode.RURAL
            }
            !urbanSufficient -> {
                desiredDrivingMode = DrivingMode.URBAN
            }
        }

        return desiredDrivingMode
    }

    /**
     * Choose which should be the next driving mode
     * @return the chosen driving mode
     */
    private fun chooseNextDrivingMode(firstDrivingMode: DrivingMode, secondDrivingMode: DrivingMode): DrivingMode {
        return if (currentDrivingMode() == firstDrivingMode || currentDrivingMode() == firstDrivingMode) {
            firstDrivingMode
        } else {
            secondDrivingMode
        }
    }

    /**
     * Check that a speed of 145km/h is driven for less than 3% of the maximum test time
     * of the motorway driving mode.
     * If this is exceeded, set isInvalid to true.
     * @return the duration of the driven speed if it is valid and requires warning, null otherwise
     */
    private fun isVeryHighSpeedValid(): Double? {
        val veryHighSpeedDuration = velocityProfile.getVeryHighSpeed()
        if (veryHighSpeedDuration > 0.03 * 120 * 0.43) {
            // driven in > 145 km/h for more than 3% of the max test time
            isInvalid = true
            return null
        } else if (veryHighSpeedDuration == (0.025 * 90 * 0.29).toLong()) {
            return 0.025 // driven in > 145 km/h for more 1.5% of the min test time
        }
        else if (veryHighSpeedDuration == (0.015 * 90 * 0.29).toLong()) {
            return 0.015 // driven in > 145 km/h for more 1.5% of the min test time
        }
        return null
    }

    /**
     * Check that the motorway driving style is valid and does not violate the constraints.
     * If not and they cannot be validated, set isInvalid to true to terminate the test.
     * @return the remaining time to be driven if it is valid and requires instruction, null otherwise
     */
    private fun isHighSpeedValid(): Long? {
        return when (val highSpeed = canHighSpeedPass()) {
            null -> {
                isInvalid = true
                null
            }
            else -> highSpeed // the remaining time to be driven at 100km/h, or 0 if it is exceeded.
        }
    }

    /**
     * Consider time and distance left to compute whether high speed can pass
     */
    private fun canHighSpeedPass(): Long? {
        val highSpeedDuration = velocityProfile.getHighSpeed()
        return if (highSpeedDuration > 5) {
            0
        } else {
            if (totalTime + (5 - highSpeedDuration) <= 120) {
                5 - highSpeedDuration // There is enough time to drive at 100km/h for 5 minutes
            } else {
                null
            }
        }
    }

    /**
     * Check if the stopping percentage can be increased or decreased to pass this condition.
     * @return the stopping percentage that can be increased or decreased to pass this condition
     *         or null if not required.
     */
    private fun isStoppingTimeValid() : Double? {
        val currentStoppingTime = velocityProfile.getStoppingTime()
        val remainingTime = 120 - totalTime

        when {
            currentStoppingTime.toDouble() ==  0.03 * 90 -> {
                // Stopping percentage is close to being valid but can be increased to pass
                return  currentStoppingTime/90 - 0.06
            }
            currentStoppingTime.toDouble() == 0.25 * 120 -> {
                // Stopping percentage is close to being invalid but can be decreased to pass
                return currentStoppingTime/120 - 0.3
            }
            currentStoppingTime > 0.3 * 90 && currentStoppingTime < 0.3 * 120 -> {
                // Stopping percentage is invalid but can be decreased to pass
                return currentStoppingTime/120 - 0.3
            }
            currentStoppingTime > 0.3 * 120 && remainingTime < (0.3 * 120 - currentStoppingTime)-> {
                // Stopping percentage is invalid and can't be decreased to pass
                return null
            }
            currentStoppingTime < 0.06 * 120 && remainingTime < (0.06 * 120 - currentStoppingTime) -> {
                // Stopping percentage is invalid and can't be increased to pass
                return null
            }
            0.06 * totalTime <= currentStoppingTime && currentStoppingTime <= 0.3 * totalTime ->{
                return null
            }
            else -> {
                return null
            }
        }
    }

    /**
     * Check if the average urban speed is between 15km/h and 40km/h, or can be increased or
     * decreased to pass this condition.
     */
    private fun isAverageSpeedValid(): Double? {
        return if (averageUrbanSpeed > 15 && averageUrbanSpeed < 40) {
            0.0
        } else {
            if (canAverageUrbanSpeedPassWithExpectedDistance(averageUrbanSpeed)) {
                50.0 // average speed is not valid but can be increased or decreased to pass
            } else {
                isInvalid = true
                null
            }
        }
    }

    /**
     * Can the average urban speed be still achieved with the expected distance.
     */
    private fun canAverageUrbanSpeedPassWithExpectedDistance(averageUrbanSpeed: Double): Boolean {
        val urbanDistanceLeft = (0.44 - urbanProportion) * expectedDistance
        val remainingTime = 120 - totalTime

        return if (urbanDistanceLeft < 0 || urbanComplete) {
            // Need to drive a longer distance
            canAverageUrbanSpeedPass()
        } else {
            // compute the average urban speed with the remaining distance to be done
            val averageUrbanSpeedWithDistanceLeft = urbanDistanceLeft / remainingTime
            averageUrbanSpeedWithDistanceLeft > 15 && averageUrbanSpeedWithDistanceLeft < 40
        }
    }

    /**
     * Check that the average urban speed of 15km/h and 40km/h can be achieved
     * when the expected distance is extended but within the time limit of 120 minutes.
     */
    private fun canAverageUrbanSpeedPass() : Boolean{
        // val remainingTime = 120 - totalTime
        // All the driving modes sufficient then the average urban speed can be achieved without worrying about exceeding the proportion
        //TODO: Implement logic to check all possibilities
        return true
    }

    /**
     * Check whether the distance in a driving mode is sufficient.
     * @return a driving style has become sufficient, or null if none has become sufficient.
     */
    fun checkSufficient(): DrivingMode? {
        if (motorwaySufficient && !sufficientModes.contains(DrivingMode.MOTORWAY)) {
            sufficientModes.add(DrivingMode.MOTORWAY)
            return DrivingMode.MOTORWAY
        } else if (ruralSufficient && !sufficientModes.contains(DrivingMode.RURAL)) {
            sufficientModes.add(DrivingMode.RURAL)
            return DrivingMode.RURAL
        } else if (urbanSufficient && !sufficientModes.contains(DrivingMode.URBAN)) {
            sufficientModes.add(DrivingMode.URBAN)
            return DrivingMode.URBAN
        }
        return null
    }

    /**
     * Determine the current driving mode according to the current speed.
     */
    private fun currentDrivingMode(): DrivingMode {
        return when {
            currentSpeed < 60 -> DrivingMode.URBAN
            currentSpeed < 90 -> DrivingMode.RURAL
            else -> DrivingMode.MOTORWAY
        }
    }

    /**
     * Calculate the speed change required to improve the driving style.
     */
    fun computeSpeedChange () : Double {
        val lowerThreshold: Double;
        val upperThreshold: Double;

        when (desiredDrivingMode) {
            DrivingMode.URBAN -> { lowerThreshold = 0.0; upperThreshold = 60.0 }
            DrivingMode.RURAL -> { lowerThreshold = 60.0; upperThreshold = 90.0 }
            DrivingMode.MOTORWAY -> { lowerThreshold = 90.0; upperThreshold = 145.0 }
        }

        return if (currentSpeed < lowerThreshold) {
            lowerThreshold - currentSpeed
        } else if (currentSpeed > upperThreshold) {
            currentSpeed - upperThreshold
        } else {
            0.0
        }

    }

    /**
     * Calculate how long the user to should drive in the certain driving mode to improve their
     * driving style.
     */
    fun computeDuration() : Double {
        when(desiredDrivingMode) {
            DrivingMode.URBAN -> {
                // Calculate the distance left to drive in urban mode with an average speed of 30 km/h
                val urbanDistanceLeft = (0.29 - urbanProportion) * expectedDistance
                return urbanDistanceLeft * 2
            }
            DrivingMode.RURAL -> {
                // Calculate the distance left to drive in rural mode with an average speed of 75 km/h
                val ruralDistanceLeft = (0.23 - ruralProportion) * expectedDistance
                return ruralDistanceLeft * 0.8
            }
            DrivingMode.MOTORWAY -> {
                // Calculate the distance left to drive in motorway mode with an average speed of 115 km/h
                val motorwayDistanceLeft = (0.23 - motorwayProportion) * expectedDistance
                return motorwayDistanceLeft * 60 / 115
            }
        }
    }
}