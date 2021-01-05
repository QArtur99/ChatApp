package com.artf.chatapp.view.userProfile

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.artf.chatapp.R
import com.artf.chatapp.databinding.FragmentUsernameBinding
import com.artf.chatapp.utils.extension.afterTextChangedLowerCase
import com.artf.chatapp.utils.states.Status
import com.artf.chatapp.view.FirebaseViewModel
import com.firebase.ui.auth.AuthUI
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
open class UsernameFragment : Fragment() {

    private val firebaseVm: FirebaseViewModel by viewModels({ requireActivity() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentUsernameBinding.inflate(LayoutInflater.from(context))
        binding.usernameButton.setOnClickListener { firebaseVm.addUsername(binding.usernameEditText.text.toString()) }

        observeUsernameEditText(binding)
        observeUsernameStatus(binding)
        binding.root.onBackPress()
        setHasOptionsMenu(true)
        return binding.root
    }

    private fun observeUsernameEditText(binding: FragmentUsernameBinding) {
        binding.usernameEditText.afterTextChangedLowerCase { text ->
            if (text.isNotEmpty() && text.length > 3) {
                firebaseVm.isUsernameAvailable(text)
            } else {
                binding.progressBar.visibility = View.GONE
                binding.usernameButton.visibility = View.VISIBLE
                binding.usernameButton.isEnabled = false
                binding.usernameEditText.isSelected = false
                binding.usernameErrorTextView.text = getString(R.string.usernameHint)
                binding.usernameErrorTextView.setTextColor(getColor(R.color.colorText))
            }
        }
    }

    private fun observeUsernameStatus(binding: FragmentUsernameBinding) {
        firebaseVm.usernameStatus.observe(viewLifecycleOwner, Observer {
            when (it.status) {
                Status.RUNNING -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.usernameButton.visibility = View.GONE
                }
                Status.SUCCESS -> {
                    binding.usernameErrorTextView.text = getString(R.string.usernameHint)
                    binding.usernameErrorTextView.setTextColor(getColor(R.color.colorText))
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
                    binding.usernameErrorTextView.setTextColor(getColor(R.color.colorError))
                }
            }
        })
    }

    private fun getColor(color: Int): Int {
        return ContextCompat.getColor(requireActivity(), color)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.username_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun View.onBackPress() {
        this.isFocusableInTouchMode = true
        this.requestFocus()
        this.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                AuthUI.getInstance().signOut(requireContext())
                true
            } else {
                false
            }
        }
    }
}