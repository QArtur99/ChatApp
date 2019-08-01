package com.artf.chatapp.utils.extension

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.util.regex.Pattern

fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}

fun EditText.afterTextChangedLowerCase(afterTextChangedLowerCase: (String) -> Unit) {
    val upperCaseRegex = Pattern.compile("[A-Z]")
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            editable?.let {
                val matcher = upperCaseRegex.matcher(editable)
                while (matcher.find()) {
                    val upperCaseRegion = editable.subSequence(matcher.start(), matcher.end())
                    editable.replace(matcher.start(), matcher.end(), upperCaseRegion.toString().toLowerCase())
                }
            }
            afterTextChangedLowerCase.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}