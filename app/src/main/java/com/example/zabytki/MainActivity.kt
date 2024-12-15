package com.example.zabytki

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentContainerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_REQUEST_CODE = 1000

    private lateinit var placesClient: PlacesClient
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PopularAttractionAdapter

    private lateinit var responseTextView: TextView
    private lateinit var mainTextView: TextView

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

        mainTextView = findViewById(R.id.mainText)
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
            Places.initialize(applicationContext, "INSERT_API_KEY")
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

        val warsawLatLng = LatLng(52.237049, 21.017532)
        fetchNearbyPlacesAndShowInRecyclerView(warsawLatLng)


        responseTextView = findViewById(R.id.responseTextView)

//        val apiKey = "INSERT_API_KEY"
//        val placeId = "ChIJj61dQgK6j4AR4GeTYWZsKWw"

//        getPlaceDetails(apiKey, placeId)
    }

    // Funkcja do pobierania Nearby Search i aktualizacji RecyclerView
    private fun fetchNearbyPlacesAndShowInRecyclerView(latLng: LatLng) {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()

        val apiKey = "INSERT_API_KEY"

        val requestBody = """
        {
          "includedTypes": ["restaurant"],
          "maxResultCount": 10,
          "locationRestriction": {
            "circle": {
              "center": {
                "latitude": ${latLng.latitude},
                "longitude": ${latLng.longitude}
              },
              "radius": 1500.0
            }
          }
        }
    """.trimIndent()

        val request = Request.Builder()
            .url("https://places.googleapis.com/v1/places:searchNearby")
            .addHeader("Content-Type", "application/json")
            .addHeader("X-Goog-Api-Key", apiKey)
//            .addHeader("X-Goog-FieldMask", "places.displayName,places.location,places.photos,places.rating")
            .addHeader("X-Goog-FieldMask", "places.displayName,places.location,places.photos")
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), requestBody))
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val attractions = parsePlacesToAttractionList(responseBody)

                    runOnUiThread {
                        adapter.updateAttractions(attractions)
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Błąd: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: okhttp3.Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Błąd połączenia: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun parsePlacesToAttractionList(response: String?): List<PopularAttraction> {
        val apiKey = "INSERT_API_KEY"
        val attractions = mutableListOf<PopularAttraction>()

        if (response == null) return attractions
        try {
            val jsonObject = JSONObject(response)
            val placesArray = jsonObject.getJSONArray("places")

            for (i in 0 until placesArray.length()) {
                val place = placesArray.getJSONObject(i)
                val displayName = place.getJSONObject("displayName").getString("text")
                val rating = place.optDouble("rating", 0.0).toString()
                val photos = place.optJSONArray("photos")
                val photoUrl = photos?.let {
                    val photoReference = it.getJSONObject(0).getString("name")
                    "https://places.googleapis.com/v1/ $photoReference/media?maxHeightPx=400&maxWidthPx=400&key=$apiKey"
                } ?: "https://via.placeholder.com/400"

                attractions.add(PopularAttraction(photoUrl, displayName, rating))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return attractions
    }

    // nearby search (on map)

    private fun searchNearbyPlacesAndShowOnMap(latLng: LatLng) {
        val client = OkHttpClient()
        val apiKey = "INSERT_API_KEY"

        val requestBody = """
        {
          "includedTypes": ["restaurant"],
          "maxResultCount": 10,
          "locationRestriction": {
            "circle": {
              "center": {
                "latitude": ${latLng.latitude},
                "longitude": ${latLng.longitude}
              },
              "radius": 1500.0
            }
          }
        }
    """.trimIndent()

        val request = Request.Builder()
            .url("https://places.googleapis.com/v1/places:searchNearby")
            .addHeader("Content-Type", "application/json")
            .addHeader("X-Goog-Api-Key", apiKey)
            .addHeader("X-Goog-FieldMask", "places.displayName,places.location")
            .post(RequestBody.create(null, requestBody))
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val places = parseNearbyPlaces(responseBody)

                    runOnUiThread {
                        map.clear()
                        updateMapWithLocation(latLng)
                        addNearbyPlaceMarkers(places)
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Błąd: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: okhttp3.Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Błąd połączenia: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun parseNearbyPlaces(response: String?): List<Pair<String, LatLng>> {
        val placesList = mutableListOf<Pair<String, LatLng>>()
        if (response == null) return placesList

        try {
            val jsonObject = JSONObject(response)
            val places = jsonObject.getJSONArray("places")

            for (i in 0 until places.length()) {
                val place = places.getJSONObject(i)
                val displayName = place.getJSONObject("displayName").getString("text")
                val location = place.getJSONObject("location")
                val lat = location.getDouble("latitude")
                val lng = location.getDouble("longitude")
                placesList.add(Pair(displayName, LatLng(lat, lng)))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return placesList
    }

    private fun addNearbyPlaceMarkers(places: List<Pair<String, LatLng>>) {
        for ((name, latLng) in places) {
            map.addMarker(MarkerOptions().position(latLng).title(name))
        }
    }

    // user location

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
            updateMapWithLocation(userLatLng)
            searchNearbyPlacesAndShowOnMap(userLatLng)
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
        updateMapWithLocation(searchedLatLng)

        searchNearbyPlacesAndShowOnMap(searchedLatLng)

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(searchedLatLng, 15f))
        return searchedLatLng
    }

    private fun updateMapWithLocation(latLng: LatLng) {
        findViewById<FragmentContainerView>(R.id.map).visibility = View.VISIBLE
        map.clear()
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
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
}