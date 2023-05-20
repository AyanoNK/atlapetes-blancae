package com.example.atlapetesblancae

import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.example.atlapetesblancae.databinding.ActivityConfigurationBinding

class ConfigurationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfigurationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfigurationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        updateFrameRateText()

        binding.homeButton.setOnClickListener {
            finish()
        }

        // take seekbar value and change default frame rate
        binding.fpsSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                changeDefaultFrameRate(seekBar.progress)
            }
        })

    }

    private fun changeDefaultFrameRate(seconds: Int) {
        val newFrameRate = transformSecondsIntoFrameRate(seconds)
        Constants.FRAMES_PER_SECOND = newFrameRate
        updateFrameRateText()
    }

    private fun updateFrameRateText() {
        val secondsFrameRate = transformFrameRateIntoSeconds(Constants.FRAMES_PER_SECOND)
        val processingTime = "${estimateProcessingTime(secondsFrameRate)} ${getString(R.string.seconds)}"
        binding.fpsText.text = secondsFrameRate.toString()
        binding.secondsText.text = processingTime
    }

}