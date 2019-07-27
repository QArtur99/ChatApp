package com.artf.chatapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.artf.chatapp.databinding.DialogUsernameBinding

class UsernameDialog : DialogFragment() {

    var clickListener: ((string: String) -> Unit)? = null
    var isUserNameAvailable: ((string: String) -> Unit)? = null
    var userNameAvailable: LiveData<Boolean>? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DialogUsernameBinding.inflate(LayoutInflater.from(activity))
        binding.searchButton.setOnClickListener { clickListener?.invoke(binding.usernameEditText.text.toString()) }

        binding.usernameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                val text = charSequence.toString().trim { it <= ' ' }
                    if(text.isNotEmpty() && text.length > 3) {
                        isUserNameAvailable?.let { it(charSequence.toString()) }
                    }else{
                        binding.searchButton.isEnabled = false
                    }
            }

            override fun afterTextChanged(editable: Editable) {}
        })

        userNameAvailable?.observe(this, Observer {
            binding.searchButton.isEnabled = it
        })
        return binding.root
    }
}