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
    private var motorwayInsufficient: Boolean = false
    private var ruralInsufficient: Boolean = false
    private var urbanInsufficient: Boolean = false

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

        motorwaySufficient = motorwayProportion > 0.23
        ruralSufficient = ruralProportion > 0.23
        urbanSufficient = urbanProportion > 0.29

        motorwayInsufficient = motorwayProportion < 0.18
        ruralInsufficient = ruralProportion < 0.18
        urbanInsufficient = urbanProportion < 0.23
    }

    /**
     * Check if any of the driving modes are complete.
     */
    fun checkInvalid() : Boolean {
        // check if any of the constraints of specific driving modes are violated
        isMotorwayValid()
        isUrbanValid(averageUrbanSpeed)

        return isInvalid || totalTime > 120
    }

    /**
     * Check that the motorway driving style is valid and follows the constraints.
     * If not and they cannot be validates, set isInvalid to true.
     *
     * @return true if the motorway driving style is currently valid, false otherwise
     */
    fun isMotorwayValid(): Boolean {
        // Check that a speed of 100km/h is driven for at least 5 minutes for the motorway driving mode
        val isValid = canHighSpeedPass()

        // if it is not possible to drive at 100km/h for 5 minutes, set isInvalid to true to terminate the test
        if (!isValid) { isInvalid = true }

        // Check that a speed of 145km/h is driven for less than 3% of max for the motorway driving mode
        val veryHighSpeedDuration = velocityProfile.getVeryHighSpeed()
        if (veryHighSpeedDuration > 0.03 * 120 * 0.43) {
            // driven in > 145 km/h for more than 3% of the max test time
            isInvalid = true
        }
        return isValid
    }

    /**
     * Consider time and distance left to compute whether high speed can pass
     */
    private fun canHighSpeedPass(): Boolean {
        val highSpeedDuration = velocityProfile.getHighSpeed()
        return if (highSpeedDuration > 5) {
            true
        } else if (totalTime + (5 - highSpeedDuration) <= 120) {
            // There is enough time to drive at 100km/h for 5 minutes
            if (motorwaySufficient) { // driver still needs to drive more in motorway
                // TODO instruct driver to drive more in motorway and 100 km/h
            }
            true
        } else {
            false
        }
    }

    /**
     * Check that the urban driving style is valid.
     * TODO: Check that a stopping percentage of 6% to 30% is covered for the urban driving mode
     * TODO: Check that the average urban speed is between 15km/h and 40km/h
     */
    fun isUrbanValid(averageUrbanSpeed: Double): Boolean {
        var validStoppingPercentage: Boolean =
            if (velocityProfile.getStoppingTime() > 0.3 * 120) {
                false // more than 30% of the time is spent stopping
            } else if (velocityProfile.getStoppingTime() < 0.06 * 120) {
                    canStoppingPercentagePass(velocityProfile.getStoppingTime())
                } else {
            true
        }

        var validAverageSpeed: Boolean = if (averageUrbanSpeed > 15 && averageUrbanSpeed < 40) {
            true
        } else {
            canAverageUrbanSpeedPassWithExpectedDistance(averageUrbanSpeed)
        }
        return validAverageSpeed && validStoppingPercentage
    }

    /**
     * Check if the stopping percentage can be increased or decreased to pass this condition.
     * TODO: Change logic to consider all possibilities
     */
    private fun canStoppingPercentagePass(stoppingPercentage: Long) : Boolean {
        val remainingTime = 120 - totalTime

        return if (stoppingPercentage < 0.06 && !urbanComplete){
            val timeRequiredToStop =  (0.06 - stoppingPercentage) * 120
            timeRequiredToStop < remainingTime
        } else if (stoppingPercentage < 0.06){
            canStoppingSpeedPass()
        } else if (stoppingPercentage > 0.3 && !urbanInsufficient) {
            val timeRequiredToStop = (stoppingPercentage - 0.3) * 120
            timeRequiredToStop < remainingTime
        } else if (stoppingPercentage > 0.3) {
            canStoppingSpeedPass()
        } else {
            true
        }
    }

    /**
     * Check if the stopping speed can be increased or decreased to pass this condition at all.
     */
    private fun canStoppingSpeedPass() : Boolean{
        TODO("Not yet implemented")
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
     * TODO: empty the sufficientModes list if the distance is changed
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
     * Set the desired driving mode according to the proportions of urban, rural and motorway driving,
     * the current driving mode and the previously desired driving mode.
     */
    fun setDesiredDrivingMode(): DrivingMode {
        when {
            urbanSufficient && ruralSufficient && motorwayInsufficient -> {
                desiredDrivingMode = DrivingMode.MOTORWAY
            }
            urbanSufficient && ruralInsufficient && motorwaySufficient -> {
                desiredDrivingMode = DrivingMode.RURAL
            }
            urbanInsufficient && ruralSufficient && motorwaySufficient -> {
                desiredDrivingMode = DrivingMode.URBAN
            }
            urbanSufficient && ruralInsufficient && motorwayInsufficient -> {
                desiredDrivingMode = if (desiredDrivingMode == DrivingMode.MOTORWAY || currentDrivingMode() == DrivingMode.MOTORWAY) {
                    DrivingMode.MOTORWAY
                } else {
                    DrivingMode.RURAL
                }
            }
            urbanInsufficient && ruralSufficient && motorwayInsufficient -> {
                desiredDrivingMode = if (currentDrivingMode() == DrivingMode.URBAN || currentDrivingMode() == DrivingMode.URBAN) {
                    DrivingMode.URBAN
                } else {
                    DrivingMode.MOTORWAY
                }
            }
            urbanInsufficient && ruralInsufficient && motorwaySufficient -> {
                desiredDrivingMode = if (currentDrivingMode() == DrivingMode.URBAN || currentDrivingMode() == DrivingMode.URBAN) {
                    DrivingMode.URBAN
                } else {
                    DrivingMode.RURAL
                }
            }
        }
        return desiredDrivingMode
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
     * Calculate how long the user to should drive in the certain driving mode to improve their driving style.
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

    /**
     * @return the desired driving mode.
     */
    fun getDesiredDrivingMode() : DrivingMode {
        return desiredDrivingMode
    }
}