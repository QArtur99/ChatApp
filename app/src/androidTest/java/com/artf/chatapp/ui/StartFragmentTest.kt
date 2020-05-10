package com.artf.chatapp.ui

import android.content.Context
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
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
import com.artf.chatapp.ui.util.atPosition
import com.artf.chatapp.util.any
import com.artf.chatapp.util.mock
import com.artf.chatapp.util.nullable
import com.artf.chatapp.view.FirebaseViewModel
import com.artf.chatapp.view.chatRooms.ChatListAdapter
import com.artf.chatapp.view.chatRooms.StartFragment
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
class StartFragmentTest {

    companion object {
        val viewModel: FirebaseViewModel = mock()
    }

    private val chatRoomList = MutableLiveData<List<Chat>>()
    private lateinit var scenario: FragmentScenario<StartFragmentTest>
    private lateinit var appContext: Context

    @Before
    fun init() {
        Mockito.reset(viewModel)
        appContext = InstrumentationRegistry.getInstrumentation().context
        `when`(viewModel.chatRoomList).thenReturn(chatRoomList)
        scenario = launchFragmentInContainer<StartFragmentTest>()
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

        override fun injectMembers() {
            this.viewModelFactory = ViewModelFactory(viewModel)
        }

        @Suppress("UNCHECKED_CAST")
        class ViewModelFactory<T>(private val mock: T) : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>) = mock as T
        }
    }
}