package com.artf.chatapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.artf.chatapp.databinding.FragmentMainBinding
import com.artf.chatapp.model.Message
import com.artf.chatapp.utils.afterTextChanged
import com.artf.chatapp.utils.getVm

class MainFragment : Fragment() {

    private lateinit var mMessageAdapter: MessageAdapter
    private val firebaseVm by lazy { getVm<FirebaseViewModel>() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentMainBinding.inflate(LayoutInflater.from(activity!!))
        binding.lifecycleOwner = this
        binding.firebaseVm = firebaseVm

        // Initialize message ListView and its adapter
        val friendlyMessages = ArrayList<Message>()
        mMessageAdapter = MessageAdapter(activity!!, R.layout.item_message, friendlyMessages)
        binding.messageListView.adapter = mMessageAdapter
        firebaseVm.msgData.observe(this, Observer { msgList ->
            mMessageAdapter.clear()
            mMessageAdapter.addAll(msgList)
        })

        // Initialize progress bar
        binding.progressBar.visibility = ProgressBar.INVISIBLE

        // ImagePickerButton shows an image picker to upload a image for a message
        binding.photoPickerButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/jpeg"
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            startActivityForResult(
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