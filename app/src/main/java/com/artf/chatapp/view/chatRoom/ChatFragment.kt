package com.artf.chatapp.view.chatRoom

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.artf.chatapp.R
import com.artf.chatapp.data.model.Message
import com.artf.chatapp.data.source.firebase.FirebaseDaoImpl
import com.artf.chatapp.databinding.FragmentChatBinding
import com.artf.chatapp.utils.FileHelper
import com.artf.chatapp.utils.Utility
import com.artf.chatapp.utils.bindingFakeAudioProgress
import com.artf.chatapp.utils.extension.afterTextChanged
import com.artf.chatapp.utils.states.NetworkState
import com.artf.chatapp.view.FirebaseViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
open class ChatFragment : Fragment() {

    companion object {
        val TAG = ChatFragment::class.java.simpleName
        const val LOADING = "loading"
    }

    lateinit var audioHelper: AudioHelper

    private val fakeMsgAudio = Message(
        audioUrl = LOADING,
        audioFile = LOADING,
        isOwner = true
    )

    private val fakeMsg = Message(
        photoUrl = LOADING,
        isOwner = true
    )

    @Inject
    lateinit var fileHelper: FileHelper

    private val firebaseVm: FirebaseViewModel by viewModels({ requireActivity() })
    private lateinit var binding: FragmentChatBinding
    private lateinit var adapter: MsgAdapter

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentChatBinding.inflate(LayoutInflater.from(context))
        binding.lifecycleOwner = this
        binding.firebaseVm = firebaseVm

        audioHelper = AudioHelper(fileHelper, activity as AppCompatActivity)

        adapter = MsgAdapter(getMsgAdapterListener())
        binding.recyclerView.layoutAnimation = null
        binding.recyclerView.itemAnimator = null
        binding.recyclerView.adapter = adapter

        observePushPhotoState()
        observePushAudioState()

        binding.messageEditText.afterTextChanged { text ->
            binding.sendButton.isActivated = text.isNotBlank()
        }

        binding.photoPickerButton.setOnClickListener { onPhotoPickerClick() }
        binding.sendButton.setOnTouchListener(onSendButtonTouch())

        binding.sendButton.isActivated = false
        binding.progressBar.visibility = ProgressBar.INVISIBLE
        return binding.root
    }

    private fun onPhotoPickerClick() {
        val intentGallery = Intent(Intent.ACTION_PICK)
        intentGallery.type = "image/jpeg, image/png"
        intentGallery.putExtra(Intent.EXTRA_LOCAL_ONLY, true)

        val chooserIntent = Intent.createChooser(intentGallery, "Select picture")
        fileHelper.createPhotoMediaFile()?.let { photoFile ->
            val cameraIntent = Utility.getCameraIntent(requireContext(), photoFile)
            FileHelper.currentPhotoPath = photoFile.absolutePath
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
        }

        requireActivity().startActivityForResult(chooserIntent, FirebaseDaoImpl.RC_PHOTO_PICKER)
    }

    private fun onSendButtonTouch() = object : View.OnTouchListener {
        override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
            when (view.isActivated) {
                true -> onSendButtonClick(motionEvent, view)
                false -> if (onMicButtonClick(motionEvent)) return true
            }

            when (motionEvent.action) {
                MotionEvent.ACTION_UP -> view.isPressed = false
                MotionEvent.ACTION_DOWN -> view.isPressed = true
            }
            return true
        }
    }

    private fun onSendButtonClick(motionEvent: MotionEvent, view: View) {
        when (motionEvent.action) {
            MotionEvent.ACTION_UP -> {
                if (view.isActivated) pushMsg()
                binding.messageEditText.setText("")
            }
        }
    }

    private fun pushMsg() {
        firebaseVm.pushMsg(binding.messageEditText.text.toString())
    }

    private fun onMicButtonClick(motionEvent: MotionEvent): Boolean {
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> audioHelper.startRecording()
            MotionEvent.ACTION_UP -> {
                audioHelper.stopRecording()
                val recordFileName = audioHelper.recordFileName ?: return false
                val recorderDuration = audioHelper.recorderDuration ?: 0
                if (recorderDuration > 1000) pushAudio(recordFileName, recorderDuration)
            }
        }
        return false
    }

    private fun pushAudio(recordFileName: String, recorderDuration: Long) {
        fakeMsgAudio.apply {
            audioFile = recordFileName
            audioDuration = recorderDuration
        }
        firebaseVm.pushAudio(recordFileName, recorderDuration)
    }

    private fun observePushPhotoState() {
        firebaseVm.pushImgStatus.observe(viewLifecycleOwner, Observer {
            when (it) {
                NetworkState.LOADING -> {
                    val newList = mutableListOf<Message>()
                    newList.addAll(adapter.currentList)
                    newList.add(fakeMsg)
                    adapter.submitList(newList) { scrollToPosition() }
                    adapter.notifyDataSetChanged()
                }
            }
        })
    }

    private fun observePushAudioState() {
        firebaseVm.pushAudioStatus.observe(viewLifecycleOwner, Observer {
            when (it) {
                NetworkState.LOADING -> {
                    val newList = mutableListOf<Message>()
                    newList.addAll(adapter.currentList)
                    newList.add(fakeMsgAudio)
                    adapter.submitList(newList) { scrollToPosition() }
                    adapter.notifyDataSetChanged()
                }
            }
        })
    }

    private fun scrollToPosition() {
        binding.recyclerView.layoutManager?.scrollToPosition(adapter.itemCount - 1)
    }

    private fun getMsgAdapterListener(): MsgAdapter.MsgAdapterListener {
        return object : MsgAdapter.MsgAdapterListener {
            override fun showPic(view: View, message: Message) {
                val reviewDialog = PhotoDialog(message.photoUrl!!)
                reviewDialog.show(parentFragmentManager, PhotoDialog::class.simpleName)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar, item: Message) {
                val parentView = seekBar.parent as ConstraintLayout
                val playButton = parentView.findViewById<View>(R.id.playButton)
                if (playButton.isActivated) audioHelper.stopTimer()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar, item: Message) {
                val parentView = seekBar.parent as ConstraintLayout
                val playButton = parentView.findViewById<View>(R.id.playButton) ?: return
                if (playButton.isActivated) {
                    audioHelper.stopTimer()
                    audioHelper.stopPlaying()
                    audioHelper.startPlaying()
                } else {
                    val audioTimeView = parentView.findViewById<TextView>(R.id.audioTimeTextView)
                    val audioDuration = item.audioDuration ?: return
                    audioHelper.setAudioTime(seekBar, audioTimeView, audioDuration)
                }
            }

            override fun onAudioClick(view: View, message: Message) {
                if (message.audioDownloaded.not()) bindingFakeAudioProgress(view, message)
                else audioHelper.setupAudioHelper(view, message)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        audioHelper.onStop()
    }
}