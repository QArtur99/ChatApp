package com.artf.chatapp.ui

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.hasChildCount
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artf.chatapp.R
import com.artf.chatapp.data.model.Chat
import com.artf.chatapp.ui.util.ViewModelFactory
import com.artf.chatapp.ui.util.atPosition
import com.artf.chatapp.ui.util.launchFragmentInHiltContainer
import com.artf.chatapp.util.any
import com.artf.chatapp.util.mock
import com.artf.chatapp.util.nullable
import com.artf.chatapp.view.FirebaseViewModel
import com.artf.chatapp.view.chatRooms.ChatListAdapter
import com.artf.chatapp.view.chatRooms.StartFragment
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class StartFragmentTest {

    companion object {
        val viewModel: FirebaseViewModel = mock()
    }

    private val chatRoomList = MutableLiveData<List<Chat>>()
    private lateinit var appContext: Context

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun init() {
        Mockito.reset(viewModel)
        appContext = InstrumentationRegistry.getInstrumentation().context
        `when`(viewModel.chatRoomList).thenReturn(chatRoomList)
        launchFragmentInHiltContainer<StartFragmentTest>()
    }

    @Test
    fun chatRoomList() {
        val chat = Chat("1_2", "2", "3", true)
        val chatList = mutableListOf(chat, chat, chat)
        chatRoomList.postValue(chatList)
        onView(withId(R.id.recyclerView)).check(matches(hasChildCount(chatList.size)))
        val rv = onView((withId(R.id.recyclerView)))
        rv.check(matches(atPosition(0, R.id.photoImageView, isDisplayed())))
        rv.check(matches(atPosition(0, R.id.onlineDot, isDisplayed())))
    }

    @Test
    fun chatListAdapterItemClick() {
        val chat = Chat("1_2", "2", "3", true)
        val chatList = mutableListOf(chat, chat, chat)
        chatRoomList.postValue(chatList)
        onView(withId(R.id.recyclerView)).perform(
            actionOnItemAtPosition<ChatListAdapter.MsgViewHolder>(0, click())
        )
        verify(viewModel).setReceiver(nullable())
        verify(viewModel, times(2)).setFragmentState(any(), any())
    }

    class StartFragmentTest : StartFragment() {
        override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory {
            return ViewModelFactory(viewModel)
        }
    }
}