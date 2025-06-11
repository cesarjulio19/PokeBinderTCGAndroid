package com.example.pokemontcg.ui.card

import android.Manifest
import android.app.AlertDialog
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.pokemontcg.R
import com.example.pokemontcg.api.request.card.CardCreateData
import com.example.pokemontcg.api.request.card.CardCreateRequest
import com.example.pokemontcg.api.request.card.CardUpdateData
import com.example.pokemontcg.api.request.card.CardUpdateRequest
import com.example.pokemontcg.dto.CardDto
import com.example.pokemontcg.local.entity.SetEntity
import com.example.pokemontcg.ui.set.SetViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import java.io.File
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody

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

    private var dialogImageView: ImageView? = null

    //Guarda la Uri de la imagen seleccionada
    private var selectedImageUri: Uri? = null

    // Registra los lanzadores en el cuerpo de la clase
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { loadImagePreview(it) }
    }

    // Define un launcher para pedir el permiso
    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // si te autorizaron, relanza la acción de cámara
            launchCamera()
        } else {
            Toast.makeText(requireContext(),
                getString(R.string.Sin_permiso_de_camara),
                Toast.LENGTH_SHORT
            ).show()
        }
    }



    // Lanzador para capturar foto en un fichero
    private val takePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && selectedImageUri != null) {
            loadImagePreview(selectedImageUri!!)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_cards, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Título
        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.cartas)

        // Inicializa el PagingDataAdapter (hereda de PagingDataAdapter<CardDto, VH>)
        cardAdapter = CardAdapter(
            onEdit = { card -> showCardDialog(isEditing = true, existing = card) },
            onDelete = { card ->
                AlertDialog.Builder(requireContext())
                    .setTitle( getString(R.string.Eliminar_carta) +
                    " ${card.id}")
                    .setMessage( getString(R.string.Seguro_que_deseas_eliminar_carta) + " «${card.name}»?")
                    .setPositiveButton(getString(R.string.Sí)) { _, _ ->
                        cardViewModel.deleteCard(card.id)
                    }
                    .setNegativeButton(getString(R.string.cancelar), null)
                    .show()
            }
        )

        //  Configura el RecyclerView con footer de carga/reintento
        recyclerView = view.findViewById(R.id.recycler_cards)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = cardAdapter.withLoadStateFooter(
            footer = CardLoadStateAdapter { cardAdapter.retry() }
        )

        //  Spinner de sets
        spinner = view.findViewById(R.id.spinner_sets)
        setAdapter = SetSpinnerAdapter(requireContext())
        spinner.adapter = setAdapter
        // obtenemos los Sets desde Strapi hacia Room
        setViewModel.refresh()

        // Observa la lista de sets desde el ViewModel
        setViewModel.sets
            .onEach { list ->
                sets = list.toList() // mantiene copia mutable
                setAdapter.updateItems(sets)
                if (sets.isNotEmpty()) spinner.setSelection(0, false)
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        // Pre-carga inicial: carga cartas del primer set
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            setViewModel.sets.firstOrNull()?.firstOrNull()?.let { firstSet ->
                selectedSetId = firstSet.id
                cardViewModel.fetchCardsBySet(firstSet.id)
                cardViewModel.onSetSelected(firstSet.id)
            }
        }

        lifecycleScope.launch {
            cardViewModel.pagedCards
                .collectLatest { pagingData ->
                    cardAdapter.submitData(pagingData)
                }
        }

        // Cuando el usuario cambia de set, vuelve a cargar paging source
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                val setId = sets[pos].id
                selectedSetId = setId
                cardViewModel.fetchCardsBySet(setId)
                cardViewModel.onSetSelected(setId)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }


        // Botones de set (crear, editar, borrar)
        view.findViewById<Button>(R.id.btn_new_set)
            .setOnClickListener { showSetDialog(isEditing = false) }

        view.findViewById<Button>(R.id.btn_edit_set)
            .setOnClickListener {
                selectedSetId?.let { id ->
                    // obtenemos el nombre del set del array local 'sets'
                    val name = sets.first { it.id == id }.name
                    showSetDialog(isEditing = true, setId = id, setName = name)
                }
            }

        view.findViewById<Button>(R.id.btn_delete_set)
            .setOnClickListener {
                selectedSetId?.let { id ->
                    AlertDialog.Builder(requireContext())
                        .setTitle(getString(R.string.Eliminar_set)+" $id")
                        .setMessage(getString(R.string.Seguro_que_deseas_eliminar_set))
                        .setPositiveButton(getString(R.string.Sí)) { _, _ ->
                            setViewModel.deleteSet(id)
                        }
                        .setNegativeButton(getString(R.string.cancelar), null)
                        .show()
                }
            }

        // Botón para nueva carta
        view.findViewById<Button>(R.id.btn_new_card)
            .setOnClickListener { showCardDialog(isEditing = false) }


        // Cuando recibas el resultado de creación/edición:
        cardViewModel.isSuccess
            .onEach { success ->
                if (success) {
                    // recarga la fuente paginada desde página 1
                    cardAdapter.refresh()
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            cardViewModel.uiEvents.collect { event ->
                when(event) {
                    is CardViewModel.CardUiEvent.ShowMessage ->{
                        val message = requireContext().getString(event.resId)
                        Snackbar
                            .make(requireView(), message, Snackbar.LENGTH_SHORT)
                            .show()

                    }
                }
            }
        }
    }






    private fun showSetDialog(isEditing: Boolean, setId: Int = 0, setName: String = "") {
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.Nombre_del_set)
            setText(if (isEditing) setName else "")
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (isEditing) getString(R.string.editar_set2) else getString(R.string.nuevo_set2))
            .setView(input)
            .setPositiveButton(getString(R.string.guardar)) { _, _ ->
                val name = input.text.toString()
                if (isEditing) {
                    setViewModel.updateSet(setId, name)
                } else {
                    setViewModel.createSet(name)
                }
            }
            .setNegativeButton(getString(R.string.cancelar), null)
            .show()
    }

    private fun showCardDialog(isEditing: Boolean, existing: CardDto? = null) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_card_form, null)
        val btnGallery = dialogView.findViewById<Button>(R.id.btn_pick_image)
        val btnCamera  = dialogView.findViewById<Button>(R.id.btn_take_photo)
        val imgPreview = dialogView.findViewById<ImageView>(R.id.iv_image_preview)
        dialogImageView = imgPreview
        selectedImageUri = null
        imgPreview.visibility = View.GONE

        val etName         = dialogView.findViewById<EditText>(R.id.et_name)
        val etNumber       = dialogView.findViewById<EditText>(R.id.et_number)
        val spinnerType    = dialogView.findViewById<Spinner>(R.id.spinner_type)
        val spinnerRarity  = dialogView.findViewById<Spinner>(R.id.spinner_rarity)
        val spinnerSuper   = dialogView.findViewById<Spinner>(R.id.spinner_superType)

        btnGallery.setOnClickListener {
            pickImage.launch("image/*")
        }

        btnCamera.setOnClickListener {
            //  Comprueba permiso
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                launchCamera()
            } else {
                requestCameraPermission.launch(Manifest.permission.CAMERA)
            }
        }

        //Configurar adapters de spinner
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

        //Si es edición, precarga valores
        if (isEditing && existing != null) {
            val imageUrl = existing.image ?: existing.illustration
            if (imageUrl != null) {
                imgPreview.visibility = View.VISIBLE
                Glide.with(this).load(imageUrl).into(imgPreview)
                // guardamos esa Uri como “seleccionada”, para que no intente subir null
                selectedImageUri = Uri.parse(imageUrl)
            }
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
        }else{

        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (isEditing) getString(R.string.editar_carta2) else getString(R.string.nueva_carta2))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.guardar)) { _, _ ->
                val name  = etName.text.toString().trim()
                val num   = etNumber.text.toString().toIntOrNull() ?: 0
                val type  = spinnerType.selectedItem as String
                val rar   = spinnerRarity.selectedItem as String
                val sup   = spinnerSuper.selectedItem as String
                val setId = selectedSetId ?: return@setPositiveButton
                val localImagePart = selectedImageUri
                    // sólo si el URI es local
                    ?.takeIf { uri ->
                        val scheme = uri.scheme?.lowercase()
                        scheme == ContentResolver.SCHEME_CONTENT || scheme == ContentResolver.SCHEME_FILE
                    }
                    ?.let { uri ->
                        try {
                            uriToImagePart(uri)
                        } catch (e: Exception) {
                            Log.w("CardsFragment", "No pude empaquetar la imagen: $uri", e)
                            null
                        }
                    }

                if (isEditing && existing != null) {
                    // Prepara request de actualización
                    val req = CardUpdateRequest(
                        data = CardUpdateData(
                            name = name,
                            number = num,
                            type = type,
                            rarity = rar,
                            superType = sup,
                            set = setId,
                            image = null
                        )
                    )
                    // Convertimos la URI a MultipartBody.Part si existe


                    cardViewModel.updateCard(existing.id, req, localImagePart)

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
                            set = setId,

                        )
                    )
                    // Convertimos la URI a MultipartBody.Part si existe

                    cardViewModel.createCard(req, localImagePart)
                }

            }
            .setNegativeButton(getString(R.string.cancelar), null)
            .show()
    }

    private fun loadImagePreview(uri: Uri) {
        selectedImageUri = uri
        dialogImageView?.apply {
            visibility = View.VISIBLE
            Glide.with(this)
                .load(uri)
                .into(this)
        }

    }


    /**
     * Lee el contenido del URI y crea un MultipartBody.Part con clave "files"
     */
    private fun uriToImagePart(uri: Uri): MultipartBody.Part? {
        // sólo content/file
        if (uri.scheme !in listOf(ContentResolver.SCHEME_CONTENT, ContentResolver.SCHEME_FILE)) return null

        val cr = requireContext().contentResolver
        val mime = cr.getType(uri) ?: return null
        val input = cr.openInputStream(uri) ?: return null

        val file = File(requireContext().cacheDir, "upload_${System.currentTimeMillis()}")
        file.outputStream().use { out -> input.copyTo(out) }

        val requestBody = file.asRequestBody(mime.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData("files", file.name, requestBody)
    }

    /** Se encarga de crear el fichero, URI, dar permisos y lanzar cámara */
    private fun launchCamera() {
        val file = File(requireContext().cacheDir,
            "tmp_image_${System.currentTimeMillis()}.jpg")
        selectedImageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )

        // Dale permiso a cualquier app de cámara (o al paquete específico) para escribir ahí:
        requireContext().grantUriPermission(
            "com.android.camera2",             // o null para todas
            selectedImageUri,
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )

        // Lanza la cámara; rellenará nuestro currentPhotoUri
        takePhotoLauncher.launch(selectedImageUri)
    }
}