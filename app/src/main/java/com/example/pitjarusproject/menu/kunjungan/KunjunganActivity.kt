package com.example.pitjarusproject.menu.kunjungan

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.os.TransactionTooLargeException
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.pitjarusproject.R
import com.example.pitjarusproject.database.StoreViewModel
import com.example.pitjarusproject.database.Toko
import com.example.pitjarusproject.database.TokoDatabase
import com.example.pitjarusproject.databinding.ActivityKunjunganBinding
import com.example.pitjarusproject.helper.SharedPreferencesHelper
import com.example.pitjarusproject.menu.HomeMenuActivity
import com.example.pitjarusproject.menu.kunjungan.detail.StoreDetailActivity
import com.example.pitjarusproject.utils.Constant
import com.example.pitjarusproject.utils.Constant.REQUEST_CHECK_SETTINGS
import com.example.pitjarusproject.utils.Constant.REQUEST_ENABLE_GPS
import com.example.pitjarusproject.utils.Constant.REQUEST_SETTINGS_CHECK
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineListener
import com.mapbox.android.core.location.LocationEnginePriority
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import kotlinx.android.synthetic.main.item_rv.view.*
import org.marproject.reusablerecyclerviewadapter.ReusableAdapter
import org.marproject.reusablerecyclerviewadapter.interfaces.AdapterCallback
import java.util.*

