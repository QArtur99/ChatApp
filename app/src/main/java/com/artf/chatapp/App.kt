package com.artf.chatapp

import android.app.Application
import com.artf.chatapp.repository.FirebaseRepository
import com.google.firebase.database.FirebaseDatabase

class App : Application() {

    companion object {
        lateinit var fileName: String
        lateinit var repository: FirebaseRepository
    }

    override fun onCreate() {
        super.onCreate()
        fileName = "${this.externalCacheDir?.absolutePath}/%1s.3gp"
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        repository = FirebaseRepository()
    }
}