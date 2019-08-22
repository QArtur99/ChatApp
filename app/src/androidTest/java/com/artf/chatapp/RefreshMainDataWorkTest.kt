package com.artf.chatapp

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import com.artf.chatapp.work.NotificationWorker
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RefreshMainDataWorkTest {

    private lateinit var context: Context
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }
    @Test
    fun testRefreshMainDataWork() {
        // Get the ListenableWorker
        val worker = TestListenableWorkerBuilder<NotificationWorker>(context).build()
        // Start the work synchronously
        val result = worker.startWork().get()
        assertThat(result, `is`(Result.success()))
    }
}