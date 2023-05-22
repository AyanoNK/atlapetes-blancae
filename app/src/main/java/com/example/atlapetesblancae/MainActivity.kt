package com.example.atlapetesblancae

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.atlapetesblancae.databinding.ActivityMainBinding
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("CURRENT FRAMES PER SECOND: ${Constants.FRAMES_PER_SECOND}")

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

        binding.testerButton.setOnClickListener {
            val testerIntent = Intent(this, ReviewVideoActivity::class.java)
            startActivity(testerIntent)
        }

        binding.testButtonn.setOnClickListener {
            val feedbackIntent = Intent(this, FeedbackVideoActivity::class.java)
            feedbackIntent.putExtra("classifierSuccess", true)
            feedbackIntent.putExtra("classifierPercentage", 1f)
            startActivity(feedbackIntent)
        }

        binding.configurationButton.setOnClickListener {
            val configurationIntent = Intent(this, ConfigurationActivity::class.java)
            startActivity(configurationIntent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (requestCode == Constants.REQUEST_CODE_PERMISSIONS) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            binding.btnTakeRecording.apply {
                isEnabled = true
                text = R.string.start_capture.toString()
            }
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