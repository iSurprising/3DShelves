package com.example.roomshelves3d.ui

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.example.roomshelves3d.visual.RoomRenderer

class RoomGLSurfaceView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    enum class Mode { VIEW, ADD, MOVE, PAN }

    private val renderer: RoomRenderer
    private val gesture = GestureDetector(context, GestureListener())
    private val scaleGesture = ScaleGestureDetector(context, ScaleListener())

    private var currentMode = Mode.VIEW

    init {
        setEGLContextClientVersion(2)
        renderer = RoomRenderer(context)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun setMode(m: Mode) { currentMode = m; renderer.setMode(m) }

    fun resetScene() { renderer.reset() }

    fun saveLayout() { renderer.saveLayout() }
    fun loadLayout() { renderer.loadLayout() }

    fun adjustShelfSpacing(deltaInches: Float) { renderer.adjustShelfSpacing(deltaInches) }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGesture.onTouchEvent(event)
        if (!scaleGesture.isInProgress) gesture.onTouchEvent(event)
        when (event.pointerCount) {
            1 -> when (currentMode) {
                Mode.VIEW -> Unit // orbit handled in onScroll
                Mode.ADD, Mode.MOVE -> renderer.onSingleTouch(event)
                Mode.PAN -> Unit // panning handled in onScroll
            }
            2 -> renderer.onTwoFinger(event)
        }
        return true
    }

    inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, dx: Float, dy: Float): Boolean {
            when (currentMode) {
                Mode.VIEW -> renderer.orbit(dx, dy)
                Mode.MOVE -> renderer.dragSelected(dx, dy)
                Mode.PAN -> renderer.pan(dx, dy)
                else -> {}
            }
            return true
        }
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            renderer.onTap(e.x, e.y); return true
        }
        override fun onLongPress(e: MotionEvent) { if (currentMode == Mode.MOVE) renderer.pickAt(e.x, e.y) }
    }

    inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            renderer.zoom(detector.scaleFactor); return true
        }
    }
}