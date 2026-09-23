package com.example.budgetai

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.budgetai.data.model.AuthErrorType
import com.example.budgetai.data.model.AuthState
import com.example.budgetai.ui.auth.AuthViewModel
import com.google.android.material.snackbar.Snackbar

class ResetPasswordActivity : AppCompatActivity() {

    private val viewModel: AuthViewModel by viewModels()

    private lateinit var etEmail: EditText
    private lateinit var tvEmailError: TextView
    private lateinit var btnSend: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvBackToLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        etEmail = findViewById(R.id.etEmail)
        tvEmailError = findViewById(R.id.tvEmailError)
        btnSend = findViewById(R.id.btnSend)
        progressBar = findViewById(R.id.progressBar)
        tvBackToLogin = findViewById(R.id.tvBackToLogin)

        intent.getStringExtra("email")?.let {
            etEmail.setText(it)
        }

        btnSend.setOnClickListener {
            val email = etEmail.text.toString().trim()
            viewModel.recuperarContrasena(email)
        }

        tvBackToLogin.setOnClickListener { finish() }

        limpiarErrorAlEscribir()
        observarEstado()
    }

    private fun observarEstado() {
        viewModel.authState.observe(this) { estado ->
            when (estado) {
                is AuthState.Loading -> {
                    limpiarError()
                    setLoadingState(true)
                }

                is AuthState.Success -> {
                    setLoadingState(false)
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "Si el correo está registrado, te enviamos un enlace",
                        Snackbar.LENGTH_LONG
                    ).show()


                    tvBackToLogin.postDelayed({
                        val intent = Intent(this, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(intent)
                        finish()
                    }, 1500)
                }

                is AuthState.Error -> {
                    setLoadingState(false)
                    when (estado.tipo) {
                        AuthErrorType.CAMPOS_VACIOS ->
                            mostrarErrorEnCampo("Ingresa tu correo electrónico")

                        AuthErrorType.CREDENCIALES_INCORRECTAS ->
                            mostrarErrorEnCampo("No existe una cuenta con ese correo")

                        AuthErrorType.ERROR_RED ->
                            showError("Sin conexión. Intenta de nuevo.")

                        else -> showError(estado.mensaje)
                    }
                }

                else -> Unit // Idle, RegistroExitoso -> no aplica aquí
            }
        }
    }

    private fun mostrarErrorEnCampo(mensaje: String) {
        etEmail.setBackgroundResource(R.drawable.bg_field_error)
        tvEmailError.text = mensaje
        tvEmailError.visibility = View.VISIBLE
    }

    private fun limpiarError() {
        etEmail.setBackgroundResource(R.drawable.bg_field)
        tvEmailError.visibility = View.GONE
    }

    private fun limpiarErrorAlEscribir() {
        etEmail.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = limpiarError()
            override fun afterTextChanged(s: android.text.Editable?) = Unit
        })
    }

    private fun setLoadingState(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnSend.isEnabled = !isLoading
        btnSend.text = if (isLoading) "" else "Enviar enlace"
    }

    private fun showError(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
    }
}