package com.example.atlapetesblancae

import android.Manifest
import android.content.ContentValues
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.atlapetesblancae.databinding.ActivityMainBinding
import android.content.pm.PackageManager
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
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.PermissionChecker
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

typealias LumaListener = (luma: Double) -> Unit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null

    private var recording: Recording? = null


    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
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

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
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
                    this@MainActivity, Manifest.permission.RECORD_AUDIO
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

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(
            outputDirectory, SimpleDateFormat(
                Constants.FILE_NAME_FORMAT, Locale.getDefault()
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOption = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(outputOption,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo saved"
                    Toast.makeText(this@MainActivity, "$msg $savedUri", Toast.LENGTH_LONG).show()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(Constants.TAG, "onError: ${exception.message}", exception)
                }

            })

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
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, videoCapture
                )

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
//
    }

    private fun allPermissionsGranted() = Constants.REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun loadTorchModule(fileName: String) {
        val path = this.filesDir.absolutePath
        println(path.toString())
        // TODO: add storage permission
    }

    // TODO: analyze this function
    // https://github.com/pytorch/android-demo-app/blob/8e2700a96dd4126f7aaae0f62d52d44bac9ed722/QuestionAnswering/app/src/main/java/org/pytorch/demo/questionanswering/MainActivity.kt#L4
//    private fun answer(question: String, text: String): String? {
//        if (mModule == null) {
//            mModule = LiteModuleLoader.load(this.assetFilePath(this, "qa360_quantized.ptl"))
//        }
//
//        try {
//            val tokenIds = tokenizer(question, text)
//            val inTensorBuffer = Tensor.allocateLongBuffer(MODEL_INPUT_LENGTH)
//            for (n in tokenIds) inTensorBuffer.put(n.toLong())
//            for (i in 0 until MODEL_INPUT_LENGTH - tokenIds.size) mTokenIdMap!![PAD]?.let { inTensorBuffer.put(it) }
//
//            val inTensor = Tensor.fromBlob(inTensorBuffer, longArrayOf(1, MODEL_INPUT_LENGTH.toLong()))
//            val outTensors = mModule!!.forward(IValue.from(inTensor)).toDictStringKey()
//            val startTensor = outTensors[START_LOGITS]!!.toTensor()
//            val endTensor = outTensors[END_LOGITS]!!.toTensor()
//
//            val starts = startTensor.dataAsFloatArray
//            val ends = endTensor.dataAsFloatArray
//            val answerTokens: MutableList<String?> = ArrayList()
//            val start = argmax(starts)
//            val end = argmax(ends)
//            for (i in start until end + 1) answerTokens.add(mIdTokenMap!![tokenIds[i]])
//
//            return java.lang.String.join(" ", answerTokens).replace(" ##".toRegex(), "").replace("\\s+(?=\\p{Punct})".toRegex(), "")
//        } catch (e: QAException) {
//            runOnUiThread { mTextViewAnswer!!.text = e.message }
//        }
//        return null
//    }

    // TODO: analyze this function
//    private fun argmax(array: FloatArray): Int {
//        var maxIdx = 0
//        var maxVal: Double = -MAX_VALUE
//        for (j in array.indices) {
//            if (array[j] > maxVal) {
//                maxVal = array[j].toDouble()
//                maxIdx = j
//            }
//        }
//        return maxIdx
//    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }


}