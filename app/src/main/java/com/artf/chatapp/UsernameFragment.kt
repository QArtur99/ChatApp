package com.artf.chatapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.artf.chatapp.databinding.FragmentUsernameBinding
import com.artf.chatapp.utils.Status
import com.artf.chatapp.utils.afterTextChanged
import com.artf.chatapp.utils.getVm

class UsernameFragment : Fragment() {

    private val firebaseVm by lazy { getVm<FirebaseViewModel>() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentUsernameBinding.inflate(LayoutInflater.from(activity!!))
        binding.usernameButton.setOnClickListener { firebaseVm.addUsername(binding.usernameEditText.text.toString()) }
        binding.usernameEditText.afterTextChanged { text ->
            if (text.isNotEmpty() && text.length > 3) {
                firebaseVm.isUsernameAvailable(text)
            } else {
                binding.usernameButton.isEnabled = false
            }
        }

        firebaseVm.usernameStatus.observe(this, Observer {
            when (it.status) {
                Status.RUNNING -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.usernameButton.visibility = View.GONE
                }
                Status.SUCCESS -> {
                    binding.progressBar.visibility = View.GONE
                    binding.usernameButton.visibility = View.VISIBLE
                    binding.usernameButton.isEnabled = true
                }
                Status.FAILED -> {
                    binding.progressBar.visibility = View.GONE
                    binding.usernameButton.visibility = View.VISIBLE
                    binding.usernameButton.isEnabled = false
                    binding.usernameEditText.error = "This username is not available."
                }
            }
        })

        return binding.root
    }
}