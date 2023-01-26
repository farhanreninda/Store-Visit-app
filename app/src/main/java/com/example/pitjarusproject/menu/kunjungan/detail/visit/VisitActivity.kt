package com.example.pitjarusproject.menu.kunjungan.detail.visit

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.pitjarusproject.databinding.ActivityVisitBinding
import com.example.pitjarusproject.menu.kunjungan.KunjunganActivity

class VisitActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVisitBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVisitBinding.inflate(layoutInflater)

        initUi()
        setContentView(binding.root)
    }

    private fun initUi() {
        binding.btnDone.setOnClickListener {
            startActivity(
                Intent(this, KunjunganActivity::class.java)
            )
            finish()
        }
    }
}