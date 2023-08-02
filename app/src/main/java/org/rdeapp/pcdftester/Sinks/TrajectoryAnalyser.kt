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
     * @return whether the test is invalid or has exceeded the time limit.
     */
    fun checkInvalid() : Boolean {
        return isInvalid || totalTime > 120
    }


    /**
     * @return the Array of the constrains on motorway and urban driving modes. All return values
     * are null if the constraint is satisfied or is invalid, and a Double otherwise.
     * [0] = isHighSpeedValid()
     * [1] = isVeryHighSpeedValid()
     * [2] = isStoppingTimeValid()
     * [3] = isAverageSpeedValid()
     */
    fun getConstraints(): Array<Double?> {
        return arrayOf(isHighSpeedValid(), isVeryHighSpeedValid(), isStoppingTimeValid(), isAverageSpeedValid())
    }

    /**
     * @return the average speed of the urban driving mode
     */
    fun getAverageUrbanSpeed(): Double {
        return averageUrbanSpeed
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
        return if (desiredDrivingMode == firstDrivingMode || currentDrivingMode() == firstDrivingMode) {
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
    private fun isHighSpeedValid(): Double? {
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
    private fun canHighSpeedPass(): Double? {
        val highSpeedDuration = velocityProfile.getHighSpeed().toDouble()
        return if (highSpeedDuration > 5) {
            0.0
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
            currentStoppingTime > 0.3 * 120 && remainingTime < (0.3 * 120 - currentStoppingTime)-> {
                // Stopping percentage is invalid and can't be decreased to pass
                isInvalid = true
                return null
            }
            currentStoppingTime < 0.06 * 120 && remainingTime < (0.06 * 120 - currentStoppingTime) -> {
                // Stopping percentage is invalid and can't be increased to pass
                isInvalid = true
                return null
            }
            currentStoppingTime.toDouble() >=  0.03 * 90 && currentStoppingTime.toDouble() <  0.06 * 90 -> {
                // Stopping percentage is close to being valid but can be increased to pass
                return  currentStoppingTime/90 - 0.06
            }
            currentStoppingTime.toDouble() >= 0.25 * 120 && currentStoppingTime.toDouble() < 0.3 * 120 -> {
                // Stopping percentage is close to being invalid but can be decreased to pass
                return currentStoppingTime/120 - 0.3
            }
            currentStoppingTime > 0.3 * 90 && currentStoppingTime < 0.3 * 120 -> {
                // Stopping percentage could be invalid but can be decreased to pass
                return currentStoppingTime/120 - 0.3
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
     * @return the change in average urban speed to pass this condition
     *         eg. -5 or 10, or null if not required.
     */
    private fun isAverageSpeedValid(): Double? {
        val urbanDistanceLeft = (0.44 - urbanProportion) * expectedDistance
        val remainingTime = 120 - totalTime
        val requiredSpeed = urbanDistanceLeft / remainingTime

        when {
            averageUrbanSpeed < 15 && (15 > requiredSpeed || 40 < requiredSpeed) && remainingTime < 20 -> {
                // Need to drive faster to make the average urban speed higher to pass but can't
                isInvalid = true
                return null
            }
            averageUrbanSpeed > 40 && urbanDistanceLeft == 0.0 && remainingTime < 20-> {
                // Need to slower drive make the average urban speed lower to pass but can't
                isInvalid = true
                return null
            }
            averageUrbanSpeed > 35 && averageUrbanSpeed < 40 -> {
                // average speed is high and close to being invalid
                return 40 - averageUrbanSpeed
            }
            averageUrbanSpeed > 15 && averageUrbanSpeed < 20 -> {
                // average speed is low and close to being invalid
                return 15 - averageUrbanSpeed
            }
            averageUrbanSpeed > 40 -> {
                // average speed is invalid but can be decreased to pass
                return 40 - averageUrbanSpeed
            }
            averageUrbanSpeed < 15 -> {
                // average speed is invalid but can be increased to pass
                return 15 - averageUrbanSpeed
            }
            averageUrbanSpeed > 15 && averageUrbanSpeed < 40 -> {
                // average speed is valid
                return null
            }
            else -> {
                return null
            }
        }
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
    fun currentDrivingMode(): DrivingMode {
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
        val lowerThreshold: Double
        val upperThreshold: Double

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