package com.artf.chatapp.utils.extension

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import com.artf.chatapp.ViewModelFactory
import com.artf.chatapp.repository.FirebaseRepository


fun Fragment.getVmFactory(): ViewModelFactory {
    val repository = FirebaseRepository(activity!! as AppCompatActivity)
    return ViewModelFactory(repository)
}

inline fun <reified T : ViewModel> Fragment.getVm(): T {
    return ViewModelProviders.of(this.activity!!, getVmFactory()).get(T::class.java)
}