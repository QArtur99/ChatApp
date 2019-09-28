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
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import com.artf.chatapp.App
import com.artf.chatapp.R
import com.artf.chatapp.databinding.ActivityMainBinding
import com.artf.chatapp.model.User
import com.artf.chatapp.repository.FirebaseRepository
import com.artf.chatapp.utils.FragmentState
import com.artf.chatapp.utils.convertFromString
import com.firebase.ui.auth.AuthUI
import dagger.android.support.DaggerAppCompatActivity
import java.io.File
import javax.inject.Inject

class MainActivity : DaggerAppCompatActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: ActivityMainBinding
    private val firebaseVm: FirebaseViewModel by viewModels { viewModelFactory }

    private val navOptions = NavOptions.Builder().setLaunchSingleTop(true).build()
    private val uriUsername = Uri.parse("atr:fragment_username")
    private val uriChat = Uri.parse("atr:fragment_chat")
    private val uriSearch = Uri.parse("atr:fragment_search")
    private val uriStart = Uri.parse("atr:fragment_start")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        firebaseVm.startSignInActivity.observe(this, Observer {
            it?.let {
                startSignInActivity()
                supportActionBar?.hide()
                firebaseVm.setStartSignInActivity(null)
            }
        })

        firebaseVm.signIn.observe(this, Observer {
            supportActionBar?.show()
            //binding.splash.visibility = View.GONE
        })
        setFragmentStateListener()
        checkNotificationIntent()
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

    private fun setFragmentStateListener() {
        Navigation.setViewNavController(binding.root, findNavController(R.id.nav_host_fragment))

        binding.root.findNavController()
            .addOnDestinationChangedListener { controller, destination, arguments ->
                when (destination.label) {
                    "ChatFragment" -> {
                    }
                    else -> App.receiverId = ""
                }
            }

        firebaseVm.fragmentState.observe(this, Observer {
            it?.let {
                when (it) {
                    FragmentState.USERNAME -> binding.root.findNavController().navigate(
                        uriUsername,
                        navOptions
                    )
                    FragmentState.START -> binding.root.findNavController().navigate(
                        uriStart,
                        navOptions
                    )
                    FragmentState.CHAT -> binding.root.findNavController().navigate(
                        uriChat,
                        navOptions
                    )
                }
            }
        })
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FirebaseRepository.RC_SIGN_IN) {
            if (resultCode == Activity.RESULT_OK) {
                supportActionBar?.show()
            } else if (resultCode == Activity.RESULT_CANCELED) {
                finish()
            }
        } else if (requestCode == FirebaseRepository.RC_PHOTO_PICKER && resultCode == Activity.RESULT_OK) {
            val picUri = if (data != null) data.data!! else galleryAddPic()
            picUri?.let { firebaseVm.pushPicture(picUri) }
        }
    }

    private fun galleryAddPic(): Uri? {
        val photoFile = File(App.currentPhotoPath)
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
                // callSearch(query)
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
