package com.example.roomshelves3d.visual

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.view.MotionEvent
import com.example.roomshelves3d.ui.RoomGLSurfaceView
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class RoomRenderer(private val ctx: Context) : GLSurfaceView.Renderer {

    // Units in inches
    private val ROOM_W = 168f   // 14 ft (West-East)
    private val ROOM_L = 288f   // 24 ft (North-South)
    private val ROOM_H = 144f   // 12 ft height

    // Shelves
    private val SHELF_DEPTH = 36f
    private var shelfLevelHeight = 32f
    private val SHELF_LEVELS = 4

    // Can: 35" circumference => diameter
    private val CAN_DIAM = (35f / PI.toFloat())
    private val CAN_RADIUS = CAN_DIAM / 2f
    private val CAN_HEIGHT = 14f

    // Camera
    private var camDistance = 520f
    private var yaw = 35f
    private var pitch = 20f

    // Pan offset
    private var panX = 0f
    private var panZ = 0f

    private val view = FloatArray(16)
    private val proj = FloatArray(16)
    private val vp = FloatArray(16)

    private lateinit var roomMesh: Box
    private val shelves = mutableListOf<Box>()
    private val labels = mutableListOf<TextBillboard>()
    private val cans = mutableListOf<Cylinder>()

    private var width = 0
    private var height = 0

    private var mode = RoomGLSurfaceView.Mode.VIEW

    private var selectedCan: Cylinder? = null

    fun setMode(m: RoomGLSurfaceView.Mode) { mode = m; selectedCan = null }

    fun reset() { cans.clear() }

    override fun onSurfaceCreated(unused: javax.microedition.khronos.opengles.GL10?, config: javax.microedition.khronos.egl.EGLConfig?) {
        GLES20.glClearColor(0.15f, 0.22f, 0.65f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        buildScene()
    }

    override fun onSurfaceChanged(unused: javax.microedition.khronos.opengles.GL10?, w: Int, h: Int) {
        width = w; height = h
        GLES20.glViewport(0, 0, w, h)
        val aspect = w.toFloat() / h
        Matrix.perspectiveM(proj, 0, 45f, aspect, 1f, 4000f)
    }

    override fun onDrawFrame(unused: javax.microedition.khronos.opengles.GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        val eye = floatArrayOf(
            camDistance * cos(toRad(yaw)) * cos(toRad(pitch)),
            camDistance * sin(toRad(pitch)),
            camDistance * sin(toRad(yaw)) * cos(toRad(pitch))
        )
        val center = floatArrayOf(ROOM_W/2f, ROOM_H/2f, ROOM_L/2f)
        Matrix.setLookAtM(view, 0,
            eye[0] + panX, eye[1], eye[2] + panZ,
            center[0] + panX, center[1], center[2] + panZ,
            0f,1f,0f)
        Matrix.multiplyMM(vp, 0, proj, 0, view, 0)

        roomMesh.draw(vp)
        shelves.forEach { it.draw(vp) }

        // physics drop animation
        val g = 980f
        val dt = 1f/60f
        cans.forEach { c ->
            c.targetY?.let { ty ->
                if (kotlin.math.abs((c.position[1] - ty)) > 0.5f) {
                    c.vy += g * dt
                    c.position[1] = kotlin.math.min(c.position[1] + c.vy*dt, ty)
                } else {
                    c.position[1] = ty
                    c.vy = 0f
                    c.targetY = null
                }
            }
        }
        cans.forEach { it.draw(vp) }
        labels.forEach { it.draw(vp) }
    }

    private fun buildScene() {
        roomMesh = Box(ROOM_W, ROOM_H, ROOM_L, color = floatArrayOf(0.2f, 0.4f, 0.9f, 1f), inward = true)

        shelves.clear(); labels.clear()

        val shelfColor = floatArrayOf(0.75f, 0.75f, 0.8f, 1f)
        repeat(SHELF_LEVELS) { level ->
            val yBase = (level * shelfLevelHeight) + CAN_HEIGHT/2f
            val ySize = 2f

            // North wall
            shelves += Box(ROOM_W, ySize, SHELF_DEPTH,
                position = floatArrayOf(ROOM_W/2f, yBase, SHELF_DEPTH/2f), color = shelfColor)
            // South wall
            shelves += Box(ROOM_W, ySize, SHELF_DEPTH,
                position = floatArrayOf(ROOM_W/2f, yBase, ROOM_L - SHELF_DEPTH/2f), color = shelfColor)
            // West wall
            shelves += Box(SHELF_DEPTH, ySize, ROOM_L,
                position = floatArrayOf(SHELF_DEPTH/2f, yBase, ROOM_L/2f), color = shelfColor)
            // East wall
            shelves += Box(SHELF_DEPTH, ySize, ROOM_L,
                position = floatArrayOf(ROOM_W - SHELF_DEPTH/2f, yBase, ROOM_L/2f), color = shelfColor)
        }

        labels += makeLabel("North", ROOM_W/2f, ROOM_H - 10f, 10f)
        labels += makeLabel("South", ROOM_W/2f, ROOM_H - 10f, ROOM_L - 10f)
        labels += makeLabel("West", 10f, ROOM_H - 10f, ROOM_L/2f).apply { rotationY = 90f }
        labels += makeLabel("East", ROOM_W - 10f, ROOM_H - 10f, ROOM_L/2f).apply { rotationY = -90f }
    }

    private fun makeLabel(text: String, x: Float, y: Float, z: Float): TextBillboard {
        val bmp = textBitmap(text); return TextBillboard(bmp, floatArrayOf(x, y, z))
    }
    private fun textBitmap(t: String): Bitmap {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 48f; textAlign = Paint.Align.LEFT }
        val w = (p.measureText(t) + 40).toInt(); val h = 80
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp); c.drawColor(Color.argb(64,0,0,0)); p.color = Color.WHITE; c.drawText(t, 20f, 55f, p)
        return bmp
    }

    fun orbit(dx: Float, dy: Float) {
        // Orbit camera around center
        yaw -= dx * 0.2f
        pitch = (pitch - dy * 0.2f).coerceIn(-80f, 80f)
    }
    fun zoom(scale: Float) { camDistance = (camDistance / scale).coerceIn(200f, 1500f) }
    fun pan(dx: Float, dy: Float) {
        val s = camDistance / 300f
        panX += -dx * 0.5f * s
        panZ += dy * 0.5f * s
    }

    fun onTwoFinger(event: MotionEvent) { /* optional */ }
    fun onSingleTouch(event: MotionEvent) { /* handled in gestures */ }

    fun onTap(x: Float, y: Float) {
        when (mode) {
            RoomGLSurfaceView.Mode.ADD -> placeCanAtScreen(x, y)
            RoomGLSurfaceView.Mode.MOVE -> if (selectedCan != null) dropSelectedAt(x, y) else pickAt(x, y)
            else -> {}
        }
    }

    fun pickAt(sx: Float, sy: Float) {
        val ray = screenToRay(sx, sy)
        var best: Cylinder? = null; var bestDist = 1e9f
        cans.forEach {
            val d = distanceRayPoint(ray, it.position)
            if (d < CAN_RADIUS && d < bestDist) { best = it; bestDist = d }
        }
        selectedCan = best
    }
    fun dragSelected(dx: Float, dy: Float) { /* visual handled on drop */ }
    fun dropSelectedAt(sx: Float, sy: Float) {
        selectedCan?.let { can ->
            val (hit, pos) = raycastShelves(screenToRay(sx, sy))
            if (hit) {
                val snapped = snapToShelfGrid(pos)
                can.targetY = snapped[1]
                can.position[0] = snapped[0]
                can.position[2] = snapped[2]
            }
        }
    }

    private data class Ray(val origin: FloatArray, val dir: FloatArray)

    private fun screenToRay(sx: Float, sy: Float): Ray {
        val nx = (2f * sx / width) - 1f
        val ny = 1f - (2f * sy / height)
        val invVP = FloatArray(16); Matrix.invertM(invVP, 0, vp, 0)
        val near = floatArrayOf(nx, ny, -1f, 1f); val far = floatArrayOf(nx, ny, 1f, 1f)
        val nearW = FloatArray(4); val farW = FloatArray(4)
        Matrix.multiplyMV(nearW, 0, invVP, 0, near, 0); Matrix.multiplyMV(farW, 0, invVP, 0, far, 0)
        for (i in 0..3) { nearW[i] /= nearW[3]; farW[i] /= farW[3] }
        val dir = floatArrayOf(farW[0]-nearW[0], farW[1]-nearW[1], farW[2]-nearW[2])
        val len = sqrt(dir[0]*dir[0]+dir[1]*dir[1]+dir[2]*dir[2]); for (i in 0..2) dir[i] /= len
        return Ray(floatArrayOf(nearW[0],nearW[1],nearW[2]), dir)
    }

    private fun raycastShelves(ray: Ray): Pair<Boolean, FloatArray> {
        var bestT = Float.MAX_VALUE
        var bestPoint = floatArrayOf(0f,0f,0f); var hit = false
        shelves.forEach { shelf ->
            val y = shelf.position[1]
            val t = (y - ray.origin[1]) / ray.dir[1]
            if (t > 0) {
                val p = floatArrayOf(ray.origin[0] + ray.dir[0]*t, y, ray.origin[2] + ray.dir[2]*t)
                if (shelf.containsOnTop(p)) {
                    if (t < bestT) { bestT = t; bestPoint = p; hit = true }
                }
            }
        }
        return hit to bestPoint
    }

    private fun snapToShelfGrid(p: FloatArray): FloatArray {
        val shelf = shelves.minBy { it.distanceToTopPoint(p) }
        val step = CAN_DIAM * 1.1f
        val lx = (p[0] - (shelf.position[0] - shelf.size[0]/2f))
        val lz = (p[2] - (shelf.position[2] - shelf.size[2]/2f))
        var gx = (floor(lx / step) + 0.5f) * step
        var gz = (floor(lz / step) + 0.5f) * step
        val margin = CAN_RADIUS * 1.05f
        gx = gx.coerceIn(margin, shelf.size[0]-margin)
        gz = gz.coerceIn(margin, shelf.size[2]-margin)
        val wx = (shelf.position[0] - shelf.size[0]/2f) + gx
        val wz = (shelf.position[2] - shelf.size[2]/2f) + gz
        val baseY = shelf.position[1] + CAN_HEIGHT/2f
        val stepTol = step*0.25f
        val currentStack = cans.count { abs(it.position[0]-wx) < stepTol && abs(it.position[2]-wz) < stepTol && it.position[1] >= baseY-1f }
        val wy = baseY + currentStack * CAN_HEIGHT
        return floatArrayOf(wx, wy, wz)
    }

    private fun distanceRayPoint(ray: Ray, p: FloatArray): Float {
        val v = floatArrayOf(p[0]-ray.origin[0], p[1]-ray.origin[1], p[2]-ray.origin[2])
        val t = max(0f, v[0]*ray.dir[0] + v[1]*ray.dir[1] + v[2]*ray.dir[2])
        val proj = floatArrayOf(ray.origin[0]+ray.dir[0]*t, ray.origin[1]+ray.dir[1]*t, ray.origin[2]+ray.dir[2]*t)
        val dx = p[0]-proj[0]; val dy = p[1]-proj[1]; val dz = p[2]-proj[2]
        return sqrt(dx*dx+dy*dy+dz*dz)
    }

    private fun toRad(a: Float) = (a / 180f) * Math.PI.toFloat()

    fun adjustShelfSpacing(delta: Float) {
        shelfLevelHeight = (shelfLevelHeight + delta).coerceIn(20f, 50f)
        buildScene()
        cans.forEach { can ->
            val snapped = snapToShelfGrid(can.position)
            can.position[0] = snapped[0]
            can.position[1] = snapped[1]
            can.position[2] = snapped[2]
        }
    }

    fun saveLayout() {
        val prefs = ctx.getSharedPreferences("layout", Context.MODE_PRIVATE)
        val arr = cans.joinToString(prefix = "[", postfix = "]") {
            "{"x":${it.position[0]},"y":${it.position[1]},"z":${it.position[2]}}"
        }
        prefs.edit().putString("cans", arr).apply()
    }
    fun loadLayout() {
        val prefs = ctx.getSharedPreferences("layout", Context.MODE_PRIVATE)
        val json = prefs.getString("cans", null) ?: return
        cans.clear()
        val body = json.trim().strip("[]")
        if (body.isNotEmpty()) {
            val parts = [p + ("}" if not p.endswith("}") else "") for p in map(str.strip, body.split("},"))]  # placeholder comment
        }
        // Simple manual parse (avoid JSON lib to keep it minimal)
        val objs = json.trim().removePrefix("[").removeSuffix("]").split("},").map { it.trim().trimEnd('}') + "}" }.filter { it.length > 2 }
        objs.forEach { s ->
            fun ext(key: String): Float {
                val i = s.indexOf(""$key":")
                val sub = s.substring(i + key.length + 3)
                val end = listOf(',', '}').map { ch -> sub.indexOf(ch).let { if (it==-1) sub.length else it } }.minOrNull() ?: sub.length
                return sub.substring(0, end).toFloat()
            }
            val c = Cylinder(CAN_RADIUS, CAN_HEIGHT)
            c.position[0] = ext("x")
            c.position[1] = ext("y")
            c.position[2] = ext("z")
            cans += c
        }
    }
}