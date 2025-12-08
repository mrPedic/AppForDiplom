package com.example.roamly

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
class PureLocationManager(private val context: Context) : LocationListener {
    private var locationManager: LocationManager? = null
    private var currentLocation: Location? = null
    private var callback: ((Location) -> Unit)? = null
    init {
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }
    @Suppress("MissingPermission")
    fun getCurrentLocation(onLocation: (Location?) -> Unit) {
        if (!hasLocationPermission()) {
            onLocation(null)
            return
        }
        val gpsLocation = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        val networkLocation = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        val bestLocation = when {
            gpsLocation == null -> networkLocation
            networkLocation == null -> gpsLocation
            else -> if (gpsLocation.accuracy <= networkLocation.accuracy &&
                gpsLocation.time > networkLocation.time - 2 * 60 * 1000) {
                gpsLocation
            } else {
                networkLocation
            }
        }
        if (bestLocation != null) {
            onLocation(bestLocation)
        } else {
            requestSingleUpdate(onLocation)
        }
    }
    @Suppress("MissingPermission")
    private fun requestSingleUpdate(onLocation: (Location?) -> Unit) {
        if (!hasLocationPermission()) return
        var received = false
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (!received) {
                    received = true
                    stopListening(this)
                    onLocation(location)
                }
            }
        }
        try {
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                0L,
                0f,
                listener
            )
        } catch (e: Exception) { }
        try {
            locationManager?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                0L,
                0f,
                listener
            )
        } catch (e: Exception) { }
        Handler(Looper.getMainLooper()).postDelayed({
            if (!received) {
                stopListening(listener)
                onLocation(currentLocation ?: getLastKnownLocation())
            }
        }, 20000)
    }
    @Suppress("MissingPermission")
    fun startLocationUpdates(callback: (Location) -> Unit) {
        if (!hasLocationPermission()) return
        this.callback = callback
        try {
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                2000L,  // 2 секунды
                5f,     // 5 метров
                this
            )
        } catch (e: Exception) { }
        try {
            locationManager?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                5000L,
                10f,
                this
            )
        } catch (e: Exception) { }
    }
    fun stopLocationUpdates() {
        locationManager?.removeUpdates(this)
        callback = null
    }
    override fun onLocationChanged(location: Location) {
        currentLocation = location
        callback?.invoke(location)
    }
    override fun onLocationChanged(locations: List<Location>) {
        locations.lastOrNull()?.let { onLocationChanged(it) }
    }
    @Suppress("MissingPermission")
    private fun getLastKnownLocation(): Location? {
        if (!hasLocationPermission()) return null
        val providers = locationManager?.getProviders(true) ?: return null
        var bestLocation: Location? = null
        for (provider in providers) {
            val loc = locationManager?.getLastKnownLocation(provider) ?: continue
            if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                bestLocation = loc
            }
        }
        return bestLocation
    }
    fun isProviderEnabled(provider: String): Boolean {
        return locationManager?.isProviderEnabled(provider) ?: false
    }
    private fun stopListening(listener: LocationListener) {
        locationManager?.removeUpdates(listener)
    }
}