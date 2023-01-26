package com.example.pitjarusproject.menu

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.example.pitjarusproject.auth.LoginActivity
import com.example.pitjarusproject.database.StoreViewModel
import com.example.pitjarusproject.database.Toko
import com.example.pitjarusproject.database.TokoDatabase
import com.example.pitjarusproject.databinding.ActivityHomeMenuBinding
import com.example.pitjarusproject.helper.SharedPreferencesHelper
import com.example.pitjarusproject.menu.kunjungan.KunjunganActivity
import com.example.pitjarusproject.utils.Constant
import com.example.pitjarusproject.utils.DialogUtils

class HomeMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeMenuBinding
    private lateinit var sharedPref: SharedPreferencesHelper
    private var doubleBackToExitPressedOnce: Boolean = false
    private lateinit var storeViewModel: StoreViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeMenuBinding.inflate(layoutInflater)
        sharedPref = SharedPreferencesHelper(this)
        val application = requireNotNull(this).application
        val dataSource = TokoDatabase.getInstance(application).tokoDao
        val factory = StoreViewModel.Factory(dataSource, application)
        storeViewModel = ViewModelProvider(this, factory)[StoreViewModel::class.java]

        initUi()

        setContentView(binding.root)
    }

    @SuppressLint("SetTextI18n")
    private fun initUi() {
        binding.tvTotalStore.text = sharedPref.getInt(Constant.TOTAL_STORE).toString()
        binding.tvActualStore.text = sharedPref.getInt(Constant.ACTUAL_STORE).toString()
        val percentage = ((binding.tvActualStore.text.toString().toInt().toDouble() /
                binding.tvTotalStore.text.toString().toInt().toDouble()) * 100).toString().split(".")[0]
        binding.tvPercenStore.text = "$percentage%"
        binding.kunjungan.setOnClickListener {
            startActivity(Intent(this, KunjunganActivity::class.java))
        }
        binding.logout.setOnClickListener {
            DialogUtils.showDialog(
                this,
                "Peringatan",
                "Yakin Ingin Logout ?"
            ) { logout() }
        }
    }

    private fun logout() {
        sharedPref.clear()
        storeViewModel.toko.observe({ lifecycle }, {
            deleteDataToko(it)
        })
        startActivity(
            Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }

    private fun deleteDataToko(items: List<Toko>){
        val dataList = ArrayList<Toko>(items)
        val list: List<Toko> = dataList
        for (data in list) {
            storeViewModel.deleteToko(data.id)
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else if (!doubleBackToExitPressedOnce) {
            doubleBackToExitPressedOnce = true
            Toast.makeText(this, "Tekan lagi untuk keluar", Toast.LENGTH_SHORT).show()
            Handler().postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
        } else {
            super.onBackPressed()
            return
        }
    }
}