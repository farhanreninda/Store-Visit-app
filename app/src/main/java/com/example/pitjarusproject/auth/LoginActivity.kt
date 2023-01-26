package com.example.pitjarusproject.auth

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.example.pitjarusproject.utils.Constant
import com.example.pitjarusproject.menu.HomeMenuActivity
import com.example.pitjarusproject.helper.SharedPreferencesHelper
import com.example.pitjarusproject.api.APIConfig
import com.example.pitjarusproject.api.response.Response
import com.example.pitjarusproject.api.response.StoresItem
import com.example.pitjarusproject.database.StoreViewModel
import com.example.pitjarusproject.database.Toko
import com.example.pitjarusproject.database.TokoDatabase
import com.example.pitjarusproject.databinding.ActivityLoginBinding
import retrofit2.Call

class LoginActivity : AppCompatActivity() {

    private var _binding: ActivityLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedPref: SharedPreferencesHelper
    private val storesList: MutableList<StoresItem> = mutableListOf()
    private lateinit var storeViewModel: StoreViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityLoginBinding.inflate(layoutInflater)
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
        binding.progressBar.visibility = View.GONE
        binding.btnLogin.setOnClickListener {
            validateUserPass()
        }
        val getVersion =
            this@LoginActivity.packageManager.getPackageInfo(
                this@LoginActivity.packageName,
                0
            )
        binding.appVer.text = "App Ver ${getVersion.versionName} (${getVersion.versionCode})"
    }

    private fun validateUserPass() {
        when {
            TextUtils.isEmpty(binding.etAccount.text) -> {
                binding.etAccount.error = "Silahkan isi username"
                Toast.makeText(this, "Silahkan isi username", Toast.LENGTH_SHORT).show()
                binding.etAccount.requestFocus()
            }
            TextUtils.isEmpty(binding.etPassword.text) -> {
                binding.etPassword.error = "Silahkan isi password"
                Toast.makeText(this, "Silahkan isi password", Toast.LENGTH_SHORT).show()
                binding.etPassword.requestFocus()
            }
            else -> sendDataLogin()
        }
    }

    private fun sendDataLogin() {
        binding.btnLogin.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE

        APIConfig.instanceRetrofit.getData(
            binding.etAccount.text.toString(),
            binding.etPassword.text.toString(),
        ).enqueue(object : retrofit2.Callback<Response> {
            override fun onResponse(
                call: Call<Response>,
                response: retrofit2.Response<Response>,
            ) {
                when (response.body()?.status) {
                    "success" -> {
                        binding.progressBar.visibility = View.GONE
                        sharedPref.put(Constant.PREF_LOGIN, true)
                        response.body()?.let {
                            storesList.addAll(it.stores)
                        }
                        addStores()
                        startActivity(
                            Intent(this@LoginActivity, HomeMenuActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        )
                        finish()
                    }
                    else -> {
                        Toast.makeText(
                            this@LoginActivity,
                            "Username dan Password salah !",
                            Toast.LENGTH_SHORT
                        ).show()
                        binding.btnLogin.visibility = View.VISIBLE
                        binding.progressBar.visibility = View.GONE
                    }
                }
            }

            override fun onFailure(call: Call<Response>, t: Throwable) {
                Handler().postDelayed({
                    Toast.makeText(
                        this@LoginActivity,
                        "Server Not Responding",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    binding.btnLogin.visibility = View.VISIBLE
                    binding.progressBar.visibility = View.GONE
                }, 30000)
            }
        })
    }

    private fun addStores() {
        val storesList = ArrayList<StoresItem>(storesList)
        val list: List<StoresItem> = storesList
        for (data in list) {
            val store = Toko()

            store.store_id = data.storeId.toString()
            sharedPref.putInt(Constant.TOTAL_STORE, list.size)
            store.store_code = data.storeCode.toString()
            store.store_name = data.storeName.toString()
            store.address = data.address.toString()
            store.dc_id = data.dcId.toString()
            store.dc_name = data.dcName.toString()
            store.account_id = data.accountId.toString()
            store.account_name = data.accountName.toString()
            store.subchannel_id = data.subchannelId.toString()
            store.subchannel_name = data.subchannelName.toString()
            store.channel_id = data.channelId.toString()
            store.channel_name = data.channelId.toString()
            store.area_id = data.areaId.toString()
            store.area_name = data.areaName.toString()
            store.region_id = data.regionId.toString()
            store.region_name = data.regionName.toString()
            store.latitude = data.latitude.toString()
            store.longitude = data.longitude.toString()
            store.last_visit = ""

            storeViewModel.insertDataToko(store)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            onBackPressed()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        finish()
    }
}