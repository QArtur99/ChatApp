package com.artf.chatapp.ui

import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.artf.chatapp.MainCoroutineRule
import com.artf.chatapp.data.model.Message
import com.artf.chatapp.data.model.User
import com.artf.chatapp.data.repository.Repository
import com.artf.chatapp.data.source.firebase.ChatRoomListLiveData
import com.artf.chatapp.data.source.firebase.ChatRoomLiveData
import com.artf.chatapp.util.LiveDataTestUtil.getValue
import com.artf.chatapp.util.any
import com.artf.chatapp.util.mock
import com.artf.chatapp.util.nullable
import com.artf.chatapp.util.unlockThread
import com.artf.chatapp.utils.states.AuthenticationState
import com.artf.chatapp.utils.states.FragmentState
import com.artf.chatapp.view.FirebaseViewModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

@ExperimentalCoroutinesApi
class FirebaseViewModelTest {

    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @ObsoleteCoroutinesApi
    private var mainThreadSurrogate = newSingleThreadContext("UI thread")

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var sharedViewModel: FirebaseViewModel
    private val chatRoomListLiveData: ChatRoomListLiveData = mock()
    private val chatRoomLiveData: ChatRoomLiveData = mock()
    private val repository: Repository = mock()

    @ObsoleteCoroutinesApi
    @Before
    fun init() {
        Dispatchers.setMain(mainThreadSurrogate)
        setLiveData()
        sharedViewModel = FirebaseViewModel(repository)
    }

    private fun setLiveData() {
        `when`(repository.getChatRoomLiveData()).thenReturn(chatRoomLiveData)
        `when`(repository.getChatRoomListLiveData()).thenReturn(chatRoomListLiveData)
        val user = MutableLiveData<Pair<User?, AuthenticationState>>()
        `when`(repository.getUserLiveData()).thenReturn(user)
    }

    @ObsoleteCoroutinesApi
    @After
    fun tearDown() {
        Dispatchers.resetMain()
        mainThreadSurrogate.close()
    }

    @Test
    fun testNull() {
        assertThat(sharedViewModel.userList).isNotNull()
        assertThat(sharedViewModel.userSearchStatus).isNotNull()
        assertThat(sharedViewModel.pushImgStatus).isNotNull()
        assertThat(sharedViewModel.pushAudioStatus).isNotNull()
        assertThat(sharedViewModel.chatRoomList).isNotNull()
        assertThat(sharedViewModel.msgList).isNotNull()
        assertThat(sharedViewModel.msgLength).isNotNull()
        assertThat(sharedViewModel.usernameStatus).isNotNull()
        assertThat(sharedViewModel.fragmentState).isNotNull()
        assertThat(sharedViewModel.user).isNotNull()
        assertThat(sharedViewModel.authenticationState).isNotNull()
    }

    @Test
    fun testInteraction() {
        verify(repository).fetchConfigMsgLength(any())
        verify(repository).getChatRoomLiveData()
        verify(repository).getChatRoomListLiveData()
        verify(repository).getUserLiveData()
    }

    @Test
    fun testNoInteraction() {
        verify(repository, never()).addUsername(any(), any())
        runBlocking {
            verify(repository, never()).searchForUser(any(), any())
            verify(repository, never()).isUsernameAvailable(any(), any())
        }
    }

    @Test
    fun setMsgList() {
        val list = listOf<Message>()
        sharedViewModel.setMsgList(list)
        verify(chatRoomLiveData).value = any()
    }

    @Test
    fun setFragmentState() {
        val fragmentState = FragmentState.START
        val notify = true
        sharedViewModel.setFragmentState(fragmentState, notify)
        val fragmentStateValue = getValue(sharedViewModel.fragmentState)
        assertThat(fragmentStateValue.first).isEqualTo(fragmentState)
        assertThat(fragmentStateValue.second).isEqualTo(notify)
    }

    @Test
    fun setReceiver() {
        val user = User(userId = "999")
        sharedViewModel.setReceiver(user)
        verify(chatRoomLiveData).receiverId = user.userId
    }

    @Test
    fun onSearchTextChange() = runBlocking {
        sharedViewModel.onSearchTextChange("test")
        unlockThread()
        verify(repository).searchForUser(any(), any())
    }

    @Test
    fun isUsernameAvailable() = runBlocking {
        sharedViewModel.isUsernameAvailable("test")
        unlockThread()
        verify(repository).isUsernameAvailable(any(), any())
    }

    @Test
    fun addUsername() = runBlocking {
        sharedViewModel.addUsername("test")
        unlockThread()
        verify(repository).addUsername(any(), any())
    }

    @Test
    fun pushAudio() {
        val audioPath = "path"
        val audioDuration = 15L
        sharedViewModel.pushAudio(audioPath, audioDuration)
        verify(chatRoomLiveData).pushAudio(any(), any(), any())
    }

    @Test
    fun pushMsg() {
        val msg = "test"
        sharedViewModel.pushMsg(msg)
        verify(chatRoomLiveData).pushMsg(
            any(),
            nullable(),
            nullable(),
            nullable(),
            nullable(),
            nullable()
        )
    }

    @Test
    fun pushPicture() {
        val pictureUri = mock<Uri>()
        sharedViewModel.pushPicture(pictureUri)
        verify(chatRoomLiveData).pushPicture(any(), any())
    }
}