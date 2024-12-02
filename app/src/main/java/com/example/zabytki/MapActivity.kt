package com.example.zabytki

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // Przyciski i klient lokalizacji
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        findViewById<ImageView>(R.id.closeMapButton).setOnClickListener { finish() }

        // Inicjalizacja mapy
        val mapFragment = supportFragmentManager.findFragmentById(R.id.fullscreenMap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        checkLocationPermissionAndShowMap()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true
    }

    private fun checkLocationPermissionAndShowMap() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_REQUEST_CODE
            )
        } else {
            showUserLocationAndNearbyPlaces()
        }
    }

    private fun showUserLocationAndNearbyPlaces() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val userLatLng = LatLng(it.latitude, it.longitude)
                    map.isMyLocationEnabled = true
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))

                    map.addMarker(MarkerOptions().position(userLatLng).title("Twoja lokalizacja"))

                    map.addCircle(
                        CircleOptions()
                            .center(userLatLng)
                            .radius(1000.0) // 1 km
                            .strokeColor(0xFF6200EE.toInt()) // Fioletowy
                            .fillColor(0x226200EE.toInt()) // Półprzezroczysty fioletowy
                            .strokeWidth(4f)
                    )

                    showNearbyMonuments(userLatLng)
                } ?: run {
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
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showUserLocationAndNearbyPlaces()
            } else {
                Toast.makeText(this, "Brak dostępu do lokalizacji", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
