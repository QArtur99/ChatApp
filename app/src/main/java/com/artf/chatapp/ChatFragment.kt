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
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.artf.chatapp.databinding.FragmentChatBinding
import com.artf.chatapp.model.Message
import com.artf.chatapp.repository.FirebaseRepository
import com.artf.chatapp.utils.NetworkState
import com.artf.chatapp.utils.Utility
import com.artf.chatapp.utils.extension.afterTextChanged
import com.artf.chatapp.utils.extension.getVm
import java.io.IOException
import java.util.Timer
import java.util.TimerTask

class ChatFragment : Fragment() {

    companion object {
        val TAG = ChatFragment::class.java.simpleName
        const val RC_RECORD_AUDIO = 200
        const val LOADING = "loading"
    }

    private var recordFileName: String? = null
    private var playFileName: String? = null
    private var permissionToRecord = false
    private var permissions: Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO)

    private var pauseTime: Int? = null
    private var playButton: View? = null
    private var seekBar: SeekBar? = null
    private var audioTimeTextView: TextView? = null
    private var timer: Timer? = null
    private var recorder: MediaRecorder? = null
    private var recorderDuration: Long? = null
    private var player: MediaPlayer? = null

    private val fakeMsgAudio = Message(
        audioUrl = LOADING,
        audioFile = LOADING,
        isOwner = true
    ).apply { setAudioDownloaded(false) }

    private val fakeMsg = Message(
        photoUrl = LOADING,
        isOwner = true
    )

    private val firebaseVm by lazy { getVm<FirebaseViewModel>() }
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
        val permissionCheck = checkSelfPermission(activity!!, permissions[0])
        permissionToRecord = permissionCheck == PackageManager.PERMISSION_GRANTED
        if (permissionToRecord.not()) {
            requestPermissions(permissions, RC_RECORD_AUDIO)
            return true
        }

        if (permissionToRecord) {
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    startRecording()
                }
                MotionEvent.ACTION_UP -> {
                    stopRecording()
                    val recordFileName = recordFileName ?: return false
                    val recorderDuration = recorderDuration ?: 0
                    if (recorderDuration > 1000) {
                        fakeMsgAudio.apply {
                            audioFile = recordFileName
                            audioDuration = recorderDuration
                        }
                        firebaseVm.pushAudio(recordFileName, recorderDuration)
                    }
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionToRecord = if (requestCode == RC_RECORD_AUDIO && grantResults.isNotEmpty()) {
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        } else false
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
                    stopTimer()
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar, item: Message) {
                val playButton =
                    (seekBar.parent as ConstraintLayout).findViewById<View>(R.id.playButton)
                if (playButton.isActivated) {
                    stopTimer()
                    stopPlaying()
                    playButton?.let { if (it.isActivated) startPlaying { getTime() } }
                } else {
                    val audioTimeTextView =
                        (seekBar.parent as ConstraintLayout).findViewById<TextView>(R.id.audioTimeTextView)
                    item.audioDuration?.let { setAudioTime(seekBar, audioTimeTextView, it) }
                }
            }

            override fun onAudioClick(view: View, message: Message) {
                if (message.audioDownloaded.value != true) return
                if (playButton != view) {
                    pauseTime = player?.currentPosition
                    stopPlaying()
                    playButton?.isActivated = false
                }

                if (view.isActivated.not()) {
                    playButton = view
                    seekBar = (view.parent as ConstraintLayout).findViewById(R.id.seekBar)
                    audioTimeTextView =
                        (view.parent as ConstraintLayout).findViewById(R.id.audioTimeTextView)
                    playFileName = message.audioFile
                    stopPlaying()
                    startPlaying { getTime() }
                    view.isActivated = true
                } else {
                    pauseTime = player?.currentPosition
                    stopPlaying()
                    view.isActivated = false
                }
            }
        }
    }

    fun setAudioTime(seekBar: SeekBar, textView: TextView, duration: Long) {
        val time = duration * seekBar.progress / 100
        Utility.setAudioTimeMmSs(textView, time)
    }

    fun getTime(): Int {
        val totalDuration = player?.duration
        return totalDuration!! * seekBar?.progress!! / 100
    }

    private fun startPlaying(getPosition: () -> Int) {
        player = MediaPlayer()
        player?.apply {

            setOnCompletionListener {
                audioTimeTextView?.let { Utility.setAudioTimeMmSs(it, duration.toLong()) }
                stopPlaying()
                playButton?.isActivated = false
                seekBar?.progress = 0
            }

            try {
                setDataSource(playFileName)
                prepare()
                start()
                seekTo(getPosition.invoke())
                setSeekBarTimer()
            } catch (e: IOException) {
                Log.e(TAG, "prepare() failed")
            }
        }
    }

    private fun setSeekBarTimer() {
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (player != null) {
                    activity?.runOnUiThread {
                        if (player == null) return@runOnUiThread
                        seekBar?.progress = player!!.currentPosition * 100 / player!!.duration
                        audioTimeTextView?.let {
                            Utility.setAudioTimeMmSs(
                                it,
                                player!!.currentPosition.toLong()
                            )
                        }
                    }
                } else {
                    timer?.cancel()
                }
            }
        }, 0, 20)
    }

    private fun stopPlaying() {
        stopTimer()
        try {
            player?.stop()
            player?.release()
            player = null
        } catch (stopException: RuntimeException) {
            Log.e(TAG, "stop() failed")
        }
    }

    private fun stopTimer() {
        timer?.cancel()
        timer = null
    }

    private fun startRecording() {
        recordFileName = Utility.createMediaFile(
            context!!,
            App.RECORDS_FOLDER_NAME,
            App.DATE_FORMAT,
            App.RECORD_PREFIX,
            App.RECORD_EXT
        )?.absolutePath

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(recordFileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            try {
                prepare()
                start()
                recorderDuration = System.currentTimeMillis()
            } catch (e: IOException) {
                Log.e(TAG, "prepare() failed")
            } catch (e: IllegalStateException) {
                Log.e(TAG, "prepare() failed")
                Toast.makeText(
                    context!!,
                    resources.getString(R.string.record_failed),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun stopRecording() {
        recorderDuration?.let { recorderDuration = System.currentTimeMillis().minus(it) }
        try {
            recorder?.stop()
            recorder?.release()
            recorder = null
        } catch (stopException: RuntimeException) {
            Log.e(TAG, "stop() failed")
        }
    }

    override fun onStop() {
        super.onStop()
        stopPlaying()
        stopRecording()
    }
}