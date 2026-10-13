package com.example.budgetai

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.budgetai.data.model.UserState
import com.example.budgetai.ui.user.UserViewModel

class SecurityActivity : AppCompatActivity() {

    private val viewModel: UserViewModel by viewModels()

    private lateinit var switchBiometria: Switch
    private lateinit var loadingOverlay: LinearLayout
    private lateinit var tvLoadingText: TextView

    private var ignorarCambioSwitch = false
    private var accionActual = ""  // "password" | "eliminar"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_security)

        switchBiometria = findViewById(R.id.switchBiometria)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        tvLoadingText = findViewById(R.id.tvLoadingText)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<LinearLayout>(R.id.itemCambiarPassword).setOnClickListener {
            mostrarDialogoCambiarPassword()
        }

        findViewById<LinearLayout>(R.id.itemEliminarCuenta).setOnClickListener {
            confirmarEliminarCuenta()
        }

        switchBiometria.setOnCheckedChangeListener { _, isChecked ->
            if (ignorarCambioSwitch) return@setOnCheckedChangeListener
            if (isChecked) {
                solicitarBiometriaParaActivar()
            } else {
                viewModel.actualizarBiometria(false)
                Toast.makeText(this, "Biometría desactivada", Toast.LENGTH_SHORT).show()
            }
        }

        observarEstado()
        viewModel.cargarPerfil()
    }

    private fun solicitarBiometriaParaActivar() {
        val biometricManager = BiometricManager.from(this)
        val puedeAutenticar = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        )

        if (puedeAutenticar != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, "Este dispositivo no tiene biometría configurada", Toast.LENGTH_LONG).show()
            revertirSwitchSinDisparar(false)
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(
            this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    viewModel.actualizarBiometria(true)
                    Toast.makeText(this@SecurityActivity, "Biometría activada", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    revertirSwitchSinDisparar(false)
                }

                override fun onAuthenticationFailed() {
                    Toast.makeText(this@SecurityActivity, "No se reconoció tu huella/rostro", Toast.LENGTH_SHORT).show()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Activar biometría")
            .setSubtitle("Confirma tu identidad para activar el acceso con huella o rostro")
            .setNegativeButtonText("Cancelar")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun revertirSwitchSinDisparar(valor: Boolean) {
        ignorarCambioSwitch = true
        switchBiometria.isChecked = valor
        ignorarCambioSwitch = false
    }

    private fun mostrarDialogoCambiarPassword() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null)
        val etActual = dialogView.findViewById<EditText>(R.id.etPasswordActual)
        val etNueva = dialogView.findViewById<EditText>(R.id.etNuevaPassword)
        val etConfirmar = dialogView.findViewById<EditText>(R.id.etConfirmarPassword)
        val tvError = dialogView.findViewById<TextView>(R.id.tvPasswordError)
        val tvFortaleza = dialogView.findViewById<TextView>(R.id.tvFortaleza)
        val barra1 = dialogView.findViewById<View>(R.id.barraFortaleza1)
        val barra2 = dialogView.findViewById<View>(R.id.barraFortaleza2)
        val barra3 = dialogView.findViewById<View>(R.id.barraFortaleza3)
        val btnActualizar = dialogView.findViewById<Button>(R.id.btnActualizarPassword)

        etNueva.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                actualizarMedidorFortaleza(s.toString(), barra1, barra2, barra3, tvFortaleza)
            }
            override fun afterTextChanged(s: android.text.Editable?) = Unit
        })

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnActualizar.setOnClickListener {
            val actual = etActual.text.toString()
            val nueva = etNueva.text.toString()
            val confirmar = etConfirmar.text.toString()

            if (actual.isBlank()) {
                tvError.text = "Ingresa tu contraseña actual"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (nueva.length < 8) {
                tvError.text = "La nueva contraseña debe tener al menos 8 caracteres"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (nueva != confirmar) {
                tvError.text = "Las contraseñas no coinciden"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            dialog.dismiss()
            accionActual = "password"
            tvLoadingText.text = "Actualizando contraseña..."
            viewModel.cambiarContrasena(actual, nueva)
        }

        dialog.show()
    }

    /** Evalúa la fortaleza de la contraseña y actualiza las 3 barras + el texto. */
    private fun actualizarMedidorFortaleza(
        password: String,
        barra1: View, barra2: View, barra3: View,
        tvFortaleza: TextView
    ) {
        if (password.isEmpty()) {
            listOf(barra1, barra2, barra3).forEach {
                it.setBackgroundColor(ContextCompat.getColor(this, R.color.field_border))
            }
            tvFortaleza.text = ""
            return
        }

        var puntos = 0
        if (password.length >= 8) puntos++
        if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) puntos++
        if (password.any { it.isDigit() }) puntos++
        if (password.any { !it.isLetterOrDigit() }) puntos++

        val (colorRes, texto, barrasActivas) = when {
            puntos <= 1 -> Triple(R.color.error_red, "Seguridad: Débil", 1)
            puntos <= 2 -> Triple(android.graphics.Color.parseColor("#F5A623"), "Seguridad: Media", 2)
            else -> Triple(R.color.green_primary, "Seguridad: Fuerte", 3)
        }

        val color = if (colorRes is Int && colorRes < 0) colorRes else ContextCompat.getColor(this, colorRes as Int)

        val barras = listOf(barra1, barra2, barra3)
        barras.forEachIndexed { index, barra ->
            barra.setBackgroundColor(
                if (index < barrasActivas) color
                else ContextCompat.getColor(this, R.color.field_border)
            )
        }
        tvFortaleza.text = texto
        tvFortaleza.setTextColor(color)
    }

    private fun confirmarEliminarCuenta() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete_account, null)
        val etPassword = dialogView.findViewById<EditText>(R.id.etPasswordConfirmacion)
        val tvError = dialogView.findViewById<TextView>(R.id.tvDeleteError)
        val btnConfirmar = dialogView.findViewById<Button>(R.id.btnConfirmarEliminar)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnConfirmar.setOnClickListener {
            val password = etPassword.text.toString()
            if (password.isBlank()) {
                tvError.text = "Ingresa tu contraseña actual"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            dialog.dismiss()
            accionActual = "eliminar"
            tvLoadingText.text = "Eliminando cuenta..."
            viewModel.reautenticarYEliminarCuenta(password)
        }

        dialog.show()
    }

    private fun observarEstado() {
        viewModel.userState.observe(this) { estado ->
            when (estado) {
                is UserState.Loading -> {
                    loadingOverlay.visibility = View.VISIBLE
                }

                is UserState.PerfilCargado -> {
                    loadingOverlay.visibility = View.GONE
                    revertirSwitchSinDisparar(estado.usuario.seguridad.biometriaHabilitada)
                }

                is UserState.GuardadoExitoso -> {
                    loadingOverlay.visibility = View.GONE
                    when (accionActual) {
                        "eliminar" -> {
                            Toast.makeText(this, "Cuenta eliminada", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, LoginActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(intent)
                            finish()
                        }
                        "password" -> {
                            mostrarPantallaExitoPassword()
                        }
                    }
                    accionActual = ""
                }

                is UserState.Error -> {
                    loadingOverlay.visibility = View.GONE
                    Toast.makeText(this, estado.mensaje, Toast.LENGTH_LONG).show()
                }

                else -> Unit
            }
        }
    }

    private fun mostrarPantallaExitoPassword() {
        AlertDialog.Builder(this)
            .setTitle("¡Contraseña actualizada!")
            .setMessage("Tu contraseña ha sido cambiada exitosamente. Tu cuenta está segura.")
            .setPositiveButton("Continuar", null)
            .show()
    }
}