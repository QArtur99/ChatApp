package com.artf.chatapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.artf.chatapp.databinding.FragmentChatBinding
import com.artf.chatapp.model.Message
import com.artf.chatapp.repository.FirebaseRepository
import com.artf.chatapp.utils.NetworkState
import com.artf.chatapp.utils.extension.afterTextChanged
import com.artf.chatapp.utils.extension.getVm

class ChatFragment : Fragment() {

    private val firebaseVm by lazy { getVm<FirebaseViewModel>() }
    private lateinit var binding: FragmentChatBinding
    private lateinit var adapter: MsgAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentChatBinding.inflate(LayoutInflater.from(context))
        binding.lifecycleOwner = this
        binding.firebaseVm = firebaseVm

        adapter = MsgAdapter(MsgAdapter.OnClickListener { product ->
            //movieDetailViewModel.onReviewListItemClick(product)
        })

        binding.recyclerView.layoutAnimation = null
        binding.recyclerView.itemAnimator = null
        binding.recyclerView.adapter = adapter

        val fakeMsg = Message(
            photoUrl = "loading",
            isOwner = true
        )

        firebaseVm.pushImgStatus.observe(viewLifecycleOwner, Observer {
            when (it) {
                NetworkState.LOADING -> {
                    val newList = mutableListOf<Message>()
                    newList.addAll(adapter.currentList)
                    newList.add(fakeMsg)
                    adapter.submitList(newList) { binding.recyclerView.layoutManager?.scrollToPosition(adapter.itemCount - 1) }
                    adapter.notifyDataSetChanged()
                }
                NetworkState.LOADED -> {
                    val newList = mutableListOf<Message>()
                    newList.addAll(adapter.currentList)
                    newList.remove(fakeMsg)
                    adapter.submitList(newList) { binding.recyclerView.layoutManager?.scrollToPosition(adapter.itemCount - 1) }
                    adapter.notifyDataSetChanged()
                }
                NetworkState.FAILED -> {
                    val newList = mutableListOf<Message>()
                    newList.addAll(adapter.currentList)
                    newList.remove(fakeMsg)
                    adapter.submitList(newList) { binding.recyclerView.layoutManager?.scrollToPosition(adapter.itemCount - 1) }
                    adapter.notifyDataSetChanged()
                }
                else -> {
                }
            }
        })

        binding.progressBar.visibility = ProgressBar.INVISIBLE

        binding.photoPickerButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/jpeg"
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            activity!!.startActivityForResult(
                Intent.createChooser(intent, "Complete action using"),
                FirebaseRepository.RC_PHOTO_PICKER
            )
        }

        binding.messageEditText.afterTextChanged { text ->
            binding.sendButton.isEnabled = text.isNotEmpty()
        }

        binding.sendButton.setOnClickListener {
            firebaseVm.pushMsg(binding.messageEditText.text.toString(), null)
            binding.messageEditText.setText("")
        }

        return binding.root
    }
}