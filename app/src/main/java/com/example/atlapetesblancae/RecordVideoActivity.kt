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
import com.example.atlapetesblancae.databinding.ActivityRecordVideoBinding
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.Tensor
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class RecordVideoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecordVideoBinding
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRecordVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else ActivityCompat.requestPermissions(
            this, Constants.REQUIRED_PERMISSIONS, Constants.REQUEST_CODE_PERMISSIONS
        )
        binding.btnTakeRecording.setOnClickListener {
            captureVideo()
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun captureVideo() {
        val videoCapture = videoCapture ?: return

        binding.btnTakeRecording.isEnabled = false

        var curRecording = recording
        if (curRecording != null) {
            curRecording.stop()
            recording = null
            return
        }

        val name = SimpleDateFormat(
            Constants.FILE_NAME_FORMAT, Locale.US
        ).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(
                MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES
            ) // or Movies/CameraX-Video
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions.Builder(
            contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()

        recording = videoCapture.output.prepareRecording(
            this, mediaStoreOutputOptions,
        ).apply {
            if (PermissionChecker.checkSelfPermission(
                    this@RecordVideoActivity, Manifest.permission.RECORD_AUDIO
                ) == PermissionChecker.PERMISSION_GRANTED
            ) {
                withAudioEnabled()
            }
        }.start(ContextCompat.getMainExecutor(this)) { recordEvent ->
            when (recordEvent) {
                is VideoRecordEvent.Start -> {
                    binding.btnTakeRecording.apply {
                        text = "Parar"
                        isEnabled = true
                    }
                    // Schedule a stop recording task after 10 seconds
                    Handler(Looper.getMainLooper()).postDelayed({
                        curRecording?.stop()
                        curRecording = null
                    }, 10000L)
                }

                is VideoRecordEvent.Finalize -> {
                    if (!recordEvent.hasError()) {
                        val msg = "Video saved" + "${recordEvent.outputResults.outputUri}"
                        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                        Log.d(Constants.TAG, msg)
                        val intent = Intent(this, ReviewVideoActivity::class.java)
                        intent.putExtra("videoUri", recordEvent.outputResults.outputUri)
                        startActivity(intent)
                    } else {
                        recording?.close()
                        recording = null
                        Log.e(
                            Constants.TAG, "Video recording failed: " + "${recordEvent.error}"
                        )
                    }
                    binding.btnTakeRecording.apply {
                        text = getString(R.string.start_capture)
                        isEnabled = true
                    }
                }
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // preview
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }
            val recorder =
                Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HIGHEST)).build()
            videoCapture = VideoCapture.withOutput(recorder)
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, videoCapture
                )
                val cameraInfo = camera.cameraInfo
                val cameraControl = camera.cameraControl
                val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    override fun onScale(detector: ScaleGestureDetector): Boolean {
                        // get the camera's current zoom ratio
                        val currentZoomRatio = cameraInfo.zoomState.value?.zoomRatio ?: 0F
                        // get the pinch gesture's scaling factor
                        val delta = detector.scaleFactor
                        // update the camera's zoom ratio. This is an asynchronous operation that returns
                        // a ListenableFuture, allowing you to listen to when the operation completes.
                        cameraControl.setZoomRatio(currentZoomRatio * delta)
                        return true
                    }
                }
                val scaleGestureDetector = ScaleGestureDetector(this, listener)
                //attach pinch gesture listener to the viewfinder
                binding.viewFinder.setOnTouchListener { _, event ->
                    scaleGestureDetector.onTouchEvent(event)
                    return@setOnTouchListener true
                }
            } catch (e: Exception) {
                Log.d(Constants.TAG, "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (requestCode == Constants.REQUEST_CODE_PERMISSIONS) {
            startCamera()
            // TODO: check if this is an anti pattern
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

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }


}