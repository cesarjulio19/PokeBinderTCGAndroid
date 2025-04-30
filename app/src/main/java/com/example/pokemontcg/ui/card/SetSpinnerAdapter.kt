package com.example.pokemontcg.ui.card

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.pokemontcg.local.entity.SetEntity

class SetSpinnerAdapter(
    context: Context,
    private var sets: List<SetEntity>
) : ArrayAdapter<SetEntity>(context, android.R.layout.simple_spinner_item, sets) {
    fun updateItems(newItems: List<SetEntity>) {
        sets = newItems
        clear()
        addAll(newItems)
        notifyDataSetChanged()
    }
    init {
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }

    override fun getItem(position: Int): SetEntity? = sets[position]

    override fun getCount(): Int = sets.size

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return super.getView(position, convertView, parent).apply {
            (this as TextView).text = sets[position].name
        }
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return super.getDropDownView(position, convertView, parent).apply {
            (this as TextView).text = sets[position].name
        }
    }
}