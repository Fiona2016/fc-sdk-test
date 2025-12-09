package com.example.fc_sdk_test

import android.app.Application
import android.util.Log

import cloud.flashcat.android.Flashcat
import cloud.flashcat.android.FlashcatSite
import cloud.flashcat.android.core.configuration.Configuration
import cloud.flashcat.android.privacy.TrackingConsent


import cloud.flashcat.android.rum.GlobalRumMonitor
import cloud.flashcat.android.rum.Rum
import cloud.flashcat.android.rum.RumConfiguration


class FcSdkTestApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Flashcat.setVerbosity(Log.VERBOSE)
        
        val configuration = Configuration.Builder(
            clientToken = "156b22631d5359deae51668d3c54da69131",  // Replace with your actual client token
            env = "dev",
            variant = "test"
        )
//            .useSite(FlashcatSite.STAGING)  // Endpoint already points to FlashCat backend
            .build()

        Flashcat.initialize(
            this,
            configuration,
            TrackingConsent.GRANTED
        )



        Rum.enable(
            RumConfiguration
                .Builder("Fzr9QakpXPJHWwSRxt5CLS")
                .trackUserInteractions()
                .trackLongTasks()
                .build()
        )
        GlobalRumMonitor.get().debug = true

    }
}

