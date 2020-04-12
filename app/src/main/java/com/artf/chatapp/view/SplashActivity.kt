package com.artf.chatapp.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import com.artf.chatapp.utils.Utility

class SplashActivity : AppCompatActivity() {

    companion object {
        private const val INITIAL_REQUEST = 1337
        private val INITIAL_PERMS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()
    }

    private fun startApp() {
        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
        finish()
    }

    private fun checkPermissions() {
        if (checkHasPermissions().not()) {
            ActivityCompat.requestPermissions(this, INITIAL_PERMS, INITIAL_REQUEST)
        } else {
            startApp()
        }
    }

    private fun checkHasPermissions(): Boolean {
        var result = false
        for (i in INITIAL_PERMS) {
            result = if (isPermissionGranted(i)) true else return false
        }
        return result
    }

    private fun isPermissionGranted(i: String): Boolean {
        return checkSelfPermission(this, i) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, @NonNull permissions: Array<String>, @NonNull grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            INITIAL_REQUEST -> if (checkHasPermissions()) {
                startApp()
            } else {
                Utility.makeText(this, "Permissions are necessary")
                Handler().postDelayed({ checkPermissions() }, 1500)
            }
        }
    }
}