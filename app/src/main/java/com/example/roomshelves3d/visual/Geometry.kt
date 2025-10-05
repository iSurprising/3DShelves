package com.example.roomshelves3d.visual

import android.graphics.Bitmap
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Box(
    val w: Float, val h: Float, val d: Float,
    val position: FloatArray = floatArrayOf(0f,0f,0f),
    color: FloatArray = floatArrayOf(1f,1f,1f,1f),
    inward: Boolean = false
) : MeshBase(
    vertices = run {
        val x = w/2f; val y = h/2f; val z = d/2f
        floatArrayOf(
            -x,-y,-z,  x,-y,-z,  x, y,-z, -x, y,-z,
            -x,-y, z,  x,-y, z,  x, y, z, -x, y, z
        )
    },
    normals = run {
        val s = if (inward) -1f else 1f
        floatArrayOf(
            0f,0f,-1f, 0f,0f,-1f, 0f,0f,-1f, 0f,0f,-1f,
            0f,0f, 1f, 0f,0f, 1f, 0f,0f, 1f, 0f,0f, 1f
        ).map { it * s }.toFloatArray()
    },
    uvs = floatArrayOf(
        0f,0f, 1f,0f, 1f,1f, 0f,1f,
        0f,0f, 1f,0f, 1f,1f, 0f,1f
    ),
    indices = shortArrayOf(
        0,1,2, 0,2,3,  4,5,6, 4,6,7,
        0,1,5, 0,5,4, 1,2,6, 1,6,5, 2,3,7, 2,7,6, 3,0,4, 3,4,7
    ),
    color = color
) {
    val size = floatArrayOf(w,h,d)
    init { this.position[0]=position[0]; this.position[1]=position[1]; this.position[2]=position[2] }

    fun containsOnTop(p: FloatArray): Boolean {
        val minX = position[0] - w/2f
        val maxX = position[0] + w/2f
        val minZ = position[2] - d/2f
        val maxZ = position[2] + d/2f
        val onY = kotlin.math.abs(p[1] - position[1]) < 0.6f
        return (p[0] in minX..maxX && p[2] in minZ..maxZ && onY)
    }
    fun distanceToTopPoint(p: FloatArray): Float {
        val cx = p[0].coerceIn(position[0]-w/2f, position[0]+w/2f)
        val cz = p[2].coerceIn(position[2]-d/2f, position[2]+d/2f)
        val dx = p[0]-cx; val dz = p[2]-cz; val dy = p[1]-position[1]
        return sqrt(dx*dx+dy*dy+dz*dz)
    }
}

class Cylinder(
    private val r: Float,
    private val h: Float,
    color: FloatArray = floatArrayOf(0.9f,0.4f,0.4f,1f)
) : MeshBase(
    vertices = run {
        val seg = 36
        val verts = mutableListOf<Float>()
        for (i in 0..seg) {
            val a = (i / seg.toFloat()) * 2f * Math.PI.toFloat()
            val x = cos(a) * r
            val z = sin(a) * r
            verts += x; verts += -h/2f; verts += z
            verts += x; verts += h/2f; verts += z
        }
        verts.toFloatArray()
    },
    normals = run {
        val seg = 36
        val norms = mutableListOf<Float>()
        for (i in 0..seg) {
            val a = (i / seg.toFloat()) * 2f * Math.PI.toFloat()
            val x = cos(a); val z = sin(a)
            norms += x; norms += 0f; norms += z
            norms += x; norms += 0f; norms += z
        }
        norms.toFloatArray()
    },
    uvs = null,
    indices = run {
        val seg = 36
        val idx = mutableListOf<Short>()
        for (i in 0 until seg) {
            val a: Short = (i*2).toShort()
            val b: Short = (i*2+1).toShort()
            val c: Short = (i*2+2).toShort()
            val d: Short = (i*2+3).toShort()
            idx += a; idx += b; idx += c
            idx += b; idx += d; idx += c
        }
        idx.toShortArray()
    },
    color = color
) {
    var vy = 0f
    var targetY: Float? = null
    init { position = floatArrayOf(0f, h/2f, 0f) }
}

class TextBillboard(bmp: Bitmap, pos: FloatArray) : MeshBase(
    vertices = floatArrayOf(-20f,0f,0f,  20f,0f,0f,  20f,10f,0f, -20f,10f,0f),
    normals = floatArrayOf(0f,0f,1f, 0f,0f,1f, 0f,0f,1f, 0f,0f,1f),
    uvs = floatArrayOf(0f,1f, 1f,1f, 1f,0f, 0f,0f),
    indices = shortArrayOf(0,1,2, 0,2,3),
    color = floatArrayOf(1f,1f,1f,1f),
    texture = bmp
) { init { position = pos } }