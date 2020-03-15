package com.artf.chatapp.utils.extension

import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.net.URL

fun String.saveTo(path: String) {
    val file = File(path)
    if (file.exists()) return
    URL(this).openStream().use { input ->
        try {
            if (file.parentFile?.exists() != true) file.parentFile?.mkdirs()
            file.createNewFile()
            FileOutputStream(file).use { output -> input.copyTo(output) }
        } catch (e: Exception) {
            Log.e("TAG", e.toString())
            throw e
        }
    }
}