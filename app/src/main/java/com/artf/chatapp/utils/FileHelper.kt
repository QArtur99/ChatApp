package com.artf.chatapp.utils

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.artf.chatapp.BuildConfig
import com.artf.chatapp.R
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileHelper @Inject constructor(private val app: Application) {
    companion object {
        const val AUTHORITY = BuildConfig.APPLICATION_ID + ".provider"
        const val DATE_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        const val PHOTO_EXT = ".jpg"
        const val PHOTO_PREFIX = "JPG"
        const val PHOTOS_FOLDER_NAME = "Pics"
        const val RECORD_EXT = ".3gp"
        const val RECORD_PREFIX = "AUDIO"
        const val RECORDS_FOLDER_NAME = "Records"
        lateinit var fileName: String
        var currentPhotoPath = ""

        @SuppressLint("SimpleDateFormat")
        fun createMediaFile(
            context: Context,
            folderName: String,
            dateFormat: String,
            prefix: String,
            extension: String
        ): File? {
            var photoFile: File? = null
            try {
                val timeStamp: String = SimpleDateFormat(dateFormat).format(Date())
                val storageDir = getOutputDirectory(context, folderName)
                photoFile = File(storageDir, "${prefix}_${timeStamp}$extension")
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return photoFile
        }

        /** Use external media if it is available, our app's file directory otherwise */
        private fun getOutputDirectory(context: Context, folderName: String): File {
            val appContext = context.applicationContext
            val rootPath = appContext.resources.getString(R.string.app_name) + "/" + folderName
            val mediaDir = appContext.externalMediaDirs.firstOrNull()?.let {
                File(it, rootPath).apply { mkdirs() }
            }
            return if (mediaDir != null && mediaDir.exists()) mediaDir else appContext.filesDir
        }
    }

    init {
        fileName = "${app.externalCacheDir?.absolutePath}/%1s$RECORD_EXT"
    }
}