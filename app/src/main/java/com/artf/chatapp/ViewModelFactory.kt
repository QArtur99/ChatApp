package com.artf.chatapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.artf.chatapp.repository.FirebaseRepository

/**
 * Factory for all ViewModels.
 */
@Suppress("UNCHECKED_CAST")
class ViewModelFactory constructor(
    val repository: FirebaseRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>) =
        with(modelClass) {
            when {
                isAssignableFrom(FirebaseViewModel::class.java) -> FirebaseViewModel(repository)
                else ->
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        } as T
}
