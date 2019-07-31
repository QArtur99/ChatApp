package com.artf.chatapp.utils

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import com.artf.chatapp.repository.FirebaseRepository
import com.artf.chatapp.ViewModelFactory


fun Fragment.getVmFactory(): ViewModelFactory {
    val repository = FirebaseRepository(activity!! as AppCompatActivity)
    return ViewModelFactory(repository)
}

inline fun <reified T : ViewModel> Fragment.getVm(): T {
    return ViewModelProviders.of(this.activity!!, getVmFactory()).get(T::class.java)
}