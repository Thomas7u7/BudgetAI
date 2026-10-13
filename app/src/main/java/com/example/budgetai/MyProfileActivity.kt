package com.example.budgetai

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.budgetai.data.model.UserState
import com.example.budgetai.ui.user.UserViewModel

class MyProfileActivity : AppCompatActivity() {

    private val viewModel: UserViewModel by viewModels()

    private lateinit var ivAvatar: ImageView
    private lateinit var tvNombre: TextView
    private lateinit var tvCorreo: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_profile)

        ivAvatar = findViewById(R.id.ivAvatar)
        tvNombre = findViewById(R.id.tvNombre)
        tvCorreo = findViewById(R.id.tvCorreo)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<LinearLayout>(R.id.itemEditarPerfil).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.itemInfoFinanciera).setOnClickListener {
            startActivity(Intent(this, FinancialInfoActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.itemPreferencias).setOnClickListener {
            startActivity(Intent(this, PreferencesActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.itemSeguridad).setOnClickListener {
            startActivity(Intent(this, SecurityActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.itemCerrarSesion).setOnClickListener {
            viewModel.cerrarSesion()
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finish()
        }

        findViewById<LinearLayout>(R.id.navInicio).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.navAnalisis).setOnClickListener {
            Toast.makeText(this, "Análisis (próximamente)", Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.navTarjetas).setOnClickListener {
            Toast.makeText(this, "Tarjetas (próximamente)", Toast.LENGTH_SHORT).show()
        }

        observarEstado()
        viewModel.cargarPerfil()
    }

    override fun onResume() {
        super.onResume()
        // Recarga el perfil cada vez que se vuelve a esta pantalla (ej. después de editar)
        viewModel.cargarPerfil()
    }

    private fun observarEstado() {
        viewModel.userState.observe(this) { estado ->
            when (estado) {
                is UserState.PerfilCargado -> {
                    val usuario = estado.usuario
                    tvNombre.text = usuario.nombreCompleto.ifBlank { "Sin nombre" }
                    tvCorreo.text = usuario.correo

                    if (!usuario.fotoBase64.isNullOrBlank()) {
                        try {
                            val bytes = android.util.Base64.decode(usuario.fotoBase64, android.util.Base64.DEFAULT)
                            val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            ivAvatar.setImageBitmap(bitmap)
                            ivAvatar.setPadding(0, 0, 0, 0)
                            ivAvatar.imageTintList = null
                        } catch (e: Exception) {
                            // Si falla la decodificación, se queda con el ícono placeholder
                        }

                    }
                }

                is UserState.Error -> {
                    Toast.makeText(this, estado.mensaje, Toast.LENGTH_SHORT).show()
                }

                else -> Unit
            }
        }
    }
}