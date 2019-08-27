package com.artf.chatapp.utils.extension

import androidx.appcompat.app.AppCompatActivity
import com.artf.chatapp.view.ViewModelFactory

fun AppCompatActivity.getVmFactory(): ViewModelFactory {
    return ViewModelFactory()
}