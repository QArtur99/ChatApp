package com.artf.chatapp.view

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.artf.chatapp.data.model.Chat
import com.artf.chatapp.data.model.Message
import com.artf.chatapp.data.model.User
import com.artf.chatapp.data.repository.ChatRoomListLiveData
import com.artf.chatapp.data.repository.ChatRoomLiveData
import com.artf.chatapp.data.repository.FirebaseRepository
import com.artf.chatapp.data.repository.FirebaseUserLiveData
import com.artf.chatapp.utils.extension.add
import com.artf.chatapp.utils.extension.clear
import com.artf.chatapp.utils.extension.count
import com.artf.chatapp.utils.extension.get
import com.artf.chatapp.utils.extension.remove
import com.artf.chatapp.utils.states.AuthenticationState
import com.artf.chatapp.utils.states.FragmentState
import com.artf.chatapp.utils.states.NetworkState
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

    private val _chatRoomListRaw: ChatRoomListLiveData = ChatRoomListLiveData()
    private val _chatRoomList: MutableLiveData<List<Chat>> =
        Transformations.switchMap(_chatRoomListRaw) {
            MutableLiveData<List<Chat>>().apply { value = it }
        } as MutableLiveData<List<Chat>>
    val chatRoomList: LiveData<List<Chat>> = _chatRoomList

    private val _msgListRaw: ChatRoomLiveData = ChatRoomLiveData()
    private val _msgList: MutableLiveData<List<Message>> =
        Transformations.switchMap(_msgListRaw) {
            MutableLiveData<List<Message>>().apply { value = it }
        } as MutableLiveData<List<Message>>
    val msgList: LiveData<List<Message>> = _msgList

    private val _msgLength = MutableLiveData<Int>()
    val msgLength: LiveData<Int> = _msgLength

    private val _usernameStatus = MutableLiveData<NetworkState>()
    val usernameStatus: LiveData<NetworkState> = _usernameStatus

    private val _fragmentState = MutableLiveData<Pair<FragmentState, Boolean>>()
    val fragmentState: LiveData<Pair<FragmentState, Boolean>> = _fragmentState

    private val _receiver = MutableLiveData<User>()
    val receiver: LiveData<User> = _receiver

    init {
        firebaseRepository.startListening()
        firebaseRepository.fetchConfigMsgLength { _msgLength.value = it }
        firebaseRepository.onFragmentStateChanged = { setFragmentState(it) }
        setMsgListener()
    }

    val authenticationState = FirebaseUserLiveData().map { user ->
        if (user != null) {
            viewModelScope.launch {
                firebaseRepository.onSignedIn(user.uid)
                _chatRoomListRaw.setNewDocRef(firebaseRepository.mUser!!)
                _msgListRaw.user = firebaseRepository.mUser!!
            }
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

    fun setFragmentState(fragmentState: FragmentState, notify: Boolean = true) {
        _fragmentState.value = Pair(fragmentState, notify)
    }

    fun setReceiver(user: User?) {
        _receiver.value = user
        user?.userId?.let { _msgListRaw.receiverId = it }
    }

    private fun onSignIn() {
    }

    private fun onSignOut() {
        _msgList.clear()
        _chatRoomList.clear()
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
        _msgListRaw.pushAudio(audioPath, audioDuration) {
            _pushAudioStatus.value = it
        }
    }

    fun pushMsg(msg: String) {
        _msgListRaw.pushMsg(msg)
    }

    fun pushPicture(pictureUri: Uri) {
        _msgListRaw.pushPicture(pictureUri) {
            _pushImgStatus.value = it
        }
    }
}