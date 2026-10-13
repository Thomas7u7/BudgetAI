package com.example.budgetai

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.budgetai.data.model.AuthErrorType
import com.example.budgetai.data.model.AuthState
import com.example.budgetai.ui.auth.AuthViewModel
import com.google.android.material.snackbar.Snackbar

class LoginActivity : AppCompatActivity() {

    private val viewModel: AuthViewModel by viewModels()

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var tvEmailError: TextView
    private lateinit var tvPasswordError: TextView
    private lateinit var btnLogin: Button
    private lateinit var loadingOverlay: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        tvEmailError = findViewById(R.id.tvEmailError)
        tvPasswordError = findViewById(R.id.tvPasswordError)
        btnLogin = findViewById(R.id.btnLogin)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        val btnGoogle = findViewById<Button>(R.id.btnGoogle)
        val btnApple = findViewById<Button>(R.id.btnApple)
        val tvCreateAccount = findViewById<TextView>(R.id.tvCreateAccount)

        // Resaltado dinámico del campo con foco
        etEmail.setOnFocusChangeListener { _, tieneFoco ->
            etEmail.setBackgroundResource(if (tieneFoco) R.drawable.bg_field_focused else R.drawable.bg_field)
        }

        etPassword.setOnFocusChangeListener { _, tieneFoco ->
            etPassword.setBackgroundResource(if (tieneFoco) R.drawable.bg_field_focused else R.drawable.bg_field)
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            viewModel.iniciarSesion(email, password)
        }

        tvForgotPassword.setOnClickListener {
            val intent = Intent(this, ResetPasswordActivity::class.java)
            intent.putExtra("email", etEmail.text.toString().trim())
            startActivity(intent)
        }

        btnGoogle.setOnClickListener { showError("Inicio de sesión con Google (próximamente)") }
        btnApple.setOnClickListener { showError("Inicio de sesión con Apple (próximamente)") }

        tvCreateAccount.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        limpiarErroresAlEscribir()
        observarEstado()
    }

    private fun observarEstado() {
        viewModel.authState.observe(this) { estado ->
            when (estado) {
                is AuthState.Loading -> {
                    limpiarErrores()
                    setLoadingState(true)
                }

                is AuthState.Success -> {
                    setLoadingState(false)
                    val intent = Intent(this, HomeActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                    finish()
                }

                is AuthState.Error -> {
                    setLoadingState(false)
                    when (estado.tipo) {
                        AuthErrorType.CAMPOS_VACIOS ->
                            showError("Completa todos los campos")

                        AuthErrorType.EMAIL_INVALIDO ->
                            mostrarErrorEnCampo(etEmail, tvEmailError, "Correo electrónico inválido")

                        AuthErrorType.CREDENCIALES_INCORRECTAS -> {
                            mostrarErrorEnCampo(etEmail, tvEmailError, null)
                            mostrarErrorEnCampo(etPassword, tvPasswordError, "Correo o contraseña incorrectos")
                        }

                        else -> showError(estado.mensaje)
                    }
                }

                else -> Unit
            }
        }
    }

    private fun mostrarErrorEnCampo(campo: EditText, tvError: TextView, mensaje: String?) {
        campo.setBackgroundResource(R.drawable.bg_field_error)
        if (mensaje != null) {
            tvError.text = mensaje
            tvError.visibility = View.VISIBLE
        }
    }

    private fun limpiarErrorEnCampo(campo: EditText, tvError: TextView) {
        campo.setBackgroundResource(R.drawable.bg_field)
        tvError.visibility = View.GONE
    }

    private fun limpiarErrores() {
        limpiarErrorEnCampo(etEmail, tvEmailError)
        limpiarErrorEnCampo(etPassword, tvPasswordError)
    }

    private fun limpiarErroresAlEscribir() {
        etEmail.addTextChangedListener(simpleWatcher { limpiarErrorEnCampo(etEmail, tvEmailError) })
        etPassword.addTextChangedListener(simpleWatcher { limpiarErrorEnCampo(etPassword, tvPasswordError) })
    }

    private fun simpleWatcher(onChanged: () -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = onChanged()
        override fun afterTextChanged(s: Editable?) = Unit
    }

    private fun setLoadingState(isLoading: Boolean) {
        loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
    }
}