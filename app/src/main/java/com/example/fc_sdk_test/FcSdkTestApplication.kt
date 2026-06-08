package com.example.fc_sdk_test

import android.app.Application
import android.util.Log

import com.datadog.android.Datadog
import com.datadog.android.FlashcatSite
import com.datadog.android.core.configuration.Configuration
import com.datadog.android.privacy.TrackingConsent

import com.datadog.android.ndk.NdkCrashReports

import com.datadog.android.rum.GlobalRumMonitor
import com.datadog.android.rum.Rum
import com.datadog.android.rum.RumConfiguration

import com.datadog.android.trace.Trace
import com.datadog.android.trace.TraceConfiguration


class FcSdkTestApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Datadog.setVerbosity(Log.VERBOSE)
        
        val configuration = Configuration.Builder(
            clientToken = "55dfd4ca9cf75beda92ad63b4c1d6c68131",
            env = "dev",
            variant = "test"
        )
            // Route to the dev fc-rum (jira.flashcat.cloud)
            .useSite(FlashcatSite.STAGING)
            .build()

        Datadog.initialize(
            this,
            configuration,
            TrackingConsent.GRANTED
        )

        // Install the native (NDK) signal handler so native crashes are captured
        // and reported as RUM errors on the next app launch.
        NdkCrashReports.enable()

        // Enable Trace feature (required for network tracing)
        Trace.enable(
            TraceConfiguration.Builder()
                .build()
        )

        // Enable RUM feature
        Rum.enable(
            RumConfiguration
                .Builder("AtQmjhr99iABTZywhsgV9b")
                .trackUserInteractions()
                .trackLongTasks()
                .trackNonFatalAnrs(true)
                .build()
        )
        GlobalRumMonitor.get().debug = true
        
        Log.d("FcSdkTestApp", "Flashcat SDK initialized with Trace and RUM features")
    }
}

