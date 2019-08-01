package com.artf.chatapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.artf.chatapp.databinding.FragmentStartBinding

class StartFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentStartBinding.inflate(LayoutInflater.from(context))
        return binding.root
    }
}