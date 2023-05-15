package com.example.atlapetesblancae

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.atlapetesblancae.databinding.ActivityMainBinding
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.icu.text.AlphabeticIndex.Record
import androidx.core.content.ContextCompat
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.ImageCapture
import android.util.Log
import androidx.camera.core.ImageCaptureException
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.ScaleGestureDetector
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.PermissionChecker
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.Tensor
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // request camera permissions
        if (allPermissionsGranted()) {
            Toast.makeText(
                this, "Permisos permitidos.", Toast.LENGTH_SHORT
            ).show()
        } else ActivityCompat.requestPermissions(
            this, Constants.REQUIRED_PERMISSIONS, Constants.REQUEST_CODE_PERMISSIONS
        )

        if (!allPermissionsGranted()) {
            binding.btnTakeRecording.apply {
                isEnabled = false
                text = "Revisa los permisos de la cámara y micrófono"
            }
        }

        binding.btnTakeRecording.setOnClickListener {
            val intent = Intent(this, RecordVideoActivity::class.java)
            startActivity(intent)
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (requestCode == Constants.REQUEST_CODE_PERMISSIONS) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        } else {
            Toast.makeText(
                this, "Permisos no permitidos. Revise sus ajustes.", Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    private fun allPermissionsGranted() = Constants.REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }
}