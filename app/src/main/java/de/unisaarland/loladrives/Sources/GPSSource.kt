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
    val mUUID = "6183f7d2-9dc1-11ea-bb37-0242ac130002"
    var running = false
    fun start() {
        running = true
        println("starte gps")
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)
        requestNewLocationData()
    }

    fun stop() {
        println("Stoppe GPS")
        running = false
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
                try {
                    println("Receive gps")
                    outputChannel.send(event)
                } catch(e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun stopLocationUpdates() {
        try {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback)
        } catch (e: Exception) {
        }
    }
}
