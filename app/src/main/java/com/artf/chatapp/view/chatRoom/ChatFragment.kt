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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.artf.chatapp.App
import com.artf.chatapp.R
import com.artf.chatapp.databinding.FragmentChatBinding
import com.artf.chatapp.model.Message
import com.artf.chatapp.repository.FirebaseRepository
import com.artf.chatapp.utils.NetworkState
import com.artf.chatapp.utils.Utility
import com.artf.chatapp.utils.extension.afterTextChanged
import com.artf.chatapp.view.FirebaseViewModel
import dagger.android.support.DaggerFragment
import javax.inject.Inject

class ChatFragment : DaggerFragment() {

    companion object {
        val TAG = ChatFragment::class.java.simpleName
        const val LOADING = "loading"
    }

    lateinit var audioHelper: AudioHelper

    private val fakeMsgAudio = Message(
        audioUrl = LOADING,
        audioFile = LOADING,
        isOwner = true
    ).apply { setAudioDownloaded(false) }

    private val fakeMsg = Message(
        photoUrl = LOADING,
        isOwner = true
    )

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val firebaseVm: FirebaseViewModel by activityViewModels { viewModelFactory }
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

        audioHelper = AudioHelper((activity as AppCompatActivity?)!!)

        adapter = MsgAdapter(viewLifecycleOwner, getMsgAdapterInt())
        binding.recyclerView.layoutAnimation = null
        binding.recyclerView.itemAnimator = null
        binding.recyclerView.adapter = adapter

        pushPhotoStateListener()
        pushAudioStateListener()

        binding.progressBar.visibility = ProgressBar.INVISIBLE

        binding.photoPickerButton.setOnClickListener {
            val intentGallery = Intent(Intent.ACTION_PICK)
            intentGallery.type = "image/jpeg, image/png"
            intentGallery.putExtra(Intent.EXTRA_LOCAL_ONLY, true)

            val chooserIntent = Intent.createChooser(intentGallery, "Select picture")
            Utility.createMediaFile(
                context!!,
                App.PHOTOS_FOLDER_NAME,
                App.DATE_FORMAT,
                App.PHOTO_PREFIX,
                App.PHOTO_EXT
            )?.let { photoFile ->
                val cameraIntent = Utility.getCameraIntent(context!!, photoFile)
                App.currentPhotoPath = photoFile.absolutePath
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
            }
            activity!!.startActivityForResult(chooserIntent, FirebaseRepository.RC_PHOTO_PICKER)
        }

        binding.messageEditText.afterTextChanged { text ->
            binding.sendButton.isActivated = text.isNotBlank()
        }

        binding.sendButton.setOnTouchListener { view, motionEvent ->
            when (view.isActivated) {
                true -> onSendButtonClick(motionEvent, view)
                false -> if (onMicButtonClick(motionEvent)) return@setOnTouchListener true
            }

            when (motionEvent.action) {
                MotionEvent.ACTION_UP -> view.isPressed = false
                MotionEvent.ACTION_DOWN -> view.isPressed = true
            }
            true
        }

