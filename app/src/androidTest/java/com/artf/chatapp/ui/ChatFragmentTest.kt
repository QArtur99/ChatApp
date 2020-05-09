package com.artf.chatapp.ui

import android.content.Context
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artf.chatapp.util.mock
import com.artf.chatapp.utils.states.NetworkState
import com.artf.chatapp.view.FirebaseViewModel
import com.artf.chatapp.view.userProfile.UsernameFragment
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito

@RunWith(AndroidJUnit4::class)
class ChatFragmentTest {

    companion object {
        val viewModel: FirebaseViewModel = mock()
    }

    private val usernameStatusLd = MutableLiveData<NetworkState>()
    private lateinit var scenario: FragmentScenario<ChatFragmentTest>
    private lateinit var appContext: Context

    @Before
    fun init() {
        appContext = InstrumentationRegistry.getInstrumentation().context
        Mockito.`when`(viewModel.usernameStatus).thenReturn(usernameStatusLd)
        scenario = launchFragmentInContainer<ChatFragmentTest>()
    }

    @Test
    fun todo() {
        // TODO all for this fragment
    }

    class ChatFragmentTest : UsernameFragment() {

        override fun injectMembers() {
            this.viewModelFactory = ViewModelFactory(viewModel)
        }

        @Suppress("UNCHECKED_CAST")
        class ViewModelFactory<T>(private val mock: T) : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>) = mock as T
        }
    }
}