package com.artf.chatapp

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
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
import java.io.File
import java.util.concurrent.TimeUnit

class App : DaggerApplication(), LifecycleObserver {

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerAppComponent.factory().create(applicationContext)
    }

    companion object {
        const val AUTHORITY = BuildConfig.APPLICATION_ID + ".provider"
        const val DATE_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        const val PHOTO_EXT = ".jpg"
        const val PHOTO_PREFIX = "JPG"
        const val PHOTOS_FOLDER_NAME = "Pics"
        const val RECORD_EXT = ".3gp"
        const val RECORD_PREFIX = "AUDIO"
        const val RECORDS_FOLDER_NAME = "Records"
        lateinit var app: Application
        lateinit var fileName: String
        lateinit var workManager: WorkManager
        var currentPhotoPath = ""
        var tempReceiverId = ""
        var receiverId = ""

        /** Use external media if it is available, our app's file directory otherwise */
        fun getOutputDirectory(context: Context, folderName: String): File {
            val appContext = context.applicationContext
            val rootPath = appContext.resources.getString(R.string.app_name) + "/" + folderName
            val mediaDir = appContext.externalMediaDirs.firstOrNull()?.let {
                File(it, rootPath).apply { mkdirs() }
            }
            return if (mediaDir != null && mediaDir.exists()) mediaDir else appContext.filesDir
        }
    }

    override fun onCreate() {
        super.onCreate()
        app = this
        fileName = "${this.externalCacheDir?.absolutePath}/%1s$RECORD_EXT"

        workManager = WorkManager.getInstance(this)
        workManager.cancelAllWork()

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
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

        WorkManager.getInstance(app).enqueueUniquePeriodicWork(
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