package com.artf.chatapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.artf.chatapp.databinding.DialogPhotoBinding
import com.artf.chatapp.utils.bindImage
import kotlin.math.max
import kotlin.math.min

class PhotoDialog() : DialogFragment() {
    constructor(photoUrl: String) : this() {
        this.photoUrl = photoUrl
    }

    companion object {
        const val DRAWABLE_KEY = "DrawablePhoto"
    }

    lateinit var photoUrl: String
    lateinit var binding: DialogPhotoBinding

    private var mScaleGestureDetector: ScaleGestureDetector? = null
    private var mScaleFactor = 1.0f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogPhotoBinding.inflate(LayoutInflater.from(activity))
        if (savedInstanceState != null && savedInstanceState.containsKey(DRAWABLE_KEY)) {
            photoUrl = savedInstanceState.getString(DRAWABLE_KEY)!!
        }
        bindImage(binding.imageView, photoUrl)
        mScaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
        dialog!!.window?.decorView?.setOnTouchListener { view, motionEvent ->
            mScaleGestureDetector!!.onTouchEvent(motionEvent)
            true
        }
        // Utility.onCreateDialog(activity!!, dialog!!, binding.root, 400, 400)
        return binding.root
    }

    override fun getTheme(): Int {
        return android.R.style.Theme_Black_NoTitleBar_Fullscreen
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(DRAWABLE_KEY, photoUrl)
        super.onSaveInstanceState(outState)
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(scaleGestureDetector: ScaleGestureDetector): Boolean {
            mScaleFactor *= scaleGestureDetector.scaleFactor
            mScaleFactor = max(0.1f, min(mScaleFactor, 10.0f))
            binding.imageView.scaleX = mScaleFactor
            binding.imageView.scaleY = mScaleFactor
            return true
        }
    }
}