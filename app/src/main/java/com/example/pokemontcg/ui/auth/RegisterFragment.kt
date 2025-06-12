package com.example.pokemontcg.ui.auth

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.pokemontcg.R
import com.example.pokemontcg.api.request.auth.RegisterRequest
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var etEmail: TextInputEditText
    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirm: TextInputEditText
    private lateinit var btnRegister: Button
    private lateinit var progress: ProgressBar
    private lateinit var tvToLogin: TextView
    @Inject
    lateinit var prefs: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_register, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        etEmail     = view.findViewById(R.id.et_email)
        etUsername  = view.findViewById(R.id.et_username)
        etPassword  = view.findViewById(R.id.et_password)
        etConfirm   = view.findViewById(R.id.et_confirm)
        btnRegister = view.findViewById(R.id.btn_register)
        progress    = view.findViewById(R.id.register_progress)
        tvToLogin   = view.findViewById(R.id.tv_to_login)

        //Navegar a login
        tvToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }

        // Pulsar registrar
        btnRegister.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val user  = etUsername.text.toString().trim()
            val pw    = etPassword.text.toString()
            val pw2   = etConfirm.text.toString()
            if (pw != pw2) {
                etConfirm.error = "No coincide"
                return@setOnClickListener
            }
            authViewModel.register(
                RegisterRequest(
                email = email,
                username = user,
                password = pw
            )
            )
        }

        //Observa estado
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            authViewModel.authState.collect { state ->
                when (state) {
                    is AuthState.Loading -> {
                        progress.visibility = View.VISIBLE
                        btnRegister.isEnabled = false
                    }
                    is AuthState.Success -> {
                        progress.visibility = View.GONE
                        btnRegister.isEnabled = true

                        // detecta si no hay JWT => registro OFFLINE
                        val jwt = prefs.getString("jwt", "")
                        if (jwt.isNullOrBlank()) {
                            Toast
                                .makeText(requireContext(),
                                    "Registrado en modo offline",
                                    Toast.LENGTH_LONG)
                                .show()
                        }

                        // Forzar refresco de menú y navegar
                        requireActivity().invalidateOptionsMenu()
                        findNavController().navigate(R.id.action_register_to_cards)
                    }
                    is AuthState.Error -> {
                        progress.visibility = View.GONE
                        btnRegister.isEnabled = true
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                    is AuthState.Idle -> {
                        progress.visibility = View.GONE
                        btnRegister.isEnabled = true
                    }
                }
            }
        }
    }
}
