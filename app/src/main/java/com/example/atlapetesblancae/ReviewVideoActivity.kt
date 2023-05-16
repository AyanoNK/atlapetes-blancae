package com.example.atlapetesblancae

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import android.net.Uri
import com.example.atlapetesblancae.databinding.ActivityReviewVideoBinding
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.Tensor
import org.pytorch.torchvision.TensorImageUtils
import java.io.FileOutputStream
import kotlin.math.ceil

class ReviewVideoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReviewVideoBinding
    private var mModule: Module? = null
    private var progressStatus: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityReviewVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val videoUri: String? = intent.getStringExtra("videoUri")
        println(videoUri)

        if (videoUri != null) {
            startVideoPreview(videoUri)
        }
        Thread(Runnable {
            test()
        }).start()
    }

    private fun test() {
        val stored1080pVideo = resources.openRawResource(R.raw.test2)
        // model expects video to be loaded
        val mModule = LiteModuleLoader.load(this.assetFilePath(this, "model.ptl"))
        val retriever = MediaMetadataRetriever()

        // prepare InputStream to be used in retriever.setDataSource
        val tempFile = File.createTempFile("temp", "mp4")
        tempFile.deleteOnExit()

        stored1080pVideo.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        retriever.setDataSource(tempFile.absolutePath)

        val videoLength =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
        val frameRate = 1000.toLong()
        // create empty decimal array of variable size to store max values

        val predictions = mutableListOf<Float>()
        val frameNumber = videoLength / frameRate
        binding.analyzingProgressBar.max = ceil(frameNumber.toDouble()).toInt()

        for (i in 0 until videoLength step frameRate) {
            println(i)
            val bitmap = retriever.getFrameAtTime(i, MediaMetadataRetriever.OPTION_CLOSEST)
            val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
                bitmap,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
                TensorImageUtils.TORCHVISION_NORM_STD_RGB
            )
            val outputTensor = mModule.forward(IValue.from(inputTensor)).toTuple()
            for (tensor in outputTensor) {
                val prediction = getMostAccuratePrediction(tensor.toTensor())
                if (prediction != null) {
                    predictions.add(prediction)
                    progressStatus += 1
                    binding.analyzingProgressBar.progress = progressStatus
                }
            }
        }

        for (i in predictions.indices) {
            println("Prediction for frame $i: ${predictions[i]}")
        }
    }

    private fun assetFilePath(context: Context, assetName: String?): String? {
        val file = File(context.filesDir, assetName)
        if (file.exists() && file.length() > 0) {
            return file.absolutePath
        }
        context.assets.open(assetName!!).use { `is` ->
            FileOutputStream(file).use { os ->
                val buffer = ByteArray(4 * 1024)
                var read: Int
                while (`is`.read(buffer).also { read = it } != -1) {
                    os.write(buffer, 0, read)
                }
                os.flush()
            }
            return file.absolutePath
        }
    }

    private fun getMostAccuratePrediction(tensor: Tensor): Float? {
        val tensorData = tensor.dataAsFloatArray
        return tensorData.maxOrNull()
    }

    private fun startVideoPreview(videoUri: String) {
        println("I SHOULD NOT ARRIVE HERE")
        binding.recordedVideoView.setVideoURI(Uri.parse(videoUri))
        binding.recordedVideoView.start()
        // loop video
        binding.recordedVideoView.setOnCompletionListener {
            binding.recordedVideoView.start()
        }
    }
}