        binding.sendButton.isActivated = false
        return binding.root
    }

    private fun onSendButtonClick(motionEvent: MotionEvent, view: View) {
        when (motionEvent.action) {
            MotionEvent.ACTION_UP -> {
                if (view.isActivated) {
                    firebaseVm.pushMsg(binding.messageEditText.text.toString())
                }
                binding.messageEditText.setText("")
            }
        }
    }

    private fun onMicButtonClick(motionEvent: MotionEvent): Boolean {
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                audioHelper.startRecording()
            }
            MotionEvent.ACTION_UP -> {
                audioHelper.stopRecording()
                val recordFileName = audioHelper.recordFileName ?: return false
                val recorderDuration = audioHelper.recorderDuration ?: 0
                if (recorderDuration > 1000) {
                    fakeMsgAudio.apply {
                        audioFile = recordFileName
                        audioDuration = recorderDuration
                    }
                    firebaseVm.pushAudio(recordFileName, recorderDuration)
                }
            }
        }
        return false
    }

    private fun pushPhotoStateListener() {
        firebaseVm.pushImgStatus.observe(viewLifecycleOwner, Observer {
            when (it) {
                NetworkState.LOADING -> {
                    val newList = mutableListOf<Message>()
                    newList.addAll(adapter.currentList)
                    newList.add(fakeMsg)
                    adapter.submitList(newList) {
                        binding.recyclerView.layoutManager?.scrollToPosition(
                            adapter.itemCount - 1
                        )
                    }
                    adapter.notifyDataSetChanged()
                }
                // NetworkState.LOADED -> {
                //     val newList = mutableListOf<Message>()
                //     newList.addAll(adapter.currentList)
                //     newList.remove(fakeMsg)
                //     adapter.submitList(newList) {
                //         binding.recyclerView.layoutManager?.scrollToPosition(
                //             adapter.itemCount - 1
                //         )
                //     }
                //     adapter.notifyDataSetChanged()
                // }
                // NetworkState.FAILED -> {
                //     val newList = mutableListOf<Message>()
                //     newList.addAll(adapter.currentList)
                //     newList.remove(fakeMsg)
                //     adapter.submitList(newList) {
                //         binding.recyclerView.layoutManager?.scrollToPosition(
                //             adapter.itemCount - 1
                //         )
                //     }
                //     adapter.notifyDataSetChanged()
                // }
                else -> {
                }
            }
        })
    }

    private fun pushAudioStateListener() {
        firebaseVm.pushAudioStatus.observe(viewLifecycleOwner, Observer {
            when (it) {
                NetworkState.LOADING -> {
                    val newList = mutableListOf<Message>()
                    newList.addAll(adapter.currentList)
                    newList.add(fakeMsgAudio)
                    adapter.submitList(newList) {
                        binding.recyclerView.layoutManager?.scrollToPosition(
                            adapter.itemCount - 1
                        )
                    }
                    adapter.notifyDataSetChanged()
                }
                // NetworkState.LOADED -> {
                //     val newList = mutableListOf<Message>()
                //     newList.addAll(adapter.currentList)
                //     newList.remove(fakeMsgAudio)
                //     adapter.submitList(newList) {
                //         binding.recyclerView.layoutManager?.scrollToPosition(
                //             adapter.itemCount - 1
                //         )
                //     }
                //     adapter.notifyDataSetChanged()
                // }
                // NetworkState.FAILED -> {
                //     val newList = mutableListOf<Message>()
                //     newList.addAll(adapter.currentList)
                //     newList.remove(fakeMsgAudio)
                //     adapter.submitList(newList) {
                //         binding.recyclerView.layoutManager?.scrollToPosition(
                //             adapter.itemCount - 1
                //         )
                //     }
                //     adapter.notifyDataSetChanged()
                // }
                else -> {
                }
            }
        })
    }

    private fun getMsgAdapterInt(): MsgAdapter.MsgAdapterInt {
        return object : MsgAdapter.MsgAdapterInt {
            override fun showPic(view: View, message: Message) {
                val reviewDialog = PhotoDialog(message.photoUrl!!)
                reviewDialog.show(requireFragmentManager(), PhotoDialog::class.simpleName)
            }

            override fun getVm(): FirebaseViewModel {
                return firebaseVm
            }

            override fun onStartTrackingTouch(seekBar: SeekBar, item: Message) {
                val playButton =
                    (seekBar.parent as ConstraintLayout).findViewById<View>(R.id.playButton)
                if (playButton.isActivated) {
                    audioHelper.stopTimer()
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar, item: Message) {
                val playButton =
                    (seekBar.parent as ConstraintLayout).findViewById<View>(R.id.playButton)
                if (playButton.isActivated) {
                    audioHelper.stopTimer()
                    audioHelper.stopPlaying()
                    playButton?.let { if (it.isActivated) audioHelper.startPlaying { audioHelper.getTime() } }
                } else {
                    val audioTimeTextView =
                        (seekBar.parent as ConstraintLayout).findViewById<TextView>(R.id.audioTimeTextView)
                    item.audioDuration?.let { audioHelper.setAudioTime(seekBar, audioTimeTextView, it) }
                }
            }

            override fun onAudioClick(view: View, message: Message) {
                if (message.audioDownloaded.value != true) return
                audioHelper.setupAudioHelper(view, message)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        audioHelper.onStop()
    }
}