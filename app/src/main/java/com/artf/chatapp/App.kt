package com.artf.chatapp

import android.app.Application
import androidx.work.WorkManager
import com.artf.chatapp.repository.FirebaseRepository
import com.google.firebase.database.FirebaseDatabase

class App : Application() {

    companion object {
        lateinit var fileName: String
        lateinit var workManager: WorkManager
        lateinit var repository: FirebaseRepository
    }

    override fun onCreate() {
        super.onCreate()
        fileName = "${this.externalCacheDir?.absolutePath}/%1s.3gp"
        workManager = WorkManager.getInstance(this)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        repository = FirebaseRepository()
    }
}