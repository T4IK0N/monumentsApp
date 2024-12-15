package com.example.zabytki

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream


class PopularAttractionAdapter(
    private val context: Context,
    private var attractions: List<PopularAttraction>
) : RecyclerView.Adapter<PopularAttractionAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.attractionImageView)
        val titleView: TextView = view.findViewById(R.id.titleTextView)
        val ratingView: TextView = view.findViewById(R.id.ratingTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_attraction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val attraction = attractions[position]
        holder.titleView.text = attraction.title
        holder.ratingView.text = attraction.rating

//        val imageUrl = attraction.imageUrl
//        Glide.with(context)
//            .load(downloadImage(imageUrl)) // Pobiera obraz jako InputStream
//            .placeholder(R.color.lighter_gray)
//            .into(holder.imageView)

        Glide.with(context).load(attraction.imageUrl).into(holder.imageView);
    }


    override fun getItemCount() = attractions.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateAttractions(newAttractions: List<PopularAttraction>) {
        attractions = newAttractions
        notifyDataSetChanged()
    }
}

fun downloadImage(url: String): InputStream? {
    return try {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            response.body?.byteStream()
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

