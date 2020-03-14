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
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.artf.chatapp.R
import com.artf.chatapp.databinding.ActivityMainBinding
import com.artf.chatapp.model.User
import com.artf.chatapp.repository.FirebaseRepository
import com.artf.chatapp.utils.FileHelper
import com.artf.chatapp.utils.convertFromString
import com.artf.chatapp.utils.states.AuthenticationState
import com.artf.chatapp.utils.states.FragmentState
import com.firebase.ui.auth.AuthUI
import dagger.android.support.DaggerAppCompatActivity
import java.io.File
import javax.inject.Inject

class MainActivity : DaggerAppCompatActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: ActivityMainBinding
    private val navigationManager by lazy { NavigationManager(this, binding) }
    private val firebaseVm: FirebaseViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        observeAuthState()
        observeFragmentState()

        checkNotificationIntent()
        supportActionBar?.hide()
    }

    private fun observeAuthState() {
        firebaseVm.authenticationState.observe(this, Observer {
            when (it) {
                is AuthenticationState.Authenticated -> onAuthenticated()
                is AuthenticationState.Unauthenticated -> onUnathenticated()
                is AuthenticationState.InvalidAuthentication -> TODO()
            }
        })
    }

    private fun observeFragmentState() {
        firebaseVm.fragmentState.observe(this, Observer {
            it?.let { navigationManager.onFragmentStateChange(it) }
        })
    }

    private fun onAuthenticated() {
        supportActionBar?.show()
    }

    private fun onUnathenticated() {
        startSignInActivity()
        supportActionBar?.hide()
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
        if (requestCode == FirebaseRepository.RC_SIGN_IN) {
            when (resultCode) {
                Activity.RESULT_OK -> supportActionBar?.show()
                Activity.RESULT_CANCELED -> finish()
            }
        } else if (requestCode == FirebaseRepository.RC_PHOTO_PICKER && resultCode == Activity.RESULT_OK) {
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
                .build(), FirebaseRepository.RC_SIGN_IN
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
