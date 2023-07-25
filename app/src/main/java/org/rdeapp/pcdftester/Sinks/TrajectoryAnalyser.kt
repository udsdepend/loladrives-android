package org.rdeapp.pcdftester.Sinks


class TrajectoryAnalyser(
    private val expectedDistance: Double
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

    private var desiredDrivingMode: DrivingMode = DrivingMode.URBAN
    private var totalTime: Double = 0.0
    private var currentSpeed: Double = 0.0


    // TODO: Check each driving mode that sufficient distance is covered of at least 16km
    // TODO: Check that a speed of 100km/h is driven for at least 5 minutes for the motorway driving mode
    // TODO: Check that at most 3% of the motorway driving mode is driven at 145km/h to 160km/h
    // TODO: Check that a stopping percentage of 6% to 30% is covered for the urban driving mode
    // TODO: Check that the average urban speed is between 15km/h and 40km/h

    /**
     * Check the progress of Urban, Rural and Motorway driving and update corresponding booleans.
     */
    fun updateProgress(
        urbanDistance: Double,
        ruralDistance: Double,
        motorwayDistance: Double,
        totalTime: Double,
        currentSpeed: Double
    ){
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

        this.totalTime = totalTime
        this.currentSpeed = currentSpeed
    }

    /**
    * Check whether a driving style is sufficient.
    */
    fun checkSufficient(): String {
        return if (motorwaySufficient) {
            "Motorway driving is sufficient"
        } else if (ruralSufficient) {
            "Rural driving is sufficient"
        } else if (urbanSufficient) {
            "Urban driving is sufficient"
        } else {
            "Your driving style is good"
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
     * Set the desired driving mode according to the proportions of urban, rural and motorway driving,
     * the current driving mode and the previously desired driving mode.
     */
    fun setDesiredDrivingMode() {
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
     * Check if any of the driving modes are complete.
     */
    fun checkInvalid() : Boolean {
        return motorwayComplete || ruralComplete || urbanComplete || totalTime > 120
    }

    /**
     * Get desired driving mode.
     */
    fun getDesiredDrivingMode() : DrivingMode {
        return desiredDrivingMode
    }
}