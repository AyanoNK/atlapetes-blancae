package com.example.atlapetesblancae

import android.Manifest
object Constants {
    const val TAG = "cameraX"
    const val FILE_NAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    const val EMAIL_TO_SHARE = "victoria.isaza@udea.edu.co"
    const val DEFAULT_VIDEO_DURATION = 10
    const val REQUEST_CODE_PERMISSIONS = 123
    const val ESTIMATE_PROCESSING_TIME_SECONDS = 1.25
    const val VIDEO_DEBUGGER = false
    var FRAMES_PER_SECOND = 1000
    val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
    )
}