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

class PopularAttractionAdapter(
    private val context: Context,
    private var attractions: List<PopularAttraction>
) : RecyclerView.Adapter<PopularAttractionAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
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

        Glide.with(context)
            .load(attraction.imageUrl)
            .placeholder(R.color.lighter_gray)
            .into(holder.imageView)
    }

    override fun getItemCount() = attractions.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateAttractions(newAttractions: List<PopularAttraction>) {
        attractions = newAttractions
        notifyDataSetChanged()
    }
}
