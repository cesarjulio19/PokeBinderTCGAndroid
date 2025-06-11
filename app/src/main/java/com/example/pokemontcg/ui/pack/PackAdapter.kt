package com.example.pokemontcg.ui.pack

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.pokemontcg.R
import com.example.pokemontcg.dto.CardDto

class PackAdapter : RecyclerView.Adapter<PackAdapter.VH>() {
    private val items = mutableListOf<CardDto>()
    fun submitList(list: List<CardDto>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pack_card, parent, false)
        return VH(v)
    }
    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }
    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val iv = view.findViewById<ImageView>(R.id.iv_card)
        fun bind(card: CardDto) {
            Glide.with(iv).load(card.image ?: card.illustration)
                .placeholder(R.drawable.pokeball)
                .into(iv)
        }
    }
}