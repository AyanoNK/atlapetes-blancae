package com.example.atlapetesblancae

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.atlapetesblancae.databinding.ActivityFeedbackVideoBinding

class FeedbackVideoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFeedbackVideoBinding
    private var classifierSuccess = false
    private var classifierPercentage: Float = 0.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbackVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        classifierSuccess = intent.getBooleanExtra("classifierSuccess", false)
        classifierPercentage = intent.getFloatExtra("classifierPercentage", 0.0f) * 100
        println("classifierSuccess: $classifierSuccess")
        if (classifierSuccess) changeLayoutToSuccess()

    }

    private fun changeLayoutToSuccess() {
        binding.root.setBackgroundResource(R.drawable.success_gradient_background)
        binding.titleText.text = resources.getString(R.string.found_prefix)
        binding.birdText.setTextAppearance(androidx.appcompat.R.style.TextAppearance_AppCompat_Display1)
        binding.birdText.setTextColor(resources.getColor(R.color.black, null))
        val floatFormat = "%.2f"
        val formattedPercentage = floatFormat.format(classifierPercentage)
        binding.percentageText.text = String.format(resources.getString(R.string.percentage_suffix), formattedPercentage)
    }

}