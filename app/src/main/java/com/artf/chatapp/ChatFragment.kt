package com.artf.chatapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.artf.chatapp.databinding.FragmentChatBinding
import com.artf.chatapp.model.Message
import com.artf.chatapp.repository.FirebaseRepository
import com.artf.chatapp.utils.NetworkState
import com.artf.chatapp.utils.extension.afterTextChanged
import com.artf.chatapp.utils.extension.getVm
import java.io.IOException

class ChatFragment : Fragment() {

    private val fileName: String by lazy { "${activity!!.externalCacheDir?.absolutePath}/audiorecordtest.3gp" }
    private val LOG_TAG = "AudioRecordTest"
    private val RC_RECORD_AUDIO = 200
    private var permissionToRecord = false
    private var permissions: Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO)

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null


    private val firebaseVm by lazy { getVm<FirebaseViewModel>() }
    private lateinit var binding: FragmentChatBinding
    private lateinit var adapter: MsgAdapter

    @SuppressLint("ClickableViewAccessibility")
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
            binding.sendButton.isActivated = text.isNotBlank()
        }

        binding.sendButton.setOnClickListener {
            if (it.isActivated) {
                firebaseVm.pushMsg(binding.messageEditText.text.toString())
            }
            binding.messageEditText.setText("")
        }

        binding.sendButton.setOnTouchListener { view, motionEvent ->
            val permissionCheck = checkSelfPermission(activity!!, permissions[0])
            permissionToRecord = permissionCheck == PackageManager.PERMISSION_GRANTED
            if (permissionToRecord.not()) {
                requestPermissions(permissions, RC_RECORD_AUDIO)
                return@setOnTouchListener true
            }
            if (view.isActivated.not() && permissionToRecord) {
                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startRecording()
                    }
                    MotionEvent.ACTION_UP -> {
                        stopRecording()
                        firebaseVm.pushAudio(fileName)
                    }
                }
            }else{
                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startPlaying()
                    }
                    MotionEvent.ACTION_UP -> {
                        stopPlaying()
                    }
                }
            }
            true
        }

        binding.sendButton.isActivated = false
        return binding.root
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionToRecord = if (requestCode == RC_RECORD_AUDIO && grantResults.isNotEmpty()) {
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        } else false
    }

    private fun startPlaying() {
        player = MediaPlayer().apply {
            try {
                setDataSource(fileName)
                prepare()
                start()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "prepare() failed")
            }
        }
    }

    private fun stopPlaying() {
        player?.stop()
        player?.release()
        player = null
    }

    private fun startRecording() {
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(fileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            try {
                prepare()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "prepare() failed")
            }

            start()
        }
    }

    private fun stopRecording() {
        recorder?.stop()
        recorder?.release()
        recorder = null
    }


    override fun onStop() {
        super.onStop()
        stopPlaying()
        stopRecording()
    }


}