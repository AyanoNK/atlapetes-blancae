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
import com.example.atlapetesblancae.databinding.ActivityReviewVideoBinding
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.Tensor
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ReviewVideoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReviewVideoBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityReviewVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)


    }

    private fun loadRawResVideoCalledTest() {
        val video = this.resources.openRawResource(R.raw.test)
        println(video.javaClass.kotlin)
        val inTensorBuffer = Tensor.allocateLongBuffer(255)
        val mModule = LiteModuleLoader.load(this.assetFilePath("model.ptl"))
        val inTensor = Tensor.fromBlob(inTensorBuffer, longArrayOf(1, 255.toLong()))
    }

    private fun assetFilePath(asset: String): String {
        val file = File(baseContext.filesDir, asset)
        try {
            val inpStream: InputStream = baseContext.assets.open(asset)
            try {
                val outStream = FileOutputStream(file, false)
                val buffer = ByteArray(4 * 1024)
                var read: Int

                while (true) {
                    read = inpStream.read(buffer)
                    if (read == -1) {
                        break
                    }
                    outStream.write(buffer, 0, read)
                }
                outStream.flush()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            return file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
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
}