class KunjunganActivity : AppCompatActivity(), OnMapReadyCallback, PermissionsListener,
    LocationEngineListener {

    private lateinit var binding: ActivityKunjunganBinding
    private lateinit var map: MapboxMap
    private var settingsClient: SettingsClient? = null
    private var mLocationSettingsRequest: LocationSettingsRequest? = null
    private lateinit var permissionManager: PermissionsManager
    private var locationEngine: LocationEngine? = null
    private var locationComponent: LocationComponent? = null
    private var originLocation: Location? = null
    private lateinit var storeViewModel: StoreViewModel
    private lateinit var sharedPref: SharedPreferencesHelper
    var count = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))
        binding = ActivityKunjunganBinding.inflate(layoutInflater)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.READ_PHONE_STATE
                    )
                ) {
                    binding.mapBox.onCreate(savedInstanceState)
                    binding.mapBox.getMapAsync(this)
                }
            }
        } else {
            binding.mapBox.onCreate(savedInstanceState)
            binding.mapBox.getMapAsync(this)
        }

        settingsClient = LocationServices.getSettingsClient(this)
        permissionManager = PermissionsManager(this)
        val application = requireNotNull(this).application
        val dataSource = TokoDatabase.getInstance(application).tokoDao
        val factory = StoreViewModel.Factory(dataSource, application)
        storeViewModel = ViewModelProvider(this, factory)[StoreViewModel::class.java]
        sharedPref = SharedPreferencesHelper(this)

        initUi()

        setContentView(binding.root)
    }

    private fun initUi() {
        binding.btnBack.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    HomeMenuActivity::class.java
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
        }
        storeViewModel.toko.observe({ lifecycle }, {
            setupAdapter(binding.rvToko, it)
        })
    }

    private fun setupAdapter(recyclerView: RecyclerView, items: List<Toko>) {
        ReusableAdapter<Toko>(this)
            .adapterCallback(adapterCallback)
            .setLayout(R.layout.item_rv)
            .addData(items)
            .isVerticalView()
            .build(recyclerView)
    }

    private val adapterCallback = object : AdapterCallback<Toko> {
        @SuppressLint("SetTextI18n")
        override fun initComponent(itemView: View, data: Toko, itemIndex: Int) {
            itemView.tv_name.text = data.store_code
            itemView.tv_cluster.text = data.store_name
            itemView.tv_store.text = data.address
            if (data.last_visit != "") {
                itemView.iv_done.visibility = View.VISIBLE
                sharedPref.putInt(Constant.ACTUAL_STORE, ++count)
            } else itemView.background_distance.visibility = View.VISIBLE
        }

        override fun onItemClicked(itemView: View, data: Toko, itemIndex: Int) {
            try {
                startActivity(
                    Intent(
                        this@KunjunganActivity,
                        StoreDetailActivity::class.java
                    ).putExtra("data", data)
                )
            } catch (e: TransactionTooLargeException) {
                Toast.makeText(
                    this@KunjunganActivity,
                    e.toString(),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onMapReady(mapboxMap: MapboxMap?) {
        map = mapboxMap ?: return
        val locationRequestBuilder = LocationSettingsRequest.Builder().addLocationRequest(
            LocationRequest()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        )
        val locationRequest = locationRequestBuilder.build()

        settingsClient?.checkLocationSettings(locationRequest)?.run {
            addOnSuccessListener {
                enabledLocation()
            }
            addOnFailureListener {
                val statusCode = (it as ApiException).statusCode

                if (statusCode == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                    val resolvableException = it as? ResolvableApiException
                    resolvableException?.startResolutionForResult(
                        this@KunjunganActivity,
                        REQUEST_CHECK_SETTINGS
                    )
                }
            }
        }
    }

    private fun enabledLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val builder = LocationSettingsRequest.Builder()
            builder.addLocationRequest(LocationRequest().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY))
            builder.setAlwaysShow(true)
            mLocationSettingsRequest = builder.build()

            settingsClient =
                LocationServices.getSettingsClient(this@KunjunganActivity)

            settingsClient!!
                .checkLocationSettings(mLocationSettingsRequest!!)
                .addOnSuccessListener {
                    if (PermissionsManager.areLocationPermissionsGranted(this)) {
                        initializeLocationComponent()
                        initializeLocationEngine()
                    } else {
                        permissionManager = PermissionsManager(this)
                        permissionManager.requestLocationPermissions(this)
                    }
                }
                .addOnFailureListener { e ->
                    when ((e as ApiException).statusCode) {
                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                            val rae = e as ResolvableApiException
                            rae.startResolutionForResult(
                                this@KunjunganActivity,
                                REQUEST_SETTINGS_CHECK
                            )
                        } catch (sie: IntentSender.SendIntentException) {
                            Toast.makeText(
                                this,
                                "Tidak dapat menjalankan permintaan.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE ->
                            Toast.makeText(
                                this,
                                "Pengaturan lokasi tidak memadai, dan tidak dapat diperbaiki di sini. Perbaiki di Pengaturan.",
                                Toast.LENGTH_SHORT
                            ).show()
                    }
                }
                .addOnCanceledListener {
                    Toast.makeText(
                        this,
                        "Periksa Pengaturan Lokasi -> Di Dibatalkan",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } else {
            if (PermissionsManager.areLocationPermissionsGranted(this)) {
                initializeLocationComponent()
                initializeLocationEngine()
            } else {
                permissionManager = PermissionsManager(this)
                permissionManager.requestLocationPermissions(this)
            }
        }
    }

    private fun initializeLocationComponent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                locationComponent = map.locationComponent
                locationComponent?.activateLocationComponent(this)
                locationComponent?.isLocationComponentEnabled = true
                locationComponent?.cameraMode =
                    com.mapbox.mapboxsdk.location.modes.CameraMode.TRACKING
                return
            }
        } else {
            locationComponent = map.locationComponent
            locationComponent?.activateLocationComponent(this)
            locationComponent?.isLocationComponentEnabled = true
            locationComponent?.cameraMode =
                com.mapbox.mapboxsdk.location.modes.CameraMode.TRACKING
        }
    }

    private fun initializeLocationEngine() {
        locationEngine = LocationEngineProvider(this).obtainBestLocationEngineAvailable()
        locationEngine?.priority = LocationEnginePriority.HIGH_ACCURACY
        locationEngine?.activate()
        locationEngine?.addLocationEngineListener(this)

        val lastLocation = locationEngine?.lastLocation
        if (lastLocation != null) {
            originLocation = lastLocation
            setCameraPosition(lastLocation)
        } else {
            locationEngine?.addLocationEngineListener(this)
        }
    }

    private fun setCameraPosition(location: Location) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.READ_PHONE_STATE
                    )
                ) {
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(
                                location.latitude,
                                location.longitude
                            ), 30.0
                        )
                    )
                }
            }
        } else {
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(
                        location.latitude,
                        location.longitude
                    ), 30.0
                )
            )
        }
        Geocoder.isPresent()
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Toast.makeText(
            this,
            permissionsToExplain?.get(0),
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            enabledLocation()
        } else {
            Toast.makeText(this, "User location was not granted", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onConnected() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        locationEngine?.requestLocationUpdates()
        val toast = Toast.makeText(this, "Please Wait!", Toast.LENGTH_LONG)

        val countDownTimer = object : CountDownTimer(15000, 2000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                toast.cancel()
            }
        }

        toast.show()
        countDownTimer.start()
    }

    override fun onLocationChanged(location: Location?) {
        if (location != null) {
            location.run {
                originLocation = this
                setCameraPosition(this)
            }
        } else Toast.makeText(this, "Location is null", Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                enabledLocation()
            } else if (resultCode == Activity.RESULT_CANCELED) {
                finish()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                binding.box.visibility = View.GONE
                if (requestCode == REQUEST_SETTINGS_CHECK) {
                    when (resultCode) {
                        RESULT_OK -> {}
                        RESULT_CANCELED -> {
                            Toast.makeText(
                                this@KunjunganActivity,
                                "User denied to access location",
                                Toast.LENGTH_SHORT
                            ).show()
                            openGpsEnableSetting()
                        }
                    }
                } else if (requestCode == REQUEST_ENABLE_GPS) {
                    val locationManager =
                        getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    val isGpsEnabled =
                        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    if (!isGpsEnabled) {
                        openGpsEnableSetting()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    applicationContext, "activity :$e",
                    Toast.LENGTH_LONG
                ).show()
                startActivity(Intent(this, HomeMenuActivity::class.java))
            }
        }
    }
    private fun openGpsEnableSetting() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivityForResult(intent, REQUEST_ENABLE_GPS)
    }
}