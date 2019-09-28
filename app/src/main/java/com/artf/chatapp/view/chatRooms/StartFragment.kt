package com.artf.chatapp.view.chatRooms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.artf.chatapp.databinding.FragmentStartBinding
import com.artf.chatapp.utils.FragmentState
import com.artf.chatapp.view.FirebaseViewModel
import dagger.android.support.DaggerFragment
import javax.inject.Inject

class StartFragment : DaggerFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val firebaseVm: FirebaseViewModel by activityViewModels { viewModelFactory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentStartBinding.inflate(LayoutInflater.from(context))
        binding.lifecycleOwner = this
        binding.firebaseVm = firebaseVm

        binding.recyclerView.adapter = ChatListAdapter(
            this,
            ChatListAdapter.OnClickListener { user ->
                firebaseVm.setReceiver(user.user.value)
                firebaseVm.setFragmentState(FragmentState.CHAT)
            })

        firebaseVm.setMsgList(mutableListOf())
        return binding.root
    }
}