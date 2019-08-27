package com.artf.chatapp.view.chatRoom

import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.artf.chatapp.databinding.DialogPhotoBinding
import com.artf.chatapp.utils.bindImage
import kotlin.math.atan2
import kotlin.math.sqrt

class PhotoDialog() : DialogFragment() {
    constructor(photoUrl: String) : this() {
        this.photoUrl = photoUrl
    }

    companion object {
        const val DRAWABLE_KEY = "DrawablePhoto"
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
    }

    lateinit var photoUrl: String
    lateinit var binding: DialogPhotoBinding

    private var mode = NONE

    private val matrix = Matrix()
    private val savedMatrix = Matrix()

    private val mid = PointF()
    private val start = PointF()
    private var d = 0f
    private var newRot = 0f
    private var lastEvent: FloatArray? = null
    private var oldDist = 1f

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

        dialog!!.window?.decorView?.setOnTouchListener { _, motionEvent ->
            binding.imageView.bringToFront()
            viewTransformation(binding.imageView, motionEvent)
            true
        }
        return binding.root
    }

    override fun getTheme(): Int {
        return android.R.style.Theme_Black_NoTitleBar_Fullscreen
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(DRAWABLE_KEY, photoUrl)
        super.onSaveInstanceState(outState)
    }

    private fun viewTransformation(view: ImageView, event: MotionEvent) {
        if (view.scaleType == ImageView.ScaleType.FIT_XY) swapFixXyToMatrix(view, matrix)
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                savedMatrix.set(matrix)
                start.set(event.x, event.y)
                mode = DRAG
                lastEvent = null
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                oldDist = spacing(event)
                if (oldDist > 10f) {
                    savedMatrix.set(matrix)
                    midPoint(mid, event)
                    mode = ZOOM
                }
                lastEvent = FloatArray(4).also {
                    it[0] = event.getX(0)
                    it[1] = event.getX(1)
                    it[2] = event.getY(0)
                    it[3] = event.getY(1)
                }
                d = rotation(event)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                mode = NONE
                lastEvent = null
            }
            MotionEvent.ACTION_MOVE -> if (mode == DRAG) {
                // ...
                matrix.set(savedMatrix)
                matrix.postTranslate(event.x - start.x, event.y - start.y)
            } else if (mode == ZOOM && event.pointerCount == 2) {
                val newDist = spacing(event)
                matrix.set(savedMatrix)
                if (newDist > 10f) {
                    val scale = newDist / oldDist
                    matrix.postScale(scale, scale, mid.x, mid.y)
                }
                if (lastEvent != null) {
                    newRot = rotation(event)
                    val r = (newRot - d)
                    matrix.postRotate(
                        r, (view.measuredWidth / 2).toFloat(), (view.measuredHeight / 2).toFloat()
                    )
                }
            }
        }
        view.imageMatrix = matrix
    }

    private fun swapFixXyToMatrix(image: ImageView, matrix: Matrix) {
        val imageWidth = image.drawable.intrinsicWidth.toFloat()
        val imageHeight = image.drawable.intrinsicHeight.toFloat()
        val drawableRect = RectF(0f, 0f, imageWidth, imageHeight)
        val viewRect = RectF(0f, 0f, image.width.toFloat(), image.height.toFloat())
        val newMatrix = Matrix()
        newMatrix.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.CENTER)
        image.scaleType = ImageView.ScaleType.MATRIX
        matrix.set(newMatrix)
    }

    private fun rotation(event: MotionEvent): Float {
        val deltaX = (event.getX(0) - event.getX(1)).toDouble()
        val deltaY = (event.getY(0) - event.getY(1)).toDouble()
        val radians = atan2(deltaY, deltaX)
        return Math.toDegrees(radians).toFloat()
    }

    private fun spacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return sqrt(x * x + y * y)
    }

    private fun midPoint(point: PointF, event: MotionEvent) {
        val x = event.getX(0) + event.getX(1)
        val y = event.getY(0) + event.getY(1)
        point.set(x / 2, y / 2)
    }
}