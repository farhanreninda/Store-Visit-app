package com.example.pitjarusproject

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.example.pitjarusproject.auth.LoginActivity
import com.example.pitjarusproject.databinding.ActivityMainBinding
import com.example.pitjarusproject.helper.SharedPreferencesHelper
import com.example.pitjarusproject.menu.HomeMenuActivity
import com.example.pitjarusproject.utils.Constant

class MainActivity : AppCompatActivity() {
    private val loadTime = 3000
    private lateinit var sharedPref: SharedPreferencesHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ActivityMainBinding.inflate(layoutInflater).root)
        sharedPref = SharedPreferencesHelper(this)

        if (sharedPref.getBoolean(Constant.PREF_LOGIN)) {
            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(
                    Intent(
                        this,
                        HomeMenuActivity::class.java
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
                finish()
            }, loadTime.toLong())
        } else {
            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(
                    Intent(
                        this,
                        LoginActivity::class.java
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
                finish()
            }, loadTime.toLong())
        }
    }
}