package com.artf.chatapp

import android.app.Application
import com.artf.chatapp.di.DaggerAppComponent
import com.artf.chatapp.di.FirebaseLoginModule
import com.google.firebase.database.FirebaseDatabase


class MainApplication : Application() {

    val component by lazy {
        DaggerAppComponent.builder()
            .firebaseLoginModule(FirebaseLoginModule())
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        component.inject(this)
    }
}