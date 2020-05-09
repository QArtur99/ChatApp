package com.artf.chatapp.ui.util

import android.view.View
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView

import androidx.test.espresso.matcher.BoundedMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher

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