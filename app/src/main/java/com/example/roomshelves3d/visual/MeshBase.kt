package com.example.roomshelves3d.visual

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix

open class MeshBase(
    private val vertices: FloatArray,
    private val normals: FloatArray,
    private val uvs: FloatArray? = null,
    private val indices: ShortArray,
    private val color: FloatArray = floatArrayOf(1f,1f,1f,1f),
    private val texture: Bitmap? = null
) {
    private val shader = SimpleShader()
    private val model = FloatArray(16)
    private val mvp = FloatArray(16)

    var position = floatArrayOf(0f,0f,0f)
    var rotationY = 0f
    private var texId = 0

    init {
        shader.create()
        Matrix.setIdentityM(model, 0)
        if (texture != null) {
            val ids = IntArray(1)
            GLES20.glGenTextures(1, ids, 0)
            texId = ids[0]
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, texture, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        }
    }

    fun draw(vp: FloatArray) {
        shader.use()
        Matrix.setIdentityM(model, 0)
        Matrix.translateM(model, 0, position[0], position[1], position[2])
        Matrix.rotateM(model, 0, rotationY, 0f,1f,0f)
        Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)

        val aPos = shader.getAttrib("aPos")
        val aNormal = shader.getAttrib("aNormal")
        val aUV = shader.getAttrib("aUV")
        val uMVP = shader.getUniform("uMVP")
        val uM = shader.getUniform("uM")
        val uColor = shader.getUniform("uColor")
        val uUseTex = shader.getUniform("uUseTex")
        val uTex = shader.getUniform("uTex")

        GLES20.glUniformMatrix4fv(uMVP, 1, false, mvp, 0)
        GLES20.glUniformMatrix4fv(uM, 1, false, model, 0)
        GLES20.glUniform4fv(uColor, 1, color, 0)

        GLES20.glEnableVertexAttribArray(aPos)
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 0, floatBuffer(vertices))

        GLES20.glEnableVertexAttribArray(aNormal)
        GLES20.glVertexAttribPointer(aNormal, 3, GLES20.GL_FLOAT, false, 0, floatBuffer(normals))

        if (uvs != null && texId != 0) {
            GLES20.glUniform1i(uUseTex, 1)
            GLES20.glEnableVertexAttribArray(aUV)
            GLES20.glVertexAttribPointer(aUV, 2, GLES20.GL_FLOAT, false, 0, floatBuffer(uvs))
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId)
            GLES20.glUniform1i(uTex, 0)
        } else {
            GLES20.glUniform1i(uUseTex, 0)
        }

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.size, GLES20.GL_UNSIGNED_SHORT, shortBuffer(indices))

        GLES20.glDisableVertexAttribArray(aPos)
        GLES20.glDisableVertexAttribArray(aNormal)
        if (uvs != null) GLES20.glDisableVertexAttribArray(aUV)
    }
}