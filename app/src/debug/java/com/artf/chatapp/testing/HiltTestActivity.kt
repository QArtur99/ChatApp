package com.artf.chatapp.testing

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HiltTestActivity : AppCompatActivity() {

    override val defaultViewModelProviderFactory = HiltTestActivityHelper.vmFactory

    override fun onDestroy() {
        super.onDestroy()
        HiltTestActivityHelper.restVmFactory()
    }
}

object HiltTestActivityHelper {
    var vmFactory = object : ViewModelProvider.Factory {}

    fun restVmFactory() {
        vmFactory = object : ViewModelProvider.Factory {}
    }
}
