package com.example.budgetai

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.budgetai.data.model.UserState
import com.example.budgetai.ui.user.UserViewModel
import com.example.budgetai.util.TipoBanner
import com.example.budgetai.util.mostrarBanner
import com.example.budgetai.util.ocultarBanner
import java.io.ByteArrayOutputStream

class EditProfileActivity : AppCompatActivity() {

    private val viewModel: UserViewModel by viewModels()

    private lateinit var ivAvatar: ImageView
    private lateinit var etNombre: EditText
    private lateinit var etCorreo: EditText
    private lateinit var etTelefono: EditText
    private lateinit var tvNombreError: TextView
    private lateinit var btnGuardar: Button
    private lateinit var loadingOverlay: LinearLayout
    private lateinit var statusBanner: LinearLayout

    private var fotoBase64Nueva: String? = null

    private val selectorImagen = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            procesarImagenSeleccionada(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        ivAvatar = findViewById(R.id.ivAvatar)
        etNombre = findViewById(R.id.etNombre)
        etCorreo = findViewById(R.id.etCorreo)
        etTelefono = findViewById(R.id.etTelefono)
        tvNombreError = findViewById(R.id.tvNombreError)
        btnGuardar = findViewById(R.id.btnGuardar)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        statusBanner = findViewById(R.id.statusBanner)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        val abrirSelector = {
            selectorImagen.launch(
                androidx.activity.result.PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        }
        findViewById<ImageView>(R.id.btnCambiarFoto).setOnClickListener { abrirSelector() }
        findViewById<TextView>(R.id.tvCambiarFoto).setOnClickListener { abrirSelector() }

        btnGuardar.setOnClickListener {
            statusBanner.ocultarBanner()
            val nombre = etNombre.text.toString().trim()
            val telefono = etTelefono.text.toString().trim()

            if (nombre.isEmpty()) {
                tvNombreError.text = "El nombre no puede estar vacío"
                tvNombreError.visibility = View.VISIBLE
                etNombre.setBackgroundResource(R.drawable.bg_field_error)
                return@setOnClickListener
            }

            viewModel.actualizarDatosBasicos(
                nombreCompleto = nombre,
                telefono = telefono.ifEmpty { null },
                fotoBase64 = fotoBase64Nueva
            )
        }

        etNombre.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tvNombreError.visibility = View.GONE
                etNombre.setBackgroundResource(R.drawable.bg_field)
            }
            override fun afterTextChanged(s: android.text.Editable?) = Unit
        })

        observarEstado()
        viewModel.cargarPerfil()
    }

    private fun procesarImagenSeleccionada(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmapOriginal = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            val bitmapRedimensionado = redimensionarBitmap(bitmapOriginal, 300)

            val outputStream = ByteArrayOutputStream()
            bitmapRedimensionado.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val bytes = outputStream.toByteArray()

            if (bytes.size > 700_000) {
                Toast.makeText(this, "La imagen es muy pesada, elige otra", Toast.LENGTH_SHORT).show()
                return
            }

            fotoBase64Nueva = Base64.encodeToString(bytes, Base64.DEFAULT)
            ivAvatar.setImageBitmap(bitmapRedimensionado)
            ivAvatar.setPadding(0, 0, 0, 0)
            ivAvatar.imageTintList = null

        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo procesar la imagen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun redimensionarBitmap(original: Bitmap, maxSize: Int): Bitmap {
        val ancho = original.width
        val alto = original.height
        val ratio = minOf(maxSize.toFloat() / ancho, maxSize.toFloat() / alto)
        if (ratio >= 1) return original
        val nuevoAncho = (ancho * ratio).toInt()
        val nuevoAlto = (alto * ratio).toInt()
        return Bitmap.createScaledBitmap(original, nuevoAncho, nuevoAlto, true)
    }

    private fun observarEstado() {
        viewModel.userState.observe(this) { estado ->
            when (estado) {
                is UserState.Loading -> {
                    loadingOverlay.visibility = View.VISIBLE
                }

                is UserState.PerfilCargado -> {
                    loadingOverlay.visibility = View.GONE
                    val usuario = estado.usuario
                    etNombre.setText(usuario.nombreCompleto)
                    etCorreo.setText(usuario.correo)
                    etTelefono.setText(usuario.telefono ?: "")

                    if (!usuario.fotoBase64.isNullOrBlank() && fotoBase64Nueva == null) {
                        try {
                            val bytes = Base64.decode(usuario.fotoBase64, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            ivAvatar.setImageBitmap(bitmap)
                            ivAvatar.setPadding(0, 0, 0, 0)
                            ivAvatar.imageTintList = null
                        } catch (e: Exception) {
                            // Se queda con el placeholder si falla
                        }
                    }
                }

                is UserState.GuardadoExitoso -> {
                    loadingOverlay.visibility = View.GONE
                    statusBanner.mostrarBanner("Cambios guardados exitosamente", TipoBanner.EXITO)
                }

                is UserState.Error -> {
                    loadingOverlay.visibility = View.GONE
                    statusBanner.mostrarBanner(
                        "No pudimos guardar tus cambios. Verifica tu conexión e intenta de nuevo.",
                        TipoBanner.ERROR
                    )
                }

                else -> Unit
            }
        }
    }
}