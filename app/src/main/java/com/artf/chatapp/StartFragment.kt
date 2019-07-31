package com.artf.chatapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import com.artf.chatapp.databinding.FragmentStartBinding
import com.artf.chatapp.utils.getVm

class StartFragment : Fragment() {

    private val firebaseVm by lazy { getVm<FirebaseViewModel>() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentStartBinding.inflate(LayoutInflater.from(activity!!))

        firebaseVm.fragmentState.observe(this, Observer {
            it?.let {
                when (it) {
                    FragmentState.USERNAME -> binding.root.findNavController().navigate(R.id.fragment_username)
                    FragmentState.MAIN -> binding.root.findNavController().navigate(R.id.fragment_main)
                }
            }
        })
        return binding.root
    }
}