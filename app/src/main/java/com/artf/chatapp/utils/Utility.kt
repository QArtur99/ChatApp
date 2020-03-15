package com.artf.chatapp.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Utility {

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
        val imageUri: Uri = FileProvider.getUriForFile(context, FileHelper.AUTHORITY, photoFile)
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        return takePictureIntent
    }

    fun makeText(activity: AppCompatActivity, stringId: Int) {
        Toast.makeText(activity, activity.resources.getString(stringId), Toast.LENGTH_LONG).show()
    }

    fun makeText(activity: AppCompatActivity, string: String) {
        Toast.makeText(activity, string, Toast.LENGTH_LONG).show()
    }
}