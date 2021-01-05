package com.artf.chatapp.ui.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Suppress("UNCHECKED_CAST")
class ViewModelFactory<T>(private val mock: T) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>) = mock as T
}