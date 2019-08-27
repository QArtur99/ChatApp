package com.artf.chatapp.view.chatRoom

import android.media.MediaPlayer
import android.media.MediaRecorder
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.artf.chatapp.App
import com.artf.chatapp.R
import com.artf.chatapp.model.Message
import com.artf.chatapp.utils.Utility
import java.io.IOException
import java.util.Timer
import java.util.TimerTask

class AudioHelper(val activity: AppCompatActivity) {

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

    fun setSeekBarTimer() {
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (player == null) {
                    timer?.cancel()
                    return
                }
                activity.runOnUiThread {
                    if (player == null) return@runOnUiThread
                    val audioTimeTextView = audioTimeTextView ?: return@runOnUiThread
                    seekBar?.progress = player!!.currentPosition * 100 / player!!.duration
                    Utility.setAudioTimeMmSs(audioTimeTextView, player!!.currentPosition.toLong())
                }
            }
        }, 0, 20)
    }

    fun setAudioTime(seekBar: SeekBar, textView: TextView, duration: Long) {
        val time = duration * seekBar.progress / 100
        Utility.setAudioTimeMmSs(textView, time)
    }

    fun getTime(): Int {
        val totalDuration = player?.duration
        return totalDuration!! * seekBar?.progress!! / 100
    }

    fun startPlaying(getPosition: () -> Int) {
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
                Log.e(ChatFragment.TAG, "prepare() failed")
            }
        }
    }

    fun stopPlaying() {
        stopTimer()
        try {
            player?.stop()
            player?.release()
            player = null
        } catch (stopException: RuntimeException) {
            Log.e(ChatFragment.TAG, "stop() failed")
        }
    }

    fun stopTimer() {
        timer?.cancel()
        timer = null
    }

    fun startRecording() {
        recordFileName = Utility.createMediaFile(
            activity,
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
                Log.e(ChatFragment.TAG, "prepare() failed")
            } catch (e: IllegalStateException) {
                Log.e(ChatFragment.TAG, "prepare() failed")
                Toast.makeText(
                    activity,
                    activity.resources.getString(R.string.record_failed),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun stopRecording() {
        recorderDuration?.let { recorderDuration = System.currentTimeMillis().minus(it) }
        try {
            recorder?.stop()
            recorder?.release()
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