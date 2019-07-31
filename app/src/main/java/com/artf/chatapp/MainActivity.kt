package com.artf.chatapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.artf.chatapp.databinding.ActivityMainBinding
import com.artf.chatapp.utils.getVm
import com.firebase.ui.auth.AuthUI

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val firebaseVm by lazy { getVm<FirebaseViewModel>() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        firebaseVm.startSignInActivity.observe(this, Observer {
            it?.let{
                startSignInActivity()
                firebaseVm.setStartSignInActivity(null)
            }
        })
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FirebaseRepository.RC_SIGN_IN) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show()
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, "Sign in canceled", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else if (requestCode == FirebaseRepository.RC_PHOTO_PICKER && resultCode == Activity.RESULT_OK) {
            firebaseVm.putPicture(data)
        }
    }

    private fun startSignInActivity() {
        val providers =
            mutableListOf(
                AuthUI.IdpConfig.EmailBuilder().build(),
                AuthUI.IdpConfig.FacebookBuilder().build(),
                AuthUI.IdpConfig.GoogleBuilder().build(),
                AuthUI.IdpConfig.PhoneBuilder().build()
            )

        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setIsSmartLockEnabled(false)
                .setAvailableProviders(providers)
                .setLogo(R.mipmap.ic_launcher)
                .build(), FirebaseRepository.RC_SIGN_IN
        )
    }

    override fun onResume() {
        super.onResume()
        firebaseVm.onResume()
    }

    override fun onPause() {
        super.onPause()
        firebaseVm.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.sign_out_menu -> {
                AuthUI.getInstance().signOut(this)
                true
            }
            R.id.change_username -> {
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
