package com.example.atlapetesblancae

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

fun returnHome(packageContext: AppCompatActivity) {
    val intent = Intent(packageContext, MainActivity::class.java)
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    ContextCompat.startActivity(packageContext, intent, null)
}