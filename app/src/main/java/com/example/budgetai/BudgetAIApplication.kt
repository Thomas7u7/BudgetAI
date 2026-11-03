package com.example.budgetai

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class BudgetAIApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)

        // En debug se usa el token temporal (regístralo en Firebase > App Check).
        // En release, Play Integrity valida la app instalada desde Play.
        val factory: AppCheckProviderFactory =
            if (BuildConfig.DEBUG) {
                DebugAppCheckProviderFactory.getInstance()
            } else {
                PlayIntegrityAppCheckProviderFactory.getInstance()
            }
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(factory)
    }
}
