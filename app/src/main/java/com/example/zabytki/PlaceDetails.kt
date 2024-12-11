package com.example.zabytki

import android.util.Log
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class PlaceDetails(
    @SerializedName("name") val name: String,
    @SerializedName("rating") val rating: Float,
    @SerializedName("photos") val photos: List<Photo>?
)

data class Photo(
    @SerializedName("photo_reference") val photoReference: String
)

data class PlaceDetailsResponse(
    @SerializedName("result") val result: PlaceDetails
)

interface GooglePlaceDetailsApi {
    @GET("details/json")
    fun getPlaceDetails(
        @Query("place_id") placeId: String,
        @Query("key") apiKey: String
    ): Call<PlaceDetailsResponse>
}

fun fetchPlaceDetails(placeId: String, apiKey: String, onResult: (String, Float, String?) -> Unit) {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://maps.googleapis.com/maps/api/place/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api = retrofit.create(GooglePlaceDetailsApi::class.java)
    val call = api.getPlaceDetails(placeId, apiKey)

    call.enqueue(object : retrofit2.Callback<PlaceDetailsResponse> {
        override fun onResponse(
            call: Call<PlaceDetailsResponse>,
            response: retrofit2.Response<PlaceDetailsResponse>
        ) {
            val result = response.body()?.result
            val photoReference = result?.photos?.firstOrNull()?.photoReference
            onResult(result?.name ?: "", result?.rating ?: 0f, photoReference)
        }

        override fun onFailure(call: Call<PlaceDetailsResponse>, t: Throwable) {
            Log.e("PlaceDetailsError", "Error fetching place details ${t.message}")
            onResult("", 0f, null)
        }
    })
}
