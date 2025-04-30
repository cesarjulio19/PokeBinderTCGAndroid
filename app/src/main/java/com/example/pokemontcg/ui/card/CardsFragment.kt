package com.example.pokemontcg.ui.card

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pokemontcg.R
import com.example.pokemontcg.api.request.card.CardCreateData
import com.example.pokemontcg.api.request.card.CardCreateRequest
import com.example.pokemontcg.api.request.card.CardUpdateData
import com.example.pokemontcg.api.request.card.CardUpdateRequest
import com.example.pokemontcg.dto.CardDto
import com.example.pokemontcg.local.entity.SetEntity
import com.example.pokemontcg.ui.set.SetViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CardsFragment : Fragment() {

    private lateinit var spinner: Spinner
    private lateinit var recyclerView: RecyclerView
    private lateinit var setAdapter: SetSpinnerAdapter
    private lateinit var cardAdapter: CardAdapter
    private lateinit var sets: List<SetEntity>
    private var selectedSetId: Int? = null

    private val setViewModel: SetViewModel by viewModels()
    private val cardViewModel: CardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_cards, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        (activity as AppCompatActivity)
            .supportActionBar
            ?.title = "Cartas"


        cardAdapter = CardAdapter(
            onEdit = { card -> showCardDialog(isEditing = true, existing = card) },
            onDelete = { card ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Eliminar carta ${card.id}")
                    .setMessage("¿Seguro que deseas eliminar «${card.name}»?")
                    .setPositiveButton("Sí") { _, _ ->
                        cardViewModel.deleteCard(card.id)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        )
        recyclerView = view.findViewById(R.id.recycler_cards)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = cardAdapter


        Log.i("CardRepo", "CardsFragment onViewCreated")


        spinner = view.findViewById(R.id.spinner_sets)
        setViewModel.refresh()

        setAdapter = SetSpinnerAdapter(
            requireContext(),
            ArrayList<SetEntity>()
        )
        spinner.adapter = setAdapter
        setViewModel.sets
            .onEach { list ->
                sets = list
                setAdapter.updateItems(sets)
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {

            val list = setViewModel.sets.first()
            if (list.isNotEmpty()) {

                spinner.setSelection(0, /* animate = */ false)

                // Guarda el id y lanza el sync diferencial
                val first = list[0]
                selectedSetId = first.id
                cardViewModel.syncCardsBySet(first.id)
            }
        }


        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val set = sets[position]
                selectedSetId = set.id
                Log.i("CardRepo", "Set seleccionado: ${set.name} (id=$selectedSetId)")


                viewLifecycleOwner.lifecycleScope.launch {

                    cardViewModel.syncCardsBySet(set.id)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }


        cardViewModel.filteredCards.onEach { list ->
            Log.i("CardRepo", "filteredCards emitió ${list.size} cartas")
            cardAdapter.submitList(list)
        }.launchIn(viewLifecycleOwner.lifecycleScope)


        view.findViewById<Button>(R.id.btn_new_card).setOnClickListener {
            showCardDialog(isEditing = false)
        }

        // Botones

        view.findViewById<Button>(R.id.btn_new_set).setOnClickListener {
            showSetDialog(isEditing = false)
        }

        view.findViewById<Button>(R.id.btn_edit_set).setOnClickListener {
            selectedSetId?.let {
                val setName = sets.first { s -> s.id == it }.name
                showSetDialog(isEditing = true, setId = it, setName = setName)
            }
        }

        view.findViewById<Button>(R.id.btn_delete_set).setOnClickListener {
            selectedSetId?.let { id ->
                val dialog = AlertDialog.Builder(requireContext())
                    .setTitle("Eliminar set $id")
                    .setMessage("¿Estás seguro de que deseas eliminar este set?")
                    .setPositiveButton("Sí") { _, _ ->
                        setViewModel.deleteSet(id)
                    }
                    .setNegativeButton("Cancelar", null)
                    .create()

                dialog.show()

            }
        }


        cardViewModel.isSuccess
            .onEach { success ->
                if (success && selectedSetId != null) {
                    // recarga sólo de Room las cartas de este set
                    //cardViewModel.fetchCardsBySet(selectedSetId!!)
                    cardViewModel.syncCardsBySet(selectedSetId!!)
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)






    }



    private fun showSetDialog(isEditing: Boolean, setId: Int = 0, setName: String = "") {
        val input = EditText(requireContext()).apply {
            hint = "Nombre del set"
            setText(if (isEditing) setName else "")
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (isEditing) "Editar set" else "Nuevo set")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val name = input.text.toString()
                if (isEditing) {
                    setViewModel.updateSet(setId, name)
                } else {
                    setViewModel.createSet(name)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showCardDialog(isEditing: Boolean, existing: CardDto? = null) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_card_form, null)

        val etName         = dialogView.findViewById<EditText>(R.id.et_name)
        val etNumber       = dialogView.findViewById<EditText>(R.id.et_number)
        val spinnerType    = dialogView.findViewById<Spinner>(R.id.spinner_type)
        val spinnerRarity  = dialogView.findViewById<Spinner>(R.id.spinner_rarity)
        val spinnerSuper   = dialogView.findViewById<Spinner>(R.id.spinner_superType)

        // 1) Configurar adapters de spinner
        ArrayAdapter.createFromResource(requireContext(),
            R.array.card_types, android.R.layout.simple_spinner_item)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            .let { spinnerType.adapter = it }

        ArrayAdapter.createFromResource(requireContext(),
            R.array.card_rarities, android.R.layout.simple_spinner_item)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            .let { spinnerRarity.adapter = it }

        ArrayAdapter.createFromResource(requireContext(),
            R.array.card_supertypes, android.R.layout.simple_spinner_item)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            .let { spinnerSuper.adapter = it }

        // 2) Si es edición, precarga valores
        if (isEditing && existing != null) {
            etName.setText(existing.name)
            etNumber.setText(existing.number.toString())
            spinnerType.setSelection(
                (spinnerType.adapter as ArrayAdapter<String>).getPosition(existing.type)
            )
            spinnerRarity.setSelection(
                (spinnerRarity.adapter as ArrayAdapter<String>).getPosition(existing.rarity)
            )
            spinnerSuper.setSelection(
                (spinnerSuper.adapter as ArrayAdapter<String>).getPosition(existing.superType)
            )
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (isEditing) "Editar Carta" else "Nueva Carta")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val name  = etName.text.toString().trim()
                val num   = etNumber.text.toString().toIntOrNull() ?: 0
                val type  = spinnerType.selectedItem as String
                val rar   = spinnerRarity.selectedItem as String
                val sup   = spinnerSuper.selectedItem as String

                val setId = selectedSetId ?: return@setPositiveButton

                if (isEditing && existing != null) {
                    // Prepara request de actualización
                    val req = CardUpdateRequest(
                        data = CardUpdateData(
                            name = name,
                            number = num,
                            type = type,
                            rarity = rar,
                            superType = sup,
                            set = setId
                        )
                    )
                    cardViewModel.updateCard(existing.id, req)
                } else {
                    // Request de creación
                    val req = CardCreateRequest(
                        data = CardCreateData(
                            name = name,
                            number = num,
                            type = type,
                            image = null,
                            rarity = rar,
                            superType = sup,
                            set = setId
                        )
                    )
                    cardViewModel.createCard(req)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}