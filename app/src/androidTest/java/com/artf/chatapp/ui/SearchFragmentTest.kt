package com.artf.chatapp.ui

import android.content.Context
import android.view.View
import androidx.arch.core.executor.testing.CountingTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.hasChildCount
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import androidx.test.platform.app.InstrumentationRegistry
import com.artf.chatapp.R
import com.artf.chatapp.data.model.User
import com.artf.chatapp.testing.HiltTestActivityHelper
import com.artf.chatapp.ui.SearchFragmentTest.SearchFragmentTest.Companion.vmFactory
import com.artf.chatapp.ui.util.atPosition
import com.artf.chatapp.ui.util.launchFragmentInHiltContainer
import com.artf.chatapp.utils.states.NetworkState
import com.artf.chatapp.view.FirebaseViewModel
import com.artf.chatapp.view.searchUser.SearchAdapter
import com.artf.chatapp.view.searchUser.SearchFragment
import com.artf.sharedtest.util.any
import com.artf.sharedtest.util.mock
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class SearchFragmentTest {

    @get:Rule
    var countingRule = CountingTaskExecutorRule()

    companion object {
        val viewModel: FirebaseViewModel = mock()
    }

    private val userSearchStatus = MutableLiveData<NetworkState>()
    private val userList = MutableLiveData<List<User>>()
    private lateinit var appContext: Context

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun init() {
        Mockito.reset(viewModel)
        appContext = InstrumentationRegistry.getInstrumentation().context
        `when`(viewModel.userSearchStatus).thenReturn(userSearchStatus)
        `when`(viewModel.userList).thenReturn(userList)

        HiltTestActivityHelper.vmFactory = vmFactory
        launchFragmentInHiltContainer(SearchFragmentTest::class) {
            val fragment = this as SearchFragmentTest
            fragment.binding.root.visibility = View.VISIBLE
            fragment.binding.searchView.visibility = View.VISIBLE
        }
    }

    @Test
    fun userSearchStatusRUNNING() {
        userSearchStatus.postValue(NetworkState.LOADING)
        onView(withId(R.id.progressBar)).check(matches(isDisplayed()))
    }

    @Test
    fun userSearchStatusSUCCESS() {
        userSearchStatus.postValue(NetworkState.LOADED)
        val user1 = User(userId = "1", username = "Test")
        val user2 = User(userId = "2", username = "Test")
        val user3 = User(userId = "3", username = "Test")
        val userList = mutableListOf(user1, user2, user3)
        UiThreadStatement.runOnUiThread {
            this.userList.value = userList
        }
        onView(withId(R.id.recyclerView)).check(matches(hasChildCount(userList.size)))
        val recyclerView = onView((withId(R.id.recyclerView)))
        recyclerView.check(matches(atPosition(0, R.id.photoImageView, isDisplayed())))
    }

    @Test
    fun userSearchStatusFAILED() {
        userSearchStatus.postValue(NetworkState.FAILED)
        onView(withId(R.id.info)).check(matches(isDisplayed()))
    }

    @Test
    fun searchAdapterItemClick() {
        val user = User(userId = "1", username = "Test")
        val userList = mutableListOf(user, user, user)
        UiThreadStatement.runOnUiThread {
            this.userList.value = userList
        }
        onView(withId(R.id.recyclerView)).perform(
            actionOnItemAtPosition<SearchAdapter.MsgViewHolder>(0, ViewActions.click())
        )
        verify(viewModel).setReceiver(any())
        verify(viewModel).setFragmentState(any(), any())
    }

    class SearchFragmentTest : SearchFragment() {
        companion object {
            @Suppress("UNCHECKED_CAST")
            val vmFactory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return when (modelClass) {
                        FirebaseViewModel::class.java -> viewModel as T
                        else -> super.create(modelClass)
                    }
                }
            }
        }

        override val defaultViewModelProviderFactory = vmFactory
    }
}