package de.unisaarland.loladrives

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import com.google.gson.annotations.SerializedName
import pcdfEvent.events.obdEvents.OBDCommand

class Constants {
    companion object {
        const val RECONNECTION_TRIES: Int = 1 // 5

        const val RDE_RTLOLA_ENGINE = "rde_bridge"

        private const val VERSION_NUMBER = "1.0.4"

        const val APP_ID = "LolaDrives v$VERSION_NUMBER"

        enum class RDE_RTLOLA_INPUT_QUANTITIES {
            VELOCITY,
            ALTITUDE,
            TEMPERATURE,
            NOX_PPM,
            MASS_AIR_FLOW,
            FUEL_RATE,
            FUEL_AIR_EQUIVALENCE
        }

        val NOT_TRACKABLE_EVENTS: List<OBDCommand> = listOf(
            OBDCommand.SUPPORTED_PIDS_1_00,
            OBDCommand.SUPPORTED_PIDS_1_32,
            OBDCommand.SUPPORTED_PIDS_1_64,
            OBDCommand.SUPPORTED_PIDS_1_96,
            OBDCommand.SUPPORTED_PIDS_1_128,
            OBDCommand.SUPPORTED_PIDS_1_160,
            OBDCommand.SUPPORTED_PIDS_1_192,
            OBDCommand.SUPPORTED_PIDS_9_00,
            OBDCommand.VIN
        )

        val SINGLE_TIME_EVENTS: List<OBDCommand> = listOf(
            OBDCommand.FUEL_TYPE,
            OBDCommand.MAX_VALUES
        )
    }
}

enum class OBDCommunication {
    INSUFFICIENT_SENSORS,
    OBD_COMMUNICATION_ERROR,
    UNSUPPORTED_PROTOCOL,
    OKAY
}

data class FileTokenPair(
    @SerializedName("id") val name: String,
    @SerializedName("auth_token")val authToken: String
)

fun Fragment.hideKeyboard() {
    view?.let { activity?.hideKeyboard(it) }
}

fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}
