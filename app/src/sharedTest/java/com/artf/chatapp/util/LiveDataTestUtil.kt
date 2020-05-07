package com.artf.chatapp.util

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object LiveDataTestUtil {

    /**
     * Get the value from a LiveData object. We're waiting for LiveData to emit, for 2 seconds.
     * Once we got a notification via onChanged, we stop observing.
     */

    fun <T> getValue(liveData: LiveData<T>): T {
        val data = arrayOfNulls<Any>(1)
        val latch = CountDownLatch(1)
        val observer = object : Observer<T> {
            override fun onChanged(o: T?) {
                data[0] = o
                latch.countDown()
                liveData.removeObserver(this)
            }
        }
        liveData.observeForever(observer)
        latch.await(2, TimeUnit.SECONDS)

        @Suppress("UNCHECKED_CAST")
        return data[0] as T
    }

    fun <T> getValueUI(liveData: LiveData<T>, activity: Activity): T {
        val data = arrayOfNulls<Any>(1)
        val latch = CountDownLatch(2)
        val observer = object : Observer<T> {
            override fun onChanged(o: T?) {
                data[0] = o
                latch.countDown()
                if (latch.count == 0L) liveData.removeObserver(this)
            }
        }
        activity.runOnUiThread { liveData.observeForever(observer) }
        latch.await(2, TimeUnit.SECONDS)

        @Suppress("UNCHECKED_CAST")
        return data[0] as T
    }

    fun <T> isNotInvoke(liveData: LiveData<T>, callBack: () -> Unit) {
        var counter = 0
        val latch = CountDownLatch(1)
        val observer = object : Observer<T> {
            override fun onChanged(o: T?) {
                if (counter > 0) {
                    liveData.removeObserver(this)
                    throw IllegalStateException("LiveData was invoked")
                }
                counter++
            }
        }
        liveData.observeForever(observer)
        callBack()
        latch.await(2, TimeUnit.SECONDS)
        liveData.removeObserver(observer)
    }
}
