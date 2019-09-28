package com.artf.chatapp.view.userProfile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.artf.chatapp.R
import com.artf.chatapp.databinding.FragmentUsernameBinding
import com.artf.chatapp.utils.Status
import com.artf.chatapp.utils.extension.afterTextChangedLowerCase
import com.artf.chatapp.view.FirebaseViewModel
import dagger.android.support.DaggerFragment
import javax.inject.Inject

class UsernameFragment : DaggerFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val firebaseVm: FirebaseViewModel by activityViewModels { viewModelFactory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentUsernameBinding.inflate(LayoutInflater.from(context))
        binding.usernameButton.setOnClickListener { firebaseVm.addUsername(binding.usernameEditText.text.toString()) }

        binding.usernameEditText.afterTextChangedLowerCase { text ->
            if (text.isNotEmpty() && text.length > 3) {
                firebaseVm.isUsernameAvailable(text)
            } else {
                binding.progressBar.visibility = View.GONE
                binding.usernameButton.visibility = View.VISIBLE
                binding.usernameButton.isEnabled = false
                binding.usernameEditText.isSelected = false
                binding.usernameErrorTextView.text = getString(R.string.usernameHint)
                binding.usernameErrorTextView.setTextColor(ContextCompat.getColor(context!!,
                    R.color.colorText
                ))
            }
        }

        firebaseVm.usernameStatus.observe(this, Observer {
            when (it.status) {
                Status.RUNNING -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.usernameButton.visibility = View.GONE
                }
                Status.SUCCESS -> {
                    binding.usernameErrorTextView.text = getString(R.string.usernameHint)
                    binding.usernameErrorTextView.setTextColor(ContextCompat.getColor(context!!,
                        R.color.colorText
                    ))
                    binding.progressBar.visibility = View.GONE
                    binding.usernameButton.visibility = View.VISIBLE
                    binding.usernameButton.isEnabled = true
                    binding.usernameEditText.isSelected = false
                }
                Status.FAILED -> {
                    binding.progressBar.visibility = View.GONE
                    binding.usernameButton.visibility = View.VISIBLE
                    binding.usernameButton.isEnabled = false
                    binding.usernameEditText.isSelected = true
                    binding.usernameErrorTextView.text = getString(R.string.usernameError)
                    binding.usernameErrorTextView.setTextColor(ContextCompat.getColor(activity!!,
                        R.color.colorError
                    ))
                }
            }
        })

        return binding.root
    }
}