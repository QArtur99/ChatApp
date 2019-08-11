package com.artf.chatapp

import android.app.Application
import com.artf.chatapp.repository.FirebaseRepository
import com.google.firebase.database.FirebaseDatabase

class App : Application() {

    companion object {
        lateinit var repository: FirebaseRepository
    }

    override fun onCreate() {
        super.onCreate()
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        repository = FirebaseRepository()
    }
}