package com.example.pokemontcg.ui.card


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.pokemontcg.R
import com.example.pokemontcg.dto.CardDto

class CardAdapter(
    val onEdit: (CardDto) -> Unit,
    val onDelete: (CardDto) -> Unit
) : PagingDataAdapter<CardDto, CardAdapter.CardViewHolder>(CardDiff) {

    object CardDiff : DiffUtil.ItemCallback<CardDto>() {
        override fun areItemsTheSame(oldItem: CardDto, newItem: CardDto) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: CardDto, newItem: CardDto) = oldItem == newItem
    }

    inner class CardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val img = view.findViewById<ImageView>(R.id.card_image)
        val editBtn = view.findViewById<ImageButton>(R.id.btn_edit)
        val deleteBtn = view.findViewById<ImageButton>(R.id.btn_delete)

        fun bind(card: CardDto) {
            val glide = Glide.with(img.context)

            if (!card.illustration.isNullOrEmpty() && card.image.isNullOrEmpty()) {

                glide
                    .load(card.illustration)
                    .error(R.drawable.pokeball)
                    .into(img)
            } else if(!card.image.isNullOrEmpty()){

                glide
                    .load(card.image)
                    .error(R.drawable.pokeball)
                    .into(img)
            }else if(card.image.isNullOrEmpty() && card.illustration.isNullOrEmpty()){
                glide
                    .load(R.drawable.pokeball)
                    .into(img)

            }

            editBtn.setOnClickListener { onEdit(card) }
            deleteBtn.setOnClickListener { onDelete(card) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_card, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }
}