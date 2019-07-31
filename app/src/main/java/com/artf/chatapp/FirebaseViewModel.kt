package com.artf.chatapp

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.artf.chatapp.model.Message
import com.artf.chatapp.utils.*

class FirebaseViewModel(val firebaseRepository: FirebaseRepository) : ViewModel() {


    private val _msgData = MutableLiveData<List<Message>>()
    val msgData: LiveData<List<Message>> = _msgData

    private val _msgLength = MutableLiveData<Int>()
    val msgLength: LiveData<Int> = _msgLength

    private val _usernameStatus = MutableLiveData<NetworkState>()
    val usernameStatus: LiveData<NetworkState> = _usernameStatus

    private val _startSignInActivity = MutableLiveData<Boolean>()
    val startSignInActivity: LiveData<Boolean> = _startSignInActivity
    fun setStartSignInActivity(start: Boolean?) {
        _startSignInActivity.value = start
    }


    init {
        _msgData.value = arrayListOf()
        firebaseRepository.fetchConfig { _msgLength.value = it }
    }

    fun isUsernameAvailable(username: String) {
        _usernameStatus.value = NetworkState.LOADING
        firebaseRepository.isUsernameAvailable(username) {
            _usernameStatus.value = it
        }
    }

    fun addUsername(username: String){
        firebaseRepository.addUsername(username)
    }

    fun putPicture(data: Intent?){
        firebaseRepository.putPicture(data)
    }

    fun pushMsg(msg: String, photoUrl: String?) {
        firebaseRepository.pushMsg(msg, photoUrl)
    }

    fun onResume() {
        firebaseRepository.onResume()
    }

    fun onPause() {
        firebaseRepository.onPause()
    }

    fun setFireBaseLoginCallback() {
        firebaseRepository.addFirebaseCallback(object : FirebaseRepository.FirebaseCallback {
            override fun onSignedOut() {
                _msgData.clear()
            }

            override fun startMainFragment() {

            }

            override fun startUsernameFragment() {

            }

            override fun onChildChanged(message: Message) {
                _msgData.add(message)
            }

            override fun onChildAdded(message: Message) {
                _msgData.remove(_msgData.get(_msgData.count() - 1))
                _msgData.add(message)
            }

            override fun startSignInActivity() {
                setStartSignInActivity(true)
            }

        })

    }

}