package com.example.budgetai

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.budgetai.data.model.UserState
import com.example.budgetai.ui.user.UserViewModel

class HomeActivity : AppCompatActivity() {

    private val userViewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val botonesProximamente = listOf(
            R.id.btnEnviar to "Enviar",
            R.id.btnMeta to "Meta",
            R.id.btnQR to "Código QR",
            R.id.btnDisponible to "Disponible",
            R.id.btnAlertas to "Alertas",
            R.id.btnGastos to "Gastos",
            R.id.btnMovimientos to "Movimientos",
            R.id.btnAhorro to "Ahorro"
        )

        botonesProximamente.forEach { (id, nombre) ->
            findViewById<LinearLayout>(id).setOnClickListener {
                Toast.makeText(this, "$nombre (próximamente)", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<ImageView>(R.id.ivAvatarHome).setOnClickListener {
            startActivity(Intent(this, MyProfileActivity::class.java))
        }

        observarPerfil()
        userViewModel.cargarPerfil()
    }

    override fun onResume() {
        super.onResume()
        userViewModel.cargarPerfil()
    }

    private fun observarPerfil() {
        userViewModel.userState.observe(this) { estado ->
            if (estado is UserState.PerfilCargado) {
                val usuario = estado.usuario
                val primerNombre = usuario.nombreCompleto.split(" ").firstOrNull() ?: "Usuario"
                findViewById<TextView>(R.id.tvUserName).text = primerNombre

                if (!usuario.fotoBase64.isNullOrBlank()) {
                    try {
                        val bytes = Base64.decode(usuario.fotoBase64, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        val avatar = findViewById<ImageView>(R.id.ivAvatarHome)
                        avatar.setImageBitmap(bitmap)
                        avatar.setPadding(0, 0, 0, 0)
                        avatar.imageTintList = null
                    } catch (e: Exception) {
                        // Mantiene el placeholder si falla
                    }
                }
            }
        }
    }
}