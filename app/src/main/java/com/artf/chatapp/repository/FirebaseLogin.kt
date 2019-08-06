package com.artf.chatapp.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.artf.chatapp.model.User
import com.google.firebase.auth.FirebaseAuth

class FirebaseLogin {

    var mUser: User? = null
    private val firebaseAuth by lazy { FirebaseAuth.getInstance() }
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    private val _auth = MutableLiveData<FirebaseLoginState>()
    val auth: LiveData<FirebaseLoginState> = _auth

    init {
        authStateListener = getAuthStateListener()
        firebaseAuth.addAuthStateListener(authStateListener!!)
    }

    private fun getAuthStateListener(): FirebaseAuth.AuthStateListener {
        return FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) onSignedIn(user.uid) else onSignedOut()
        }
    }

    private fun onSignedIn(userId: String) {
        mUser = User(userId)
        _auth.value = FirebaseLoginState.SIGN_IN
    }

    private fun onSignedOut() {
        this.mUser = null
        _auth.value = FirebaseLoginState.SIGN_OUT
    }

    fun removeListener() {
        if (authStateListener != null) firebaseAuth.removeAuthStateListener(authStateListener!!)
    }
}