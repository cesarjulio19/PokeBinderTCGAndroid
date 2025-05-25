package com.example.pokemontcg.ui.card

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.pokemontcg.local.entity.SetEntity

class SetSpinnerAdapter(
    context: Context
) : ArrayAdapter<SetEntity>(context, android.R.layout.simple_spinner_item, ArrayList()) {

    init {
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }

    /**
     * Actualiza todo el contenido del spinner.
     */
    fun updateItems(newItems: List<SetEntity>) {
        clear()
        addAll(newItems)
        notifyDataSetChanged()
    }

    //  Pintamos la vista “cerrada” (la que se ve cuando no has desplegado nada)
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val v = super.getView(position, convertView, parent)
        (v as TextView).text = getItem(position)?.name ?: ""
        return v
    }

    //  Pintamos cada fila del desplegable
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val v = super.getDropDownView(position, convertView, parent)
        (v as TextView).text = getItem(position)?.name ?: ""
        return v
    }
}