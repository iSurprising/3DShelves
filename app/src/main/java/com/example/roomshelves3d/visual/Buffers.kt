package com.example.roomshelves3d.visual

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

fun floatBuffer(arr: FloatArray): FloatBuffer {
    val bb = ByteBuffer.allocateDirect(arr.size * 4)
    bb.order(ByteOrder.nativeOrder())
    val fb = bb.asFloatBuffer()
    fb.put(arr)
    fb.position(0)
    return fb
}

fun shortBuffer(arr: ShortArray): ShortBuffer {
    val bb = ByteBuffer.allocateDirect(arr.size * 2)
    bb.order(ByteOrder.nativeOrder())
    val sb = bb.asShortBuffer()
    sb.put(arr)
    sb.position(0)
    return sb
}