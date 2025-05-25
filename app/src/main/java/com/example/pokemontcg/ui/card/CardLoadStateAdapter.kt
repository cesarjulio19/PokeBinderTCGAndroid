package com.example.pokemontcg.ui.card

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pokemontcg.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator

/**
 * Adapter que enlaza el LoadState (cargando / error) al RecyclerView como footer.
 */
class CardLoadStateAdapter(
    private val retry: () -> Unit
) : LoadStateAdapter<CardLoadStateAdapter.LoadStateVH>() {

    inner class LoadStateVH(view: View) : RecyclerView.ViewHolder(view) {
        private val progress = view.findViewById<CircularProgressIndicator>(R.id.load_state_progress)
        private val btnRetry = view.findViewById<MaterialButton>(R.id.load_state_retry)

        init {
            btnRetry.setOnClickListener { retry() }
        }

        fun bind(loadState: LoadState) {
            progress.isVisible = loadState is LoadState.Loading
            btnRetry.isVisible = loadState is LoadState.Error
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): LoadStateVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_load_state_footer, parent, false)
        return LoadStateVH(view)
    }

    override fun onBindViewHolder(holder: LoadStateVH, loadState: LoadState) {
        holder.bind(loadState)
    }
}
