package com.artf.chatapp.utils.extension

import androidx.fragment.app.Fragment
import com.artf.chatapp.view.ViewModelFactory

fun Fragment.getVmFactory(): ViewModelFactory {
    return ViewModelFactory()
}
