package com.example.roomshelves3d.visual

import android.opengl.GLES20

class SimpleShader {
    private var programId = 0

    private val vsh = """
        attribute vec3 aPos;
        attribute vec3 aNormal;
        attribute vec2 aUV;
        uniform mat4 uMVP;
        uniform mat4 uM;
        varying vec3 vNormal;
        varying vec3 vPos;
        varying vec2 vUV;
        void main(){
            gl_Position = uMVP * vec4(aPos,1.0);
            vPos = (uM * vec4(aPos,1.0)).xyz;
            vNormal = mat3(uM) * aNormal;
            vUV = aUV;
        }
    """

    private val fsh = """
        precision mediump float;
        varying vec3 vNormal;
        varying vec3 vPos;
        varying vec2 vUV;
        uniform vec4 uColor;
        uniform sampler2D uTex;
        uniform bool uUseTex;
        void main(){
            vec3 L = normalize(vec3(0.4, 1.0, 0.6));
            vec3 N = normalize(vNormal);
            float diff = max(dot(N,L), 0.1);
            vec4 base = uUseTex ? texture2D(uTex, vUV) : uColor;
            gl_FragColor = vec4(base.rgb * (0.2 + 0.8*diff), base.a);
        }
    """

    fun create() {
        val vs = compile(GLES20.GL_VERTEX_SHADER, vsh)
        val fs = compile(GLES20.GL_FRAGMENT_SHADER, fsh)
        programId = GLES20.glCreateProgram()
        GLES20.glAttachShader(programId, vs)
        GLES20.glAttachShader(programId, fs)
        GLES20.glLinkProgram(programId)
    }

    private fun compile(type: Int, src: String): Int {
        val id = GLES20.glCreateShader(type)
        GLES20.glShaderSource(id, src)
        GLES20.glCompileShader(id)
        return id
    }

    fun use() { GLES20.glUseProgram(programId) }
    fun getUniform(name: String) = GLES20.glGetUniformLocation(programId, name)
    fun getAttrib(name: String) = GLES20.glGetAttribLocation(programId, name)
}