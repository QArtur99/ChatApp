package com.artf.chatapp.view.chatRoom

import android.media.MediaPlayer
import android.media.MediaRecorder
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.artf.chatapp.R
import com.artf.chatapp.data.model.Message
import com.artf.chatapp.utils.FileHelper
import com.artf.chatapp.utils.Utility
import java.io.IOException
import java.util.Timer
import java.util.TimerTask

class AudioHelper(
    private val fileHelper: FileHelper,
    private val activity: AppCompatActivity
) {

    private var playButton: View? = null
    private var seekBar: SeekBar? = null
    private var audioTimeTextView: TextView? = null

    private var playFileName: String? = null
    var recordFileName: String? = null
    private var player: MediaPlayer? = null
    private var recorder: MediaRecorder? = null

    private var timer: Timer? = null
    private var pauseTime: Int? = null
    var recorderDuration: Long? = null

    fun setupAudioHelper(view: View, message: Message) {
        if (playButton != view) {
            pauseTime = player?.currentPosition
            stopPlaying()
            playButton?.isActivated = false
        }

        if (view.isActivated.not()) {
            playButton = view
            val parentView = view.parent as ConstraintLayout
            seekBar = parentView.findViewById(R.id.seekBar)
            audioTimeTextView = parentView.findViewById(R.id.audioTimeTextView)
            playFileName = message.audioFile
            stopPlaying()
            startPlaying()
            view.isActivated = true
        } else {
            pauseTime = player?.currentPosition
            stopPlaying()
            view.isActivated = false
        }
    }

    private fun setSeekBarTimer() {
        timer = Timer().apply { scheduleAtFixedRate(getTimerTask(), 0, 20) }
    }

    private fun getTimerTask() = object : TimerTask() {
        override fun run() {
            if (player == null) timer?.cancel() else activity.runOnUiThread { setSeekBarPosition() }
        }
    }

    private fun setSeekBarPosition() {
        val player = player ?: return
        val audioTimeTextView = audioTimeTextView ?: return
        seekBar?.progress = player.currentPosition * 100 / player.duration
        Utility.setAudioTimeMmSs(audioTimeTextView, player.currentPosition.toLong())
    }

    fun setAudioTime(seekBar: SeekBar, textView: TextView, duration: Long) {
        val time = duration * seekBar.progress / 100
        Utility.setAudioTimeMmSs(textView, time)
    }

    fun startPlaying() {
        player = MediaPlayer().apply {
            setOnCompletionListener { onCompleted() }
            try {
                setDataSource(playFileName); prepare(); start()
                seekTo(getTime(this)); setSeekBarTimer()
            } catch (e: IOException) {
                Log.e(ChatFragment.TAG, "prepare() failed")
            }
        }
    }

    private fun MediaPlayer.onCompleted() {
        audioTimeTextView?.let { Utility.setAudioTimeMmSs(it, duration.toLong()) }
        stopPlaying()
        playButton?.isActivated = false
        seekBar?.progress = 0
    }

    private fun getTime(mediaPlayer: MediaPlayer): Int {
        val seekBarProgress = seekBar?.progress ?: return 0
        return mediaPlayer.duration * seekBarProgress / 100
    }

    fun stopPlaying() {
        stopTimer()
        try {
            player?.apply { stop(); release() }
            player = null
        } catch (stopException: RuntimeException) {
            Log.e(ChatFragment.TAG, "stop() failed")
        }
    }

    fun stopTimer() {
        timer?.cancel(); timer = null
    }

    fun startRecording() {
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(getRecordFilePath())
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            try {
                prepare(); start()
                recorderDuration = System.currentTimeMillis()
            } catch (e: IOException) {
                Log.e(ChatFragment.TAG, "prepare() failed")
            } catch (e: IllegalStateException) {
                Log.e(ChatFragment.TAG, "prepare() failed")
                Utility.makeText(activity, R.string.record_failed)
            }
        }
    }

    private fun getRecordFilePath(): String? {
        return fileHelper.createAudioMediaFile()?.absolutePath.apply { recordFileName = this }
    }

    fun stopRecording() {
        recorderDuration?.let { recorderDuration = System.currentTimeMillis().minus(it) }
        try {
            recorder?.apply { stop(); release() }
            recorder = null
        } catch (stopException: RuntimeException) {
            Log.e(ChatFragment.TAG, "stop() failed")
        }
    }

    fun onStop() {
        stopPlaying()
        stopRecording()
    }
}