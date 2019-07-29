package com.artf.chatapp

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.artf.chatapp.databinding.DialogUsernameBinding
import com.artf.chatapp.utils.afterTextChanged

class UsernameDialog : DialogFragment() {

    var clickListener: ((string: String) -> Unit)? = null
    var isUserNameAvailable: ((string: String) -> Unit)? = null
    var usernameStatus: LiveData<NetworkState>? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DialogUsernameBinding.inflate(LayoutInflater.from(activity))
        binding.searchButton.setOnClickListener { clickListener?.invoke(binding.usernameEditText.text.toString()) }
        binding.usernameEditText.afterTextChanged { text ->
            if (text.isNotEmpty() && text.length > 3) {
                isUserNameAvailable?.let { it(text) }
            } else {
                binding.searchButton.isEnabled = false
            }
        }

        usernameStatus?.observe(this, Observer {
            when(it.status){
                Status.RUNNING -> {}
                Status.SUCCESS -> {binding.searchButton.isEnabled = true}
                Status.FAILED -> {
                    binding.searchButton.isEnabled = false
                    binding.usernameEditText.error = "This username is not available."
                }
            }
        })
        dialog!!.window?.decorView?.background =
            ColorDrawable(ContextCompat.getColor(requireContext(), R.color.transparent))
        return binding.root
    }
}