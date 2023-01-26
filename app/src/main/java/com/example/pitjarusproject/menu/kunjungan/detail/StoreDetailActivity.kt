package com.example.pitjarusproject.menu.kunjungan.detail

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.load.resource.bitmap.TransformationUtils
import com.example.pitjarusproject.R
import com.example.pitjarusproject.database.StoreViewModel
import com.example.pitjarusproject.database.Toko
import com.example.pitjarusproject.database.TokoDatabase
import com.example.pitjarusproject.databinding.ActivityStoreDetailBinding
import com.example.pitjarusproject.helper.SharedPreferencesHelper
import com.example.pitjarusproject.menu.HomeMenuActivity
import com.example.pitjarusproject.menu.kunjungan.KunjunganActivity
import com.example.pitjarusproject.menu.kunjungan.detail.visit.VisitActivity
import com.example.pitjarusproject.utils.Constant
import com.example.pitjarusproject.utils.Constant.IMAGE_CAPTURE_CODE
import com.example.pitjarusproject.utils.Constant.PERMISSION_CODE
import com.example.pitjarusproject.utils.Constant.REQUEST_CHECK_SETTINGS
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.maps.android.SphericalUtil
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class StoreDetailActivity : AppCompatActivity(), OnMapReadyCallback, PermissionsListener,
    LocationEngineListener {

    private lateinit var binding: ActivityStoreDetailBinding
    private lateinit var sharedPref: SharedPreferencesHelper
    private lateinit var map: MapboxMap
    private var settingsClient: SettingsClient? = null
    private var mLocationSettingsRequest: LocationSettingsRequest? = null
    private lateinit var permissionManager: PermissionsManager
    private var locationEngine: LocationEngine? = null
    private var locationComponent: LocationComponent? = null
    private var originLocation: Location? = null
    private var imageUri: Uri? = null
    private lateinit var storeViewModel: StoreViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))
        binding = ActivityStoreDetailBinding.inflate(layoutInflater)
        val data = intent.getParcelableExtra<Toko>("data")!!
        sharedPref = SharedPreferencesHelper(this)
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
        initUI(data)
        setContentView(binding.root)
    }

    private fun initUI(data: Toko) {
        binding.btnBack.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    KunjunganActivity::class.java
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
        }
        binding.btnLoc.setOnClickListener {
            enabledLocation()
        }
        binding.tvNameStore.text = data.store_name
        binding.tvAddressStore.text = data.address
        binding.tvStoreCode.text = data.store_code
        binding.tvDcName.text = data.dc_name
        binding.tvAccName.text = data.account_name
        binding.tvSubName.text = data.subchannel_name
        binding.tvChanName.text = data.channel_name
        binding.tvAreaName.text = data.area_name
        binding.tvRegName.text = data.region_name
        sharedPref.put(Constant.LAT, data.latitude)
        sharedPref.put(Constant.LONG, data.longitude)
        binding.tvLastVisit.text = data.last_visit

        binding.btnCam.setOnClickListener {
            permissionOpenCamera()
        }
        binding.btnNoVisit.setOnClickListener {
            startActivity(
                Intent(this, StoreDetailActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
        }
        binding.btnVisit.setOnClickListener {
            if (binding.tvLoc.text == "Lokasi Sudah Sesuai" &&
                sharedPref.getString(Constant.BASE64_SELFIE).toString().isNotEmpty()
            ) {
                val store = Toko(
                    data.id,
                    data.store_id,
                    data.store_code,
                    data.store_name,
                    data.address,
                    data.dc_id,
                    data.dc_name,
                    data.account_id,
                    data.account_name,
                    data.subchannel_id,
                    data.subchannel_name,
                    data.channel_id,
                    data.channel_name,
                    data.area_id,
                    data.area_name,
                    data.region_id,
                    data.region_name,
                    data.latitude,
                    data.longitude,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/y"))
                )
                storeViewModel.updateToko(store)
                sharedPref.put(Constant.BASE64_SELFIE, "")
                Toast.makeText(this, "Berhasil Visit", Toast.LENGTH_SHORT).show()
                startActivity(
                    Intent(this@StoreDetailActivity, VisitActivity::class.java)
                )
                finish()
            } else Toast.makeText(
                this,
                "Lokasi belum sesuai dan foto belum tersedia",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun permissionOpenCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED ||
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
        ) {
            val permission =
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            requestPermissions(permission, PERMISSION_CODE)
        } else {
            openCamera()
        }
    }

    private fun openCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Picture")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera")
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(intent, IMAGE_CAPTURE_CODE)
    }

    @SuppressLint("Range")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                enabledLocation()
            } else if (resultCode == Activity.RESULT_CANCELED) {
                finish()
            }
        }

        if (requestCode == IMAGE_CAPTURE_CODE && resultCode == Activity.RESULT_OK) {
            val contentResolver = contentResolver

            val uri = imageUri
            val uriString = uri.toString()
            val myFile = File(uriString)
            var displayName: String? = null
            if (uriString.startsWith("content://")) {
                var cursor: Cursor? = null
                try {
                    cursor = uri.let { contentResolver.query(it!!, null, null, null, null) }
                    if (cursor != null && cursor.moveToFirst()) {
                        displayName =
                            cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                    }
                } finally {
                    cursor!!.close()
                }
            } else if (uriString.startsWith("file://")) {
                displayName = myFile.name
            }
            try {
                sharedPref.put(Constant.BASE64_SELFIE, convertFileToBase64(uri!!).toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
            binding.ivSelfie.setImageURI(uri)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                binding.box.visibility = View.GONE
                if (requestCode == Constant.REQUEST_SETTINGS_CHECK) {
                    when (resultCode) {
                        RESULT_OK -> {}
                        RESULT_CANCELED -> {
                            Toast.makeText(
                                this@StoreDetailActivity,
                                "User denied to access location",
                                Toast.LENGTH_SHORT
                            ).show()
                            openGpsEnableSetting()
                        }
                    }
                } else if (requestCode == Constant.REQUEST_ENABLE_GPS) {
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
        startActivityForResult(intent, Constant.REQUEST_ENABLE_GPS)
    }

    @SuppressLint("Recycle")
    private fun convertFileToBase64(uri: Uri): String? {
        val stream = contentResolver.openInputStream(uri)
        var bitmap = BitmapFactory.decodeStream(stream)
        bitmap = rotateImageIfRequired(this, bitmap, uri)
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos)
        val byte = baos.toByteArray()
        return Base64.encodeToString(byte, Base64.DEFAULT)
    }

    @SuppressLint("Recycle")
    @Throws(IOException::class)
    private fun rotateImageIfRequired(context: Context, img: Bitmap, selectedImage: Uri): Bitmap? {
        val input = context.contentResolver.openInputStream(selectedImage)
        val ei = ExifInterface(input!!)
        return when (ei.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )) {
            ExifInterface.ORIENTATION_ROTATE_90 -> TransformationUtils.rotateImage(img, 90)
            ExifInterface.ORIENTATION_ROTATE_180 -> TransformationUtils.rotateImage(img, 180)
            ExifInterface.ORIENTATION_ROTATE_270 -> TransformationUtils.rotateImage(img, 270)
            else -> img
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
                        this@StoreDetailActivity,
                        Constant.REQUEST_CHECK_SETTINGS
                    )
                }
            }
        }
    }

    private fun enabledLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            binding.box.visibility = View.GONE
            val builder = LocationSettingsRequest.Builder()
            builder.addLocationRequest(LocationRequest().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY))
            builder.setAlwaysShow(true)
            mLocationSettingsRequest = builder.build()

            settingsClient =
                LocationServices.getSettingsClient(this@StoreDetailActivity)

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
                                this@StoreDetailActivity,
                                Constant.REQUEST_SETTINGS_CHECK
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

        // convert latlong to address
        val lat = location.latitude
        val long = location.longitude

        Geocoder.isPresent()
        getFullAddress(lat, long)
    }

    @SuppressLint("ResourceAsColor", "SetTextI18n")
    private fun getFullAddress(lat: Double, long: Double) {
        try {
            val store = com.google.android.gms.maps.model.LatLng(
                sharedPref.getString(Constant.LAT)!!.toDouble(),
                sharedPref.getString(Constant.LONG)!!.toDouble()
            )
            val curLoc = com.google.android.gms.maps.model.LatLng(lat, long)
            val distance: Double = SphericalUtil.computeDistanceBetween(store, curLoc)

            if (DecimalFormat("#####").format(distance / 0.68).toDouble() > 100) {
                binding.tvLoc.setTextColor(Color.parseColor("#ff0000"))
                binding.tvLoc.text = "Lokasi Belum Sesuai"
            } else {
                binding.tvLoc.setTextColor(Color.parseColor("#00ff00"))
                binding.tvLoc.text = "Lokasi Sudah Sesuai"
            }

        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Restart HP untuk mendapatkan lokasi akurat !", Toast.LENGTH_SHORT)
                .show()
        }
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
}