package com.artf.chatapp.view

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.artf.chatapp.model.Chat
import com.artf.chatapp.model.Message
import com.artf.chatapp.model.User
import com.artf.chatapp.repository.FirebaseRepository
import com.artf.chatapp.utils.FragmentState
import com.artf.chatapp.utils.NetworkState
import com.artf.chatapp.utils.extension.add
import com.artf.chatapp.utils.extension.clear
import com.artf.chatapp.utils.extension.clearChatRoomList
import com.artf.chatapp.utils.extension.count
import com.artf.chatapp.utils.extension.get
import com.artf.chatapp.utils.extension.remove
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class FirebaseViewModel(val firebaseRepository: FirebaseRepository) : ViewModel() {

    private var viewModelJob = Job()
    private val uiScope = CoroutineScope(viewModelJob + Dispatchers.Main)
    private var isUsernameAvailableJob: Job? = null
    private var searchForUserJob: Job? = null

    private val _signIn = MutableLiveData<Boolean>()
    val signIn: LiveData<Boolean> = _signIn

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
    fun setMsgList(msgList: List<Message>) {
        _msgList.value = msgList
    }

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

    private val _receiver = MutableLiveData<User>()
    val receiver: LiveData<User> = _receiver
    fun setReceiver(user: User?) {
        _receiver.value = user
        user?.let { firebaseRepository.setChatRoomId(user.userId!!) }
    }

    init {
        firebaseRepository.startListening()
        _msgList.value = arrayListOf()
        firebaseRepository.fetchConfigMsgLength { _msgLength.value = it }
        firebaseRepository.onFragmentStateChanged = { setFragmentState(it) }
        setOnSignInListener()
        setOnSignOutListener()
        setMsgListener()
        firebaseRepository.onMsgList = { _msgList.value = it }
        firebaseRepository.onChatRoomList = { _chatRoomList.value = it }
        setOnChatRoomListSort()
    }

    private fun setOnSignInListener() {
        firebaseRepository.onSignIn = {
            _signIn.value = true
        }
    }

    private fun setOnSignOutListener() {
        firebaseRepository.onSignOut = {
            _msgList.clear()
            _chatRoomList.clearChatRoomList()
            setStartSignInActivity(true)
        }
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
        searchForUserJob = uiScope.launch {
            firebaseRepository.searchForUser(newText) { networkState, userList ->
                _userList.value = userList
                _userSearchStatus.value = networkState
            }
        }
    }

    fun isUsernameAvailable(username: String) {
        isUsernameAvailableJob?.cancel()
        isUsernameAvailableJob = uiScope.launch {
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
        viewModelJob.cancel()
        _chatRoomList.clearChatRoomList()
        firebaseRepository.stopListening()
    }
}