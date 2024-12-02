package com.example.zabytki

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.telecom.Call
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentContainerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
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

//        onBackPressedDispatcher.addCallback(this) {
//            // Pass (its back arrow)
//        }


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
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val userLatLng = LatLng(location.latitude, location.longitude)
                fetchPopularAttractions(userLatLng)
            } else {
                val defaultLocation = LatLng(52.2297, 21.0122) // Domyślnie Warszawa
                fetchPopularAttractions(defaultLocation)
            }
        }


        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        findViewById<ImageView>(R.id.locationIcon).setOnClickListener {
            showUserLocation()
        }

//        findViewById<ImageView>(R.id.mapIcon).setOnClickListener {
//
//        }

        findViewById<ImageView>(R.id.mapIcon).setOnClickListener {
            val mainText = mainTextView.text.toString()
            searchLocationAndShowOnMap(mainText)
            val defaultLocation = LatLng(52.2297, 21.0122) // domyślnie Warszawa
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f))
            showNearbyPlaces(defaultLocation)
        }


    }

    private fun fetchPopularAttractions(location: LatLng) {
        val radius = 1000 // 1 km
        val typeQuery = listOf("tourist_attraction", "restaurant", "park").joinToString("|")

        val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=${location.latitude},${location.longitude}" +
                "&radius=$radius" +
                "&type=$typeQuery" +
                "&key=API_KEY"

        val queue = Volley.newRequestQueue(this)
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                val results = response.getJSONArray("results")
                val attractions = mutableListOf<PopularAttraction>()

                for (i in 0 until results.length()) {
                    val place = results.getJSONObject(i)

                    val name = place.getString("name")
                    val rating = place.optDouble("rating", 0.0).toString()
                    val photoReference = place.optJSONArray("photos")?.getJSONObject(0)
                        ?.getString("photo_reference")

                    val imageUrl = if (photoReference != null) {
                        "https://maps.googleapis.com/maps/api/place/photo?" +
                                "maxwidth=400" +
                                "&photo_reference=$photoReference" +
                                "&key=API_KEY"
                    } else {
                        null
                    }

                    if (imageUrl != null) {
                        attractions.add(PopularAttraction(imageUrl, name, rating))
                    }

                    // Break if we already have 10 results
                    if (attractions.size == 10) break
                }

                // Update RecyclerView
                updateRecyclerView(attractions)
            },
            { error ->
                Toast.makeText(this, "Error fetching attractions: $error", Toast.LENGTH_SHORT).show()
            }
        )

        queue.add(jsonObjectRequest)
    }

    private fun updateRecyclerView(attractions: List<PopularAttraction>) {
        adapter.updateAttractions(attractions)
    }







    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = false
        map.uiSettings.isMapToolbarEnabled = false

        findViewById<FragmentContainerView>(R.id.map).visibility = View.GONE // hide map
    }

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
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return@checkLocationPermissionAndShowMap
            }
            map.isMyLocationEnabled = true
            updateMapWithLocation(userLatLng, "Twoja lokalizacja")
            showNearbyMonuments(userLatLng)
        }
    }

    private fun searchLocationAndShowOnMap(query: String) {
        val geocoder = Geocoder(this)
        val addressList = geocoder.getFromLocationName(query, 1)
        if (addressList.isNullOrEmpty()) {
            Toast.makeText(this, "Nie znaleziono lokalizacji: $query", Toast.LENGTH_SHORT).show()
            return
        }

        val location = addressList[0]
        val searchedLatLng = LatLng(location.latitude, location.longitude)
        updateMapWithLocation(searchedLatLng, query)
        showNearbyMonuments(searchedLatLng)
    }

    private fun updateMapWithLocation(latLng: LatLng, title: String) {
        findViewById<FragmentContainerView>(R.id.map).visibility = View.VISIBLE
        map.clear()
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        map.addMarker(MarkerOptions().position(latLng).title(title))
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

    private fun showNearbyMonuments(userLatLng: LatLng) {
        val monuments = listOf(
            LatLng(userLatLng.latitude + 0.005, userLatLng.longitude),
            LatLng(userLatLng.latitude, userLatLng.longitude + 0.005),
            LatLng(userLatLng.latitude - 0.005, userLatLng.longitude - 0.005)
        )

        for (monument in monuments) {
            map.addMarker(
                MarkerOptions()
                    .position(monument)
                    .title("Zabytek")
                    .snippet("Znajduje się w pobliżu")
            )
        }
    }

    private fun searchNearbyPlaces(location: LatLng, types: List<String>) {
        val radius = 1000 // Radius in meters
        val typeQuery = types.joinToString("|") // "tourist_attraction|restaurant|park"

        val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=${location.latitude},${location.longitude}" +
                "&radius=$radius" +
                "&type=$typeQuery" +
                "&key=API_KEY"

        val queue = Volley.newRequestQueue(this)
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                val results = response.getJSONArray("results")
                for (i in 0 until results.length()) {
                    val place = results.getJSONObject(i)
                    val name = place.getString("name")
                    val lat = place.getJSONObject("geometry").getJSONObject("location").getDouble("lat")
                    val lng = place.getJSONObject("geometry").getJSONObject("location").getDouble("lng")
                    val latLng = LatLng(lat, lng)

                    val typesArray = place.optJSONArray("types") ?: continue
                    val placeType = when {
                        typesArray.toString().contains("tourist_attraction") -> "Zabytek"
                        typesArray.toString().contains("restaurant") -> "Restauracja"
                        typesArray.toString().contains("park") -> "Park"
                        else -> "Inne"
                    }

                    if (placeType in listOf("Zabytek", "Restauracja", "Park")) {
                        map.addMarker(
                            MarkerOptions()
                                .position(latLng)
                                .title(name)
                                .snippet(placeType)
                        )
                    }
                }
            },
            { error ->
                Toast.makeText(this, "Błąd podczas pobierania danych: $error", Toast.LENGTH_SHORT).show()
            }
        )

        queue.add(jsonObjectRequest)
    }

    private fun showNearbyPlaces(userLatLng: LatLng) {
        searchNearbyPlaces(userLatLng, listOf("tourist_attraction", "restaurant", "park")) //zabytki, restauracje, parki
    }


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