package com.example.roomshelves3d

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.roomshelves3d.databinding.ActivityMainBinding
import com.example.roomshelves3d.ui.RoomGLSurfaceView

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val glView = binding.glView as RoomGLSurfaceView
        binding.btnAdd.setOnClickListener { glView.setMode(RoomGLSurfaceView.Mode.ADD) }
        binding.btnMove.setOnClickListener { glView.setMode(RoomGLSurfaceView.Mode.MOVE) }
        binding.btnPan.setOnClickListener { glView.setMode(RoomGLSurfaceView.Mode.PAN) }
        binding.btnReset.setOnClickListener { glView.resetScene() }
        binding.btnSave.setOnClickListener { glView.saveLayout() }
        binding.btnLoad.setOnClickListener { glView.loadLayout() }
        binding.btnShelfUp.setOnClickListener { glView.adjustShelfSpacing(+2f) }
        binding.btnShelfDown.setOnClickListener { glView.adjustShelfSpacing(-2f) }
    }
}