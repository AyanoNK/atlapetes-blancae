package com.example.atlapetesblancae

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.atlapetesblancae.databinding.ActivityFeedbackVideoBinding
import org.pytorch.LiteModuleLoader
import java.io.File
import java.io.FileOutputStream

class FeedbackVideoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFeedbackVideoBinding
    private var classifierSuccess = false
    private var classifierPercentage: Float = 0.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbackVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        println("CURRENT FRAMES PER SECOND: ${Constants.FRAMES_PER_SECOND}")
        classifierSuccess = intent.getBooleanExtra("classifierSuccess", false)
        classifierPercentage = intent.getFloatExtra("classifierPercentage", 0.0f) * 100
        println("classifierSuccess: $classifierSuccess")
        showTransformedPercentage()
        if (classifierSuccess) changeLayoutToSuccess()

        binding.sendVideoButton.setOnClickListener {

            val stored1080pVideo = resources.openRawResource(R.raw.test2)
            // model expects video to be loaded
            val mModule = LiteModuleLoader.load(assetFilePath(this, "modell.ptl"))
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

            // attach the video and change to gmail app to send
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(Constants.EMAIL_TO_SHARE))
                putExtra(Intent.EXTRA_SUBJECT, resources.getString(R.string.email_subject))
                putExtra(Intent.EXTRA_TEXT, resources.getString(R.string.email_body))
                putExtra(Intent.EXTRA_STREAM, Uri.parse(tempFile.absolutePath))
                try {
                    startActivity(Intent.createChooser(
                        this, resources.getString(R.string.send_video))
                    )
                } catch (ex: android.content.ActivityNotFoundException) {
                    // if gmail app is not installed, open browser
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.EMAIL_TO_SHARE)))
                }
            }
        }
    }

    override fun onBackPressed() {
        returnHome(this)
    }

    private fun changeLayoutToSuccess() {
        binding.root.setBackgroundResource(R.drawable.success_gradient_background)
        binding.titleText.text = resources.getString(R.string.found_prefix)
        binding.birdText.setTextAppearance(androidx.appcompat.R.style.TextAppearance_AppCompat_Display1)
        binding.birdText.setTextColor(resources.getColor(R.color.black, null))
        binding.sendVideoButton.visibility = android.view.View.VISIBLE
    }

    private fun showTransformedPercentage() {
        val floatFormat = "%.2f"
        val formattedPercentage = floatFormat.format(classifierPercentage)
        binding.percentageText.text = String.format(resources.getString(R.string.percentage_suffix), formattedPercentage)
    }

}