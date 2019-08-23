package com.artf.chatapp.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.TextView
import androidx.core.content.FileProvider
import com.artf.chatapp.App
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Utility {
    data class ResultCameraIntent(val intent: Intent? = null, val picPath: String = "")
    companion object {
        fun getTimeStamp(): Long {
            return System.currentTimeMillis()
        }

        fun setAudioTimeMmSs(textView: TextView, duration: Long) {
            val df = SimpleDateFormat("mm:ss", Locale.getDefault())
            textView.text = df.format(Date(duration))
        }

        fun getCameraIntent(context: Context, photoFile: File): Intent? {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePictureIntent.resolveActivity(context.packageManager) == null) return null
            val imageUri: Uri = FileProvider.getUriForFile(context, App.AUTHORITY, photoFile)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            return takePictureIntent
        }

        @SuppressLint("SimpleDateFormat")
        fun createImageFile(context: Context, dateFormat: String, photoExt: String): File? {
            var photoFile: File? = null
            try {
                val timeStamp: String = SimpleDateFormat(dateFormat).format(Date())
                val storageDir = App.getOutputDirectory(context, "Pics")
                photoFile = File(storageDir, "JPG_$timeStamp" + photoExt)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return photoFile
        }
    }
}