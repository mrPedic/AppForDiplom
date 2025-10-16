package com.example.roamly

import dagger.hilt.android.HiltAndroidApp
import android.app.Application
import android.content.Context
import android.preference.PreferenceManager
import org.osmdroid.config.Configuration

@HiltAndroidApp
class RoamlyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val ctx: Context = applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        // Set a custom user agent to avoid tile server bans (use your app's package name)
        Configuration.getInstance().userAgentValue = packageName  // e.g., "com.example.roamly"
    }
}
