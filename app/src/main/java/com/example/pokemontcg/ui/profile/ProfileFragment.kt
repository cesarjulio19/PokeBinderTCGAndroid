package com.example.pokemontcg.ui.profile

import android.Manifest
import android.content.ContentResolver
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.pokemontcg.R
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment: Fragment(R.layout.fragment_profile) {

    @Inject
    lateinit var prefs: SharedPreferences

    private val viewModel: ProfileViewModel by viewModels()
    private var selectedImageUri: Uri? = null
    private var dialogImageView: ImageView? = null


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

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        super.onViewCreated(v, savedInstanceState)
        // lanza la carga desde la ViewModel
        val userId = prefs.getInt("userId", -1)
        if (userId != -1) {
            viewModel.loadProfile(userId)
        }
        // Referencias a vistas
        val iv = v.findViewById<ImageView>(R.id.iv_profile_photo)
        dialogImageView = iv
        val etUser = v.findViewById<EditText>(R.id.et_username)
        v.findViewById<Button>(R.id.btn_pick_image)
            .setOnClickListener { pickImage.launch("image/*") }
        v.findViewById<Button>(R.id.btn_take_photo)
            .setOnClickListener {
                //  Comprueba permiso
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    launchCamera()
                } else {
                    requestCameraPermission.launch(Manifest.permission.CAMERA)
                }}
        v.findViewById<Button>(R.id.btn_save_profile)
            .setOnClickListener {
                val name = etUser.text.toString().trim()
                val part = selectedImageUri?.let(::uriToImagePart)
                viewModel.updateProfile(userId, name, part)
            }

        // Observador de UIState -> precarga username e imagen
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.person.collect { dto ->
                dto?.let {
                    etUser.setText(it.username)
                    // Glide cargará la URL que venga en PersonDto.imageUrl
                    Glide.with(this@ProfileFragment)
                        .load(it.imageUrl)
                        .into(iv)
                }
            }
        }

        // Observador de eventos para Snackbar
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.uiEvents.collect { event ->
                when(event) {
                    is ProfileViewModel.UiEvent.ShowMessage ->
                        Snackbar.make(requireView(), getString(event.resId), Snackbar.LENGTH_SHORT)
                            .show()
                }
            }
        }

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