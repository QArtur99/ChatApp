package com.artf.chatapp.view

import android.net.Uri
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artf.chatapp.data.model.Chat
import com.artf.chatapp.data.model.Message
import com.artf.chatapp.data.model.User
import com.artf.chatapp.data.repository.Repository
import com.artf.chatapp.data.source.firebase.ChatRoomListLiveData
import com.artf.chatapp.data.source.firebase.ChatRoomLiveData
import com.artf.chatapp.testing.OpenForTesting
import com.artf.chatapp.utils.extension.clear
import com.artf.chatapp.utils.states.AuthenticationState
import com.artf.chatapp.utils.states.FragmentState
import com.artf.chatapp.utils.states.NetworkState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OpenForTesting
class FirebaseViewModel @ViewModelInject constructor(
    private val repository: Repository
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

    private val _chatRoomList: ChatRoomListLiveData = repository.getChatRoomListLiveData()
    val chatRoomList: LiveData<List<Chat>> = _chatRoomList

    private val _msgList: ChatRoomLiveData = repository.getChatRoomLiveData()
    val msgList: LiveData<List<Message>> = _msgList

    private val _msgLength = MutableLiveData<Int>()
    val msgLength: LiveData<Int> = _msgLength

    private val _usernameStatus = MutableLiveData<NetworkState>()
    val usernameStatus: LiveData<NetworkState> = _usernameStatus

    private val _fragmentState = MutableLiveData<Pair<FragmentState, Boolean>>()
    val fragmentState: LiveData<Pair<FragmentState, Boolean>> = _fragmentState

    init {
        repository.fetchConfigMsgLength { _msgLength.value = it }
    }

    val user: LiveData<Pair<User?, AuthenticationState>> = repository.getUserLiveData()
    val authenticationState = Transformations.map(user) { userWithState ->
        if (userWithState.second is AuthenticationState.Authenticated) {
            userWithState.first?.username ?: setFragmentState(FragmentState.USERNAME)
        }
        userWithState.second
    }

    fun setMsgList(msgList: List<Message>) {
        _msgList.value = msgList
    }

    fun setFragmentState(fragmentState: FragmentState, notify: Boolean = true) {
        _fragmentState.value = Pair(fragmentState, notify)
    }

    fun setReceiver(user: User?) {
        user?.userId?.let { _msgList.receiverId = it }
    }

    fun onSignIn() {
    }

    fun onSignOut() {
        _msgList.clear()
        _chatRoomList.clear()
    }

    fun onSearchTextChange(newText: String) {
        searchForUserJob?.cancel()
        searchForUserJob = viewModelScope.launch {
            repository.searchForUser(newText) { networkState, userList ->
                _userList.value = userList
                _userSearchStatus.value = networkState
            }
        }
    }

    fun isUsernameAvailable(username: String) {
        isUsernameAvailableJob?.cancel()
        isUsernameAvailableJob = viewModelScope.launch {
            repository.isUsernameAvailable(username) {
                _usernameStatus.value = it
            }
        }
    }

    fun addUsername(username: String) {
        repository.addUsername(username) {
            _usernameStatus.value = it
            if (it == NetworkState.LOADED) setFragmentState(FragmentState.START)
        }
    }

    fun pushAudio(audioPath: String, audioDuration: Long) {
        _msgList.pushAudio(audioPath, audioDuration) {
            _pushAudioStatus.value = it
        }
    }

    fun pushMsg(msg: String) {
        _msgList.pushMsg(msg)
    }

    fun pushPicture(pictureUri: Uri) {
        _msgList.pushPicture(pictureUri) {
            _pushImgStatus.value = it
        }
    }
}