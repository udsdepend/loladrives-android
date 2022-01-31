package de.unisaarland.loladrives.Sources

import android.annotation.SuppressLint
import android.app.Activity
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import pcdfEvent.PCDFEvent
import pcdfEvent.events.GPSEvent

class GPSSource(val outputChannel: SendChannel<PCDFEvent>, val activity: Activity) {
    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    fun start() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)
        requestNewLocationData()
    }

    fun stop() {
        stopLocationUpdates()
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        mFusedLocationClient.lastLocation.addOnSuccessListener { }
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 100
        mLocationRequest.fastestInterval = 100
        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest,
            mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult.lastLocation
            val event = GPSEvent(
                "Phone-GPS",
                System.nanoTime(),
                mLastLocation.longitude,
                mLastLocation.latitude,
                mLastLocation.altitude,
                mLastLocation.speed.toDouble()
            )
            GlobalScope.launch(Dispatchers.Main) {
                outputChannel.send(event)
            }
        }
    }

    private fun stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
    }
}
