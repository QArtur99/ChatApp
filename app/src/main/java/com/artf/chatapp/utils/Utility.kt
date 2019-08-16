package com.artf.chatapp.utils

import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class Utility {
    companion object {
        public fun getTimeStamp(): Long {
            return System.currentTimeMillis()
        }

        public fun setAudioTimeMmSs(textView: TextView, duration: Long) {
            val df = SimpleDateFormat("mm:ss", Locale.getDefault())
            textView.text = df.format(Date(duration))
        }
    }
}