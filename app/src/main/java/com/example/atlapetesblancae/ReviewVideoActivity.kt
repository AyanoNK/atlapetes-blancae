package com.example.atlapetesblancae

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import android.net.Uri
import com.example.atlapetesblancae.databinding.ActivityReviewVideoBinding
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Tensor
import org.pytorch.torchvision.TensorImageUtils
import kotlin.math.ceil
import kotlin.math.roundToInt

class ReviewVideoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReviewVideoBinding
    private var progressStatus: Int = 0
    private val predictions = mutableListOf<Float?>()
    private val analyzeThread = Thread(Runnable {
        analyzeVideo()
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityReviewVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val videoUri: String? = intent.getStringExtra("videoUri")
        println(videoUri)

        if (videoUri != null) {
            startVideoPreview(videoUri)
        }
        if (videoUri == null) {
            finish()
        }
        analyzeThread.start()
    }

    override fun onBackPressed() {
        // disable back button
    }
    private fun analyzeVideo() {
        val modelModule = LiteModuleLoader.load(assetFilePath(this, "modell.ptl"))
        // load the video from the given videoUri and analyze it
        val uriVideo = Uri.parse(intent.getStringExtra("videoUri"))

        val retriever = MediaMetadataRetriever()
        val videoAbsoluteUri = retrieveVideo(uriVideo, this)
        retriever.setDataSource(this, videoAbsoluteUri)

        // prepare InputStream to be used in retriever.setDataSource
        val tempFile = File.createTempFile("temp", "mp4")
        tempFile.deleteOnExit()
        val videoLength =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
        val frameRate = 1000.toLong()

        val frameNumber = videoLength / frameRate
        binding.analyzingProgressBar.max = ceil(frameNumber.toDouble()).toInt()

        for (i in 0 until videoLength * 1000 step frameRate * 1000) {
            println(i)
            val bitmap = retriever.getFrameAtTime(i, MediaMetadataRetriever.OPTION_CLOSEST)
            val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
                bitmap,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
                TensorImageUtils.TORCHVISION_NORM_STD_RGB
            )
            val prediction = modelModule.forward(IValue.from(inputTensor)).toTensor()
            val scores = getMostAccuratePrediction(prediction)
            predictions.add(scores)
            progressStatus += 1
            binding.analyzingProgressBar.progress = progressStatus
        }

        for (i in predictions.indices) {
            println("Prediction for frame $i: ${predictions[i]}")
        }
        startFeedbackVideoActivity()
    }


    private fun getMostAccuratePrediction(tensor: Tensor): Float? {
        val tensorData = tensor.dataAsFloatArray
        return tensorData.maxOrNull()
    }

    private fun startVideoPreview(videoUri: String) {
        binding.recordedVideoView.setVideoURI(Uri.parse(videoUri))
        binding.recordedVideoView.start()
        // loop video
        binding.recordedVideoView.setOnCompletionListener {
            binding.recordedVideoView.start()
        }
    }

    private fun startFeedbackVideoActivity() {
        val intent = Intent(this, FeedbackVideoActivity::class.java)

        // if any of the predictions are round to 0
        intent.putExtra("classifierSuccess", predictions.any { it?.roundToInt() == 0 })

        // times a rounded prediction is 0
        val roundedPredictions = predictions.map { it?.roundToInt() }
        val percentageBetweenRoundedPredictionsAndFrameCount =
            roundedPredictions.count { it == 0 } / predictions.size
        intent.putExtra("classifierSuccessCount", percentageBetweenRoundedPredictionsAndFrameCount)
        startActivity(intent)
    }
}