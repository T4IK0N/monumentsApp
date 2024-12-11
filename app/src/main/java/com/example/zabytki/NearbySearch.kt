package com.example.zabytki

import android.util.Log
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class Place(
    @SerializedName("place_id") val placeId: String
)

data class NearbySearchResponse(
    @SerializedName("results") val results: List<Place>
)

interface GooglePlacesApi {
    @GET("nearbysearch/json")
    fun getNearbyPlaces(
        @Query("location") location: String,
        @Query("radius") radius: Int,
        @Query("type") type: String,
        @Query("key") apiKey: String
    ): Call<NearbySearchResponse>
}

fun fetchNearbyPlaces(lat: Double, lng: Double, apiKey: String, onResult: (List<String>) -> Unit) {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://maps.googleapis.com/maps/api/place/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api = retrofit.create(GooglePlacesApi::class.java)
    val call = api.getNearbyPlaces("$lat,$lng", 1500, "restaurant|tourist_attraction|park", apiKey)

    call.enqueue(object : retrofit2.Callback<NearbySearchResponse> {
        override fun onResponse(
            call: Call<NearbySearchResponse>,
            response: retrofit2.Response<NearbySearchResponse>
        ) {
            val placeIds = response.body()?.results?.take(10)?.map { it.placeId } ?: emptyList()
            onResult(placeIds)
        }

        override fun onFailure(call: Call<NearbySearchResponse>, t: Throwable) {
            Log.e("NearbySearchFailure", "Error fetching nearby places: ${t.message}")
            onResult(emptyList())
        }
    })
}

