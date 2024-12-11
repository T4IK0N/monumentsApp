package com.example.zabytki

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.telecom.Call
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentContainerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_REQUEST_CODE = 1000

    private lateinit var placesClient: PlacesClient
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PopularAttractionAdapter

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Standardowe ustawienia status bara
        window.navigationBarColor = resources.getColor(R.color.lt_background_color, theme)
        this.window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        WindowCompat.getInsetsController(this.window, this.window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        val mainTextView = findViewById<TextView>(R.id.mainText)
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2) // 2 columns grid

//        val offlineAttractions = listOf(
//            PopularAttraction(R.drawable.atrakcja1, "Attraction 1", "4.5"),
//            PopularAttraction(R.drawable.atrakcja2, "Attraction 2", "4.7"),
//            PopularAttraction(R.drawable.atrakcja3, "Attraction 3", "3.2"),
//            PopularAttraction(R.drawable.atrakcja4, "Attraction 4", "5.0")
//        )

//        val adapter = PopularAttractionAdapter(this, offlineAttractions)
        adapter = PopularAttractionAdapter(this, listOf())
        recyclerView.adapter = adapter

        onBackPressedDispatcher.addCallback(this) {
            // Pass ( disable back arrow)
        }

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "API_KEY")
        }
        placesClient = Places.createClient(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

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

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        findViewById<ImageView>(R.id.locationIcon).setOnClickListener {
            showUserLocation()
        }

        findViewById<ImageView>(R.id.mapIcon).setOnClickListener {
            searchLocationAndShowOnMap(map, mainTextView.text.toString())
        }

        val apiKey = "API_KEY"
        fetchNearbyPlaces(52.2297, 21.0122, apiKey) { placeIds ->
            placeIds.forEach { placeId ->
                fetchPlaceDetails(placeId, apiKey) { name, rating, photoReference ->
//                    println("Place Name: $name, Rating: $rating, Photo: $photoReference")

                    findViewById<TextView>(R.id.b).text = "Place Name: $name, Rating: $rating, Photo: $photoReference"
                    Log.d("NearbyPlaces", "Fetched Place IDs: $placeIds")
                    Log.d("PlaceDetails", "Fetched Details: $name, $rating, $photoReference")
                    if (photoReference != null) {
                        loadPlacePhoto(photoReference, apiKey, findViewById(R.id.a))
                    }
                }
            }
        }
    }

    // user location (your localisation)

    private fun showUserLocation() {
        checkLocationPermissionAndShowMap { location ->
            val userLatLng = LatLng(location.latitude, location.longitude)
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@checkLocationPermissionAndShowMap
            }
            map.isMyLocationEnabled = true
            updateMapWithLocation(userLatLng, "To ty!")
        }
    }

    private fun checkLocationPermissionAndShowMap(callback: (Location) -> Unit) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_REQUEST_CODE
            )
        } else {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let(callback) ?: run {
                    Toast.makeText(this, "Nie udało się uzyskać lokalizacji", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // searched location (default warsaw)

    private fun searchLocationAndShowOnMap(map: GoogleMap, query: String): LatLng {
        val geocoder = Geocoder(this)
        val addressList = geocoder.getFromLocationName(query, 1)
        if (addressList.isNullOrEmpty()) {
            Toast.makeText(this, "Nie znaleziono lokalizacji: $query", Toast.LENGTH_SHORT).show()
            return LatLng(0.0, 0.0)
        }

        val location = addressList[0]
        val searchedLatLng = LatLng(location.latitude, location.longitude)
        updateMapWithLocation(searchedLatLng, query)

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(searchedLatLng, 15f))
        return searchedLatLng
    }

    private fun updateMapWithLocation(latLng: LatLng, title: String) {
        findViewById<FragmentContainerView>(R.id.map).visibility = View.VISIBLE
        map.clear()
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        map.addMarker(MarkerOptions().position(latLng).title(title))
    }

    // map cleaning/preparing

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = false
        map.uiSettings.isMapToolbarEnabled = false

        findViewById<TextView>(R.id.mainText).setOnClickListener {
            findViewById<FragmentContainerView>(R.id.map).visibility = View.GONE // hide map
        }
    }

    // check localisation perm

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showUserLocation()
            } else {
                Toast.makeText(this, "Brak dostępu do lokalizacji", Toast.LENGTH_SHORT).show()
            }
        }
    }





    // load photos

    fun loadPlacePhoto(photoReference: String, apiKey: String, imageView: ImageView) {
        val photoUrl = "https://maps.googleapis.com/maps/api/place/photo" +
                "?maxwidth=400&photo_reference=$photoReference&key=$apiKey"

        Glide.with(imageView.context)
            .load(photoUrl)
            .into(imageView)
    }
}