package com.artf.chatapp.ui

import android.content.Context
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artf.chatapp.R
import com.artf.chatapp.util.any
import com.artf.chatapp.util.mock
import com.artf.chatapp.utils.states.NetworkState
import com.artf.chatapp.view.FirebaseViewModel
import com.artf.chatapp.view.userProfile.UsernameFragment
import org.hamcrest.CoreMatchers.not
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
class UsernameFragmentTest {

    companion object {
        val viewModel: FirebaseViewModel = mock()
    }

    private val usernameStatusLd = MutableLiveData<NetworkState>()
    private lateinit var scenario: FragmentScenario<UsernameFragmentTest>
    private lateinit var appContext: Context

    @Before
    fun init() {
        appContext = InstrumentationRegistry.getInstrumentation().context
        `when`(viewModel.usernameStatus).thenReturn(usernameStatusLd)
        scenario = launchFragmentInContainer<UsernameFragmentTest>()
    }

    @Test
    fun usernameButton() {
        onView(withId(R.id.usernameButton)).perform(click())
        verify(viewModel, never()).addUsername(any())
    }

    @Test
    fun usernameEditText() {
        onView(withId(R.id.usernameEditText)).perform(typeText("Test"))
        verify(viewModel, times(1)).isUsernameAvailable(any())
    }

    @Test
    fun signOut() {
        openActionBarOverflowOrOptionsMenu(appContext)
        onView(withText(R.string.sign_out)).perform(click())
    }

    @Test
    fun usernameStatusRUNNING() {
        usernameStatusLd.postValue(NetworkState.LOADING)
        onView(withId(R.id.progressBar)).check(matches(isDisplayed()))
        onView(withId(R.id.usernameButton)).check(matches(not(isDisplayed())))
    }

    @Test
    fun usernameStatusSUCCESS() {
        usernameStatusLd.postValue(NetworkState.LOADED)
        onView(withId(R.id.usernameButton)).check(matches(isDisplayed()))
        onView(withId(R.id.usernameButton)).check(matches(isEnabled()))
        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    @Test
    fun usernameStatusFAILED() {
        usernameStatusLd.postValue(NetworkState.FAILED)
        onView(withId(R.id.usernameButton)).check(matches(isDisplayed()))
        onView(withId(R.id.usernameButton)).check(matches(not(isEnabled())))
        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
    }

    class UsernameFragmentTest : UsernameFragment() {

        override fun injectMembers() {
            this.viewModelFactory = ViewModelFactory(viewModel)
        }

        @Suppress("UNCHECKED_CAST")
        class ViewModelFactory<T>(private val mock: T) : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>) = mock as T
        }
    }
}