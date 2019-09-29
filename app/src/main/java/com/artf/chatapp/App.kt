package com.artf.chatapp

import android.annotation.SuppressLint
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.artf.chatapp.di.DaggerAppComponent
import com.artf.chatapp.work.NotificationWorker
import com.google.firebase.database.FirebaseDatabase
import dagger.android.AndroidInjector
import dagger.android.support.DaggerApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class App : DaggerApplication(), LifecycleObserver {

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerAppComponent.factory().create(applicationContext)
    }

    companion object {
        var tempReceiverId = ""
        var receiverId = ""
    }

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        delayedInit()
    }

    private val applicationScope = CoroutineScope(Dispatchers.Default)

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

        val repeatingRequest =
            PeriodicWorkRequestBuilder<NotificationWorker>(
                900,
                TimeUnit.SECONDS,
                300,
                TimeUnit.SECONDS
            ).setConstraints(constraints).build()

        repeatingRequest.workSpec.intervalDuration = 15000
        repeatingRequest.workSpec.flexDuration = 5000
        repeatingRequest.workSpec.initialDelay = 1000

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            NotificationWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            repeatingRequest
        )
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