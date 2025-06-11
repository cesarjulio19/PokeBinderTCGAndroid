package com.example.pokemontcg.ui.pack

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pokemontcg.R
import com.example.pokemontcg.dto.CardDto
import com.example.pokemontcg.local.entity.SetEntity
import com.example.pokemontcg.ui.card.CardViewModel
import com.example.pokemontcg.ui.set.SetViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OpenPackFragment : Fragment() {

    private val setViewModel: SetViewModel by viewModels()
    private val cardViewModel: CardViewModel by viewModels()

    private lateinit var spinner: Spinner
    private lateinit var btnOpen: Button
    private lateinit var packPreview: ImageView

    private var sets: List<SetEntity> = emptyList()
    private var selectedSetId: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.fragment_open_pack, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        spinner      = view.findViewById(R.id.spinner_sets)
        btnOpen      = view.findViewById(R.id.btn_open_pack)
        packPreview  = view.findViewById(R.id.iv_pack_preview)

        // Carga sets en el spinner
        setViewModel.sets.onEach { list ->
            sets = list
            spinner.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                sets.map { it.name }
            ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        }.launchIn(viewLifecycleOwner.lifecycleScope)

        // Cada vez que cambie el spinner:
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, pos: Int, id: Long
            ) {
                selectedSetId = sets[pos].id
                // Consultamos cuántas cartas hay en Room (suspend)
                viewLifecycleOwner.lifecycleScope.launch {
                    val count = cardViewModel.getCountForSet(selectedSetId)
                    btnOpen.isEnabled = count >= 5
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Al pulsar "Abrir sobre":
        btnOpen.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                // Lee todas las cartas de ese set
                val all = cardViewModel.getCardsOnceForSet(selectedSetId)
                // Elige 5 al azar
                val pick = all.shuffled().take(5)
                showPackDialog(pick)
            }
        }
    }

    //Muestra el AlertDialog de las cartas del sobre
    private fun showPackDialog(cards: List<CardDto>) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_pack, null)
        val rv = dialogView.findViewById<RecyclerView>(R.id.rv_pack_cards).apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = PackAdapter().also { it.submitList(cards) }
        }

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton(getString(R.string.cerrar), null)
            .show()
    }
}