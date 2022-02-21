package com.lirctek.driverbehaviour

import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.google.android.gms.location.LocationRequest
import com.yayandroid.locationmanager.base.LocationBaseService
import com.yayandroid.locationmanager.configuration.DefaultProviderConfiguration
import com.yayandroid.locationmanager.configuration.GooglePlayServicesConfiguration
import com.yayandroid.locationmanager.configuration.LocationConfiguration
import org.greenrobot.eventbus.EventBus

class LocationTracker: LocationBaseService() {

    private val binder = LocationTrackerBinder()
    private var isLocationRequested = false

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (!isLocationRequested) {
            isLocationRequested = true
            getLocation()
        }

        // Return type is depends on your requirements
        return START_NOT_STICKY
    }

    override fun onBind(p0: Intent?): IBinder {
        return binder
    }

    override fun onLocationChanged(location: Location?) {
        EventBus.getDefault().post(location)
    }

    override fun onLocationFailed(type: Int) {
    }

    override fun getLocationConfiguration(): LocationConfiguration {
        val locationrequest = LocationRequest.create()
        locationrequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationrequest.interval = 1000
        return LocationConfiguration.Builder()
            .keepTracking(true)
            .useGooglePlayServices(GooglePlayServicesConfiguration.Builder().askForSettingsApi(true).locationRequest(locationrequest).build())
            .useDefaultProviders(DefaultProviderConfiguration.Builder().requiredTimeInterval(1000).build())
            .build()
    }

    inner class LocationTrackerBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): LocationTracker = this@LocationTracker
    }

}