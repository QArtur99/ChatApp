package com.artf.chatapp.view

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artf.chatapp.data.model.Chat
import com.artf.chatapp.data.model.Message
import com.artf.chatapp.data.model.User
import com.artf.chatapp.data.repository.ChatRoomListLiveData
import com.artf.chatapp.data.repository.ChatRoomLiveData
import com.artf.chatapp.data.repository.FirebaseRepository
import com.artf.chatapp.data.repository.UserLiveData
import com.artf.chatapp.utils.extension.clear
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

    init {
        firebaseRepository.fetchConfigMsgLength { _msgLength.value = it }
    }

    val user = UserLiveData()
    val authenticationState = Transformations.map(user) { userWithState ->
        userWithState.first?.let { user ->
            _chatRoomListRaw.setNewDocRef(user)
            _msgListRaw.user = user
        } ?: setFragmentState(FragmentState.USERNAME)
        when (userWithState.second) {
            is AuthenticationState.Authenticated -> onSignIn()
            is AuthenticationState.Unauthenticated -> onSignOut()
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
        user?.userId?.let { _msgListRaw.receiverId = it }
    }

    private fun onSignIn() {
    }

    private fun onSignOut() {
        _msgList.clear()
        _chatRoomList.clear()
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
        user.value?.first?.let { user ->
            firebaseRepository.addUsername(user, username) {
                _usernameStatus.value = it
                if (it == NetworkState.LOADED) setFragmentState(FragmentState.START)
            }
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