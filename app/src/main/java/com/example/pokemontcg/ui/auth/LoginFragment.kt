package com.example.pokemontcg.ui.auth

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
import com.example.pokemontcg.api.request.auth.LoginRequest
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var etIdentifier: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var btnToRegister: TextView
    private lateinit var progress: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_login, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        etIdentifier = view.findViewById(R.id.et_identifier)
        etPassword   = view.findViewById(R.id.et_password)
        btnLogin     = view.findViewById(R.id.btn_login)
        btnToRegister= view.findViewById(R.id.tv_to_register)
        progress     = view.findViewById(R.id.login_progress)

        // Navegar a registro
        btnToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        // Pulsar login
        btnLogin.setOnClickListener {
            val id = etIdentifier.text.toString().trim()
            val pw = etPassword.text.toString()
            authViewModel.login(LoginRequest(identifier = id, password = pw))
        }

        // Observa estado
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            authViewModel.authState.collect { state ->
                when (state) {
                    is AuthState.Loading -> {
                        progress.visibility = View.VISIBLE
                        btnLogin.isEnabled = false
                    }
                    is AuthState.Success -> {
                        progress.visibility = View.GONE
                        //Forzar refresco de menú en MainActivity
                        requireActivity().invalidateOptionsMenu()
                        //Navegar a CardsFragment
                        findNavController().navigate(R.id.action_login_to_cards)
                    }
                    is AuthState.Error -> {
                        progress.visibility = View.GONE
                        btnLogin.isEnabled = true
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                    is AuthState.Idle -> {
                        // estado inicial, no hacemos nada
                        progress.visibility = View.GONE
                        btnLogin.isEnabled = true
                    }
                }
            }
        }
    }
}