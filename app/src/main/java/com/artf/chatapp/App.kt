package com.artf.chatapp

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.*
import com.artf.chatapp.repository.FirebaseRepository
import com.artf.chatapp.work.NotificationWorker
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit





class App : Application(), LifecycleObserver {

    companion object {
        lateinit var app: Application
        lateinit var fileName: String
        lateinit var workManager: WorkManager
        lateinit var repository: FirebaseRepository
        var tempReceiverId = ""
        var receiverId = ""
    }

    override fun onCreate() {
        super.onCreate()
        app = this
        fileName = "${this.externalCacheDir?.absolutePath}/%1s.3gp"

        workManager = WorkManager.getInstance(this)
        workManager.cancelAllWork()

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)

        repository = FirebaseRepository()
        delayedInit()
    }

    val applicationScope = CoroutineScope(Dispatchers.Default)

    private fun delayedInit() {
        applicationScope.launch {
            setupRecurringWork()
        }
    }

    @SuppressLint("RestrictedApi")
    private fun setupRecurringWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val repeatingRequest
                = PeriodicWorkRequestBuilder<NotificationWorker>(900, TimeUnit.SECONDS,300, TimeUnit.SECONDS)
            .setConstraints(constraints)
            .build()

        repeatingRequest.workSpec.intervalDuration = 15000
        repeatingRequest.workSpec.flexDuration = 5000
        repeatingRequest.workSpec.initialDelay = 1000

        WorkManager.getInstance(app).enqueueUniquePeriodicWork(
            NotificationWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            repeatingRequest)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onMoveToForeground() {
        receiverId = tempReceiverId
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onMoveToBackground() {
        tempReceiverId = receiverId
        receiverId = ""
    }
}