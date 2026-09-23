package com.example.budgetai

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.text.method.TransformationMethod
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.budgetai.data.model.AuthErrorType
import com.example.budgetai.data.model.AuthState
import com.example.budgetai.ui.auth.AuthViewModel
import com.google.android.material.snackbar.Snackbar

class RegisterActivity : AppCompatActivity() {

    private val viewModel: AuthViewModel by viewModels()
    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var tvFullNameError: TextView
    private lateinit var tvEmailError: TextView
    private lateinit var tvPasswordError: TextView
    private lateinit var tvConfirmPasswordError: TextView
    private lateinit var btnRegister: Button
    private lateinit var loadingOverlay: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        tvFullNameError = findViewById(R.id.tvFullNameError)
        tvEmailError = findViewById(R.id.tvEmailError)
        tvPasswordError = findViewById(R.id.tvPasswordError)
        tvConfirmPasswordError = findViewById(R.id.tvConfirmPasswordError)
        btnRegister = findViewById(R.id.btnRegister)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        val btnTogglePassword = findViewById<ImageButton>(R.id.btnTogglePassword)
        val btnToggleConfirmPassword = findViewById<ImageButton>(R.id.btnToggleConfirmPassword)
        val btnGoogle = findViewById<Button>(R.id.btnGoogle)
        val btnApple = findViewById<Button>(R.id.btnApple)
        val tvGoToLogin = findViewById<TextView>(R.id.tvGoToLogin)
        val btnClose = findViewById<TextView>(R.id.btnClose)

        btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            togglePasswordVisibility(etPassword, isPasswordVisible, btnTogglePassword)
        }

        btnToggleConfirmPassword.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            togglePasswordVisibility(etConfirmPassword, isConfirmPasswordVisible, btnToggleConfirmPassword)
        }

        btnRegister.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()
            viewModel.registrar(fullName, email, password, confirmPassword)
        }

        btnGoogle.setOnClickListener { showError("Inicio de sesión con Google (próximamente)") }
        btnApple.setOnClickListener { showError("Inicio de sesión con Apple (próximamente)") }

        tvGoToLogin.setOnClickListener { finish() }
        btnClose.setOnClickListener { finish() }

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

                is AuthState.RegistroExitoso -> {
                    setLoadingState(false)
                    // Navega a la pantalla de éxito y cierra Login + Register del back stack
                    val intent = Intent(this, RegistroSuccessActivity::class.java).apply {
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

                        AuthErrorType.EMAIL_YA_REGISTRADO ->
                            mostrarErrorEnCampo(etEmail, tvEmailError, "Este correo ya está registrado")

                        AuthErrorType.PASSWORDS_NO_COINCIDEN ->
                            mostrarErrorEnCampo(etConfirmPassword, tvConfirmPasswordError, "Las contraseñas no coinciden")

                        else -> showError(estado.mensaje)
                    }
                }

                else -> Unit
            }
        }
    }

    private fun mostrarErrorEnCampo(campo: EditText, tvError: TextView, mensaje: String) {
        campo.setBackgroundResource(R.drawable.bg_field_error)
        tvError.text = mensaje
        tvError.visibility = View.VISIBLE
    }

    private fun limpiarErrorEnCampo(campo: EditText, tvError: TextView) {
        campo.setBackgroundResource(R.drawable.bg_field)
        tvError.visibility = View.GONE
    }

    private fun limpiarErrores() {
        limpiarErrorEnCampo(etFullName, tvFullNameError)
        limpiarErrorEnCampo(etEmail, tvEmailError)
        limpiarErrorEnCampo(etPassword, tvPasswordError)
        limpiarErrorEnCampo(etConfirmPassword, tvConfirmPasswordError)
    }

    private fun limpiarErroresAlEscribir() {
        etFullName.addTextChangedListener(simpleWatcher { limpiarErrorEnCampo(etFullName, tvFullNameError) })
        etEmail.addTextChangedListener(simpleWatcher { limpiarErrorEnCampo(etEmail, tvEmailError) })
        etPassword.addTextChangedListener(simpleWatcher { limpiarErrorEnCampo(etPassword, tvPasswordError) })
        etConfirmPassword.addTextChangedListener(simpleWatcher { limpiarErrorEnCampo(etConfirmPassword, tvConfirmPasswordError) })
    }

    private fun simpleWatcher(onChanged: () -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = onChanged()
        override fun afterTextChanged(s: Editable?) = Unit
    }

    private fun setLoadingState(isLoading: Boolean) {
        loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnRegister.isEnabled = !isLoading
    }

    private fun togglePasswordVisibility(editText: EditText, isVisible: Boolean, toggleButton: ImageButton) {
        val transformation: TransformationMethod? = if (isVisible) {
            null
        } else {
            PasswordTransformationMethod.getInstance()
        }
        editText.transformationMethod = transformation
        toggleButton.setImageResource(
            if (isVisible) R.drawable.ic_eye_open
            else R.drawable.ic_eye_closed
        )
        editText.setSelection(editText.text?.length ?: 0)
    }

    private fun showError(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
    }
}