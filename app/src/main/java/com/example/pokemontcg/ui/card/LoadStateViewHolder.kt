package com.example.pokemontcg.ui.card

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pokemontcg.R

/**
 * ViewHolder que muestra un ProgressBar mientras carga,
 * o un botón de Retry si hubo error.
 */
class LoadStateViewHolder(
    parent: ViewGroup,
    retry: () -> Unit
) : RecyclerView.ViewHolder(
    LayoutInflater.from(parent.context)
        .inflate(R.layout.item_load_state_footer, parent, false)
) {
    private val progressBar: ProgressBar = itemView.findViewById(R.id.load_state_progress)
    private val retryButton: Button   = itemView.findViewById(R.id.load_state_retry)

    init {
        retryButton.setOnClickListener { retry() }
    }

    fun bind(loadState: LoadState) {
        when (loadState) {
            is LoadState.Loading -> {
                progressBar.visibility = View.VISIBLE
                retryButton.visibility   = View.GONE
            }
            is LoadState.Error   -> {
                progressBar.visibility = View.GONE
                retryButton.visibility   = View.VISIBLE
            }
            else                 -> {
                progressBar.visibility = View.GONE
                retryButton.visibility   = View.GONE
            }
        }
    }
}
