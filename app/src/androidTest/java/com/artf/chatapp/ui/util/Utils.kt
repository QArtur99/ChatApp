package com.artf.chatapp.ui.util

import android.view.View
import androidx.annotation.NonNull
import androidx.arch.core.executor.testing.CountingTaskExecutorRule
import androidx.recyclerview.widget.RecyclerView

import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.CoreMatchers
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

fun atPosition(position: Int, viewId: Int, @NonNull itemMatcher: Matcher<View?>): Matcher<View?>? {
    return object : BoundedMatcher<View?, RecyclerView>(RecyclerView::class.java) {
        override fun describeTo(description: Description) {
            description.appendText("has item at position $position: ")
            itemMatcher.describeTo(description)
        }

        override fun matchesSafely(view: RecyclerView): Boolean {
            val viewHolder = view.findViewHolderForAdapterPosition(position)
            val targetView = viewHolder?.itemView?.findViewById<View>(viewId)
            targetView ?: return false
            return itemMatcher.matches(targetView)
        }
    }
}

fun waitForAdapterChange(recyclerView: RecyclerView, countingRule: CountingTaskExecutorRule) {
    val latch = CountDownLatch(1)
    InstrumentationRegistry.getInstrumentation().runOnMainSync {
        recyclerView.adapter?.registerAdapterDataObserver(
            object : RecyclerView.AdapterDataObserver() {
                override fun onChanged() {
                    latch.countDown()
                }

                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    latch.countDown()
                }
            })
    }
    countingRule.drainTasks(1, TimeUnit.SECONDS)
    if (recyclerView.adapter?.itemCount ?: 0 > 0) {
        return
    }
    MatcherAssert.assertThat(latch.await(100, TimeUnit.SECONDS), CoreMatchers.`is`(true))
}