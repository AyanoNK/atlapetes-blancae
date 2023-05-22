package com.example.atlapetesblancae

import android.content.Context
import android.graphics.Bitmap
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import kotlin.math.ceil

fun assetFilePath(context: Context, assetName: String?): String? {
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

fun transformSecondsIntoFrameRate(seconds: Int): Int {
    if (seconds == 0) return 1000
    return 1000 / seconds
}

fun transformFrameRateIntoSeconds(frameRate: Int): Int {
    if (frameRate == 0) return 1
    return 1000 / frameRate
}

fun estimateProcessingTime(frames: Int): Int =
    // round up to the nearest second
    ceil(frames * Constants.ESTIMATE_PROCESSING_TIME_SECONDS * Constants.DEFAULT_VIDEO_DURATION).toInt()
