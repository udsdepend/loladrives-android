package org.rdeapp.pcdftester.Sinks

/**
 * Enum class for the different driving modes.
 */
enum class DrivingMode {
    URBAN,
    RURAL,
    MOTORWAY
}

/**
 * Enum class for the different prompt types.
 * @property SUFFICIENCY Prompt for insufficient driving style.
 * @property DRIVINGSTYLE Prompt for improving the driving style.
 * @property AVERAGEURBANSPEED Prompt for maintaining average speed between 15km/h to 40km/h.
 * @property STOPPINGPERCENTAGE Prompt for stopping percentage to increase or decrease.
 * @property HIGHSPEEDPERCENTAGE Prompt for high speed percentage(Driving at 100km/h or more for at least 5 mins).
 * @property VERYHIGHSPEEDPERCENTAGE Prompt for very high speed percentage(Driving at 145km/h for a max of 3%).
 */
enum class PromptTypes {
    SUFFICIENCY,
    DRIVINGSTYLE,
    AVERAGEURBANSPEED,
    STOPPINGPERCENTAGE,
    HIGHSPEEDPERCENTAGE,
    VERYHIGHSPEEDPERCENTAGE

}