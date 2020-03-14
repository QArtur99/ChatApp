package com.artf.chatapp.view

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.artf.chatapp.model.Chat
import com.artf.chatapp.model.Message
import com.artf.chatapp.model.User
import com.artf.chatapp.repository.FirebaseRepository
import com.artf.chatapp.repository.FirebaseUserLiveData
import com.artf.chatapp.utils.extension.add
import com.artf.chatapp.utils.extension.clear
import com.artf.chatapp.utils.extension.clearChatRoomList
import com.artf.chatapp.utils.extension.count
import com.artf.chatapp.utils.extension.get
import com.artf.chatapp.utils.extension.remove
import com.artf.chatapp.utils.states.AuthenticationState
import com.artf.chatapp.utils.states.FragmentState
import com.artf.chatapp.utils.states.NetworkState
import com.google.firebase.Timestamp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

class FirebaseViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepository
) : ViewModel() {

    private var isUsernameAvailableJob: Job? = null
    private var searchForUserJob: Job? = null

    private val _userList = MutableLiveData<List<User>>()
    val userList: LiveData<List<User>> = _userList

    private val _userSearchStatus = MutableLiveData<NetworkState>()
    val userSearchStatus: LiveData<NetworkState> = _userSearchStatus

    private val _pushImgStatus = MutableLiveData<NetworkState>()
    val pushImgStatus: LiveData<NetworkState> = _pushImgStatus

    private val _pushAudioStatus = MutableLiveData<NetworkState>()
    val pushAudioStatus: LiveData<NetworkState> = _pushAudioStatus

    private val _chatRoomList = MutableLiveData<List<Chat>>()
    val chatRoomList: LiveData<List<Chat>> = _chatRoomList

    private val _msgList = MutableLiveData<List<Message>>()
    val msgList: LiveData<List<Message>> = _msgList

    private val _msgLength = MutableLiveData<Int>()
    val msgLength: LiveData<Int> = _msgLength

    private val _usernameStatus = MutableLiveData<NetworkState>()
    val usernameStatus: LiveData<NetworkState> = _usernameStatus

    private val _fragmentState = MutableLiveData<FragmentState>()
    val fragmentState: LiveData<FragmentState> = _fragmentState

    private val _receiver = MutableLiveData<User>()
    val receiver: LiveData<User> = _receiver

    init {
        firebaseRepository.startListening()
        _msgList.value = arrayListOf()
        firebaseRepository.fetchConfigMsgLength { _msgLength.value = it }
        firebaseRepository.onFragmentStateChanged = { setFragmentState(it) }
        setMsgListener()
        firebaseRepository.onMsgList = {
            _msgList.value = it
            it.map { msg -> if (msg.audioUrl != null) getAudio(msg) }
        }
        firebaseRepository.onChatRoomList = { _chatRoomList.value = it }
        setOnChatRoomListSort()
    }

    val authenticationState = FirebaseUserLiveData().map { user ->
        if (user != null) {
            viewModelScope.launch { firebaseRepository.onSignedIn(user.uid) }
            onSignIn()
            AuthenticationState.Authenticated(user.uid)
        } else {
            viewModelScope.launch { firebaseRepository.onSignedOut() }
            onSignOut()
            AuthenticationState.Unauthenticated
        }
    }

    fun setMsgList(msgList: List<Message>) {
        _msgList.value = msgList
    }

    fun setFragmentState(fragmentState: FragmentState?) {
        _fragmentState.value = fragmentState
    }

    fun setReceiver(user: User?) {
        _receiver.value = user
        user?.let { firebaseRepository.setChatRoomId(user.userId!!) }
    }

    private fun getAudio(msg: Message) {
        viewModelScope.launch {
            msg.setAudioDownloaded(false)
            firebaseRepository.getAudio(msg)
            msg.setAudioDownloaded(true)
        }
    }

    private fun onSignIn() {
    }

    private fun onSignOut() {
        _msgList.clear()
        _chatRoomList.clearChatRoomList()
    }

    private fun setOnChatRoomListSort() {
        firebaseRepository.onChatRoomListSort = {
            val sortedList = _chatRoomList.value?.sortedByDescending {
                val timestamp = it.message.value?.timestamp
                if (timestamp is Timestamp) timestamp.seconds else 0
            }
            _chatRoomList.value = sortedList
        }
    }

    private fun setMsgListener() {
        firebaseRepository.onChildAdded = { _msgList.add(it) }
        firebaseRepository.onChildChanged = {
            _msgList.remove(_msgList.get(_msgList.count() - 1))
            _msgList.add(it)
        }
    }

    fun onSearchTextChange(newText: String) {
        searchForUserJob?.cancel()
        searchForUserJob = viewModelScope.launch {
            firebaseRepository.searchForUser(newText) { networkState, userList ->
                _userList.value = userList
                _userSearchStatus.value = networkState
            }
        }
    }

    fun isUsernameAvailable(username: String) {
        isUsernameAvailableJob?.cancel()
        isUsernameAvailableJob = viewModelScope.launch {
            firebaseRepository.isUsernameAvailable(username) {
                _usernameStatus.value = it
            }
        }
    }

    fun addUsername(username: String) {
        firebaseRepository.addUsername(username) {
            _usernameStatus.value = it
            if (it == NetworkState.LOADED) setFragmentState(FragmentState.START)
        }
    }

    fun pushAudio(audioPath: String, audioDuration: Long) {
        firebaseRepository.pushAudio(audioPath, audioDuration) {
            _pushAudioStatus.value = it
        }
    }

    fun pushMsg(msg: String) {
        firebaseRepository.pushMsg(msg)
    }

    fun pushPicture(pictureUri: Uri) {
        firebaseRepository.pushPicture(pictureUri) {
            _pushImgStatus.value = it
        }
    }

    override fun onCleared() {
        _chatRoomList.clearChatRoomList()
        firebaseRepository.stopListening()
    }
}