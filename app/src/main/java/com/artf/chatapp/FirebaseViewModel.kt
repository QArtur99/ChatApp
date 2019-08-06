package com.artf.chatapp

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.artf.chatapp.model.Message
import com.artf.chatapp.repository.FirebaseRepository
import com.artf.chatapp.utils.FragmentState
import com.artf.chatapp.utils.NetworkState
import com.artf.chatapp.utils.extension.*

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

    private val _fragmentState = MutableLiveData<FragmentState>()
    val fragmentState: LiveData<FragmentState> = _fragmentState
    fun setFragmentState(fragmentState: FragmentState?) {
        _fragmentState.value = fragmentState
    }


    init {
        _msgData.value = arrayListOf()
        firebaseRepository.fetchConfigMsgLength { _msgLength.value = it }
        firebaseRepository.onFragmentStateChanged = { setFragmentState(it) }
        setOnSignOutListener()
        setMsgListener()
    }

    private fun setOnSignOutListener() {
        firebaseRepository.onSignOut = {
            _msgData.clear()
            setStartSignInActivity(true)
        }
    }

    private fun setMsgListener() {
        firebaseRepository.onChildAdded = { _msgData.add(it) }
        firebaseRepository.onChildChanged = {
            _msgData.remove(_msgData.get(_msgData.count() - 1))
            _msgData.add(it)
        }
    }

    fun isUsernameAvailable(username: String) {
        firebaseRepository.isUsernameAvailable(username) {
            _usernameStatus.value = it
        }
    }

    fun addUsername(username: String) {
        firebaseRepository.addUsername(username) {
            _usernameStatus.value = it
            if (it == NetworkState.LOADED) setFragmentState(FragmentState.MAIN)
        }
    }

    fun putPicture(data: Intent?) {
        firebaseRepository.putPicture(data)
    }

    fun pushMsg(msg: String, photoUrl: String?) {
        firebaseRepository.pushMsg(msg, photoUrl)
    }

    override fun onCleared() {
        firebaseRepository.removeListener()
    }
}