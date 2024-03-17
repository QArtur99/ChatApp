package com.artf.chatapp.view

import android.app.Activity
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.MimeTypeMap
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.doOnAttach
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.artf.chatapp.R
import com.artf.chatapp.data.model.User
import com.artf.chatapp.data.source.firebase.FirebaseDaoImpl
import com.artf.chatapp.databinding.ActivityMainBinding
import com.artf.chatapp.utils.FileHelper
import com.artf.chatapp.utils.convertFromString
import com.artf.chatapp.utils.states.AuthenticationState
import com.artf.chatapp.utils.states.FragmentState
import com.firebase.ui.auth.AuthUI
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val navigationManager by lazy { NavigationManager(this, binding) }
    private val firebaseVm: FirebaseViewModel by viewModels()

    private var waitForResultFromSignIn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.root.doOnAttach { navigationManager.run { } }

        observeAuthState()
        observeFragmentState()

        checkNotificationIntent()
        supportActionBar?.hide()
    }

    private fun observeAuthState() {
        firebaseVm.authenticationState.observe(this) {
            when (it) {
                is AuthenticationState.Authenticated -> onAuthenticated()
                is AuthenticationState.Unauthenticated -> onUnauthenticated()
                is AuthenticationState.InvalidAuthentication -> TODO()
            }
        }
    }

    private fun observeFragmentState() {
        firebaseVm.fragmentState.observe(this, Observer {
            it?.let {
                if (!it.second) return@Observer
                navigationManager.onFragmentStateChange(it.first)
            }
        })
    }

    private fun onAuthenticated() {
        firebaseVm.onSignIn()
        supportActionBar?.show()
    }

    private fun onUnauthenticated() {
        if (waitForResultFromSignIn.not()) {
            firebaseVm.onSignOut()
            startSignInActivity()
            supportActionBar?.hide()
            waitForResultFromSignIn = true
        }
    }

    private fun checkNotificationIntent() {
        if (intent == null) return
        if (intent.hasExtra("userString")) {
            val user: User = convertFromString(intent.getStringExtra("userString")!!)
            firebaseVm.setReceiver(user)
            firebaseVm.setFragmentState(FragmentState.CHAT)
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FirebaseDaoImpl.RC_SIGN_IN) {
            when (resultCode) {
                Activity.RESULT_OK -> supportActionBar?.show()
                Activity.RESULT_CANCELED -> finish()
            }
            waitForResultFromSignIn = false
        } else if (requestCode == FirebaseDaoImpl.RC_PHOTO_PICKER && resultCode == Activity.RESULT_OK) {
            val picUri = if (data != null) data.data!! else galleryAddPic()
            picUri?.let { firebaseVm.pushPicture(picUri) }
        }
    }

    private fun galleryAddPic(): Uri? {
        val photoFile = File(FileHelper.currentPhotoPath)
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(photoFile.extension)
        MediaScannerConnection.scanFile(
            this,
            arrayOf(photoFile.absolutePath),
            arrayOf(mimeType),
            null
        )
        return Uri.fromFile(photoFile)
    }

    private fun startSignInActivity() {
        val providers = mutableListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.FacebookBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build(),
            AuthUI.IdpConfig.PhoneBuilder().build()
        )

        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setTheme(R.style.LoginTheme)
                .setIsSmartLockEnabled(false)
                .setAvailableProviders(providers)
                .setLogo(R.drawable.ic_launcher)
                .build(), FirebaseDaoImpl.RC_SIGN_IN
        )
        firebaseVm.setFragmentState(FragmentState.START)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)

        val searchItem = menu.findItem(R.id.search)
        val searchView = searchItem.actionView as SearchView
        setSearchViewListener(searchView)
        return true
    }

    private fun setSearchViewListener(searchView: SearchView) {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.isEmpty().not()) firebaseVm.onSearchTextChange(newText)
                return true
            }
        })
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
            R.id.search -> {
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
