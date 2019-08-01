package com.artf.chatapp.utils.extension


import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import com.artf.chatapp.repository.FirebaseRepository
import com.artf.chatapp.ViewModelFactory
fun AppCompatActivity.getVmFactory(): ViewModelFactory {
    val repository = FirebaseRepository(this)
    return ViewModelFactory(repository)
}

inline fun <reified T : ViewModel> AppCompatActivity.getVm(): T {
    return ViewModelProviders.of(this, getVmFactory()).get(T::class.java)
}