package com.artf.chatapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import com.artf.chatapp.databinding.ActivityMainBinding
import com.artf.chatapp.repository.FirebaseRepository
import com.artf.chatapp.utils.FragmentState
import com.artf.chatapp.utils.extension.getVm
import com.firebase.ui.auth.AuthUI

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val firebaseVm by lazy { getVm<FirebaseViewModel>() }

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

        setFragmentStateListener()
    }

    private fun setFragmentStateListener() {
        Navigation.setViewNavController(binding.root, findNavController(R.id.nav_host_fragment))
        firebaseVm.fragmentState.observe(this, Observer {
            it?.let {
                when (it) {
                    FragmentState.USERNAME -> binding.root.findNavController().navigate(uriUsername, navOptions)
                    FragmentState.START -> binding.root.findNavController().navigate(uriStart, navOptions)
                    FragmentState.CHAT -> binding.root.findNavController().navigate(uriChat, navOptions)
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
            firebaseVm.pushPicture(data)
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
                .setTheme(R.style.LoginTheme)
                .setIsSmartLockEnabled(false)
                .setAvailableProviders(providers)
                .setLogo(R.drawable.ic_launcher)
                .build(), FirebaseRepository.RC_SIGN_IN
        )
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
                //callSearch(query)
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
                //firebaseVm.setFragmentState(FragmentState.START)
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
