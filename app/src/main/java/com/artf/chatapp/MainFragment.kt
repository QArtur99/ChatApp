package com.artf.chatapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import com.artf.chatapp.databinding.FragmentMainBinding
import com.artf.chatapp.repository.FirebaseRepository
import com.artf.chatapp.utils.extension.afterTextChanged
import com.artf.chatapp.utils.extension.getVm

class MainFragment : Fragment() {

    private val firebaseVm by lazy { getVm<FirebaseViewModel>() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentMainBinding.inflate(LayoutInflater.from(context))
        binding.lifecycleOwner = this
        binding.firebaseVm = firebaseVm

        binding.recyclerView.adapter = MsgAdapter(MsgAdapter.OnClickListener { product ->
            //movieDetailViewModel.onReviewListItemClick(product)
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