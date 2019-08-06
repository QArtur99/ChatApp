package com.artf.chatapp.repository

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.artf.chatapp.MainApplication
import com.artf.chatapp.model.User
import javax.inject.Inject

open class FirebaseBaseRepository(private val activity: AppCompatActivity) {

    @Inject
    lateinit var firebaseLogin: FirebaseLogin

    fun init() {
        (activity.application as MainApplication).component.inject(this)
        setAuthObserver(activity)
    }

    private fun setAuthObserver(activity: AppCompatActivity) {
        firebaseLogin.auth.observe(activity, Observer { firebaseLoginState ->
            firebaseLoginState?.let {
                when (it) {
                    FirebaseLoginState.SIGN_IN -> onSignedIn()
                    FirebaseLoginState.SIGN_OUT -> onSignedOut()
                }
            }
        })
    }

    open fun onSignedIn() {}
    open fun onSignedOut() {}

    fun getUser(): User {
        return firebaseLogin.mUser!!
    }

    fun removeAuthListener() {
        firebaseLogin.removeListener()
    }
}