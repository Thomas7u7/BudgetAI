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
import androidx.lifecycle.lifecycleScope
import com.example.budgetai.data.model.UserState
import com.example.budgetai.ui.user.UserViewModel
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private val userViewModel: UserViewModel by viewModels()
    private val movementViewModel: com.example.budgetai.ui.movements.MovementViewModel by viewModels()

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
            R.id.btnAhorro to "Ahorro"
        )

        botonesProximamente.forEach { (id, nombre) ->
            findViewById<LinearLayout>(id).setOnClickListener {
                Toast.makeText(this, "$nombre (próximamente)", Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<LinearLayout>(R.id.btnMovimientos).setOnClickListener {
            startActivity(Intent(this, MovementsActivity::class.java))
        }

        findViewById<ImageView>(R.id.ivAvatarHome).setOnClickListener {
            startActivity(Intent(this, MyProfileActivity::class.java))
        }

        observarPerfil()
        userViewModel.cargarPerfil()
        cargarSaldo()

    }

    override fun onResume() {
        super.onResume()
        userViewModel.cargarPerfil()
        cargarSaldo()
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

    private fun cargarSaldo() {
        lifecycleScope.launch {
            try {
                val repository = com.example.budgetai.data.repository.MovementRepository()
                val saldo = repository.calcularSaldo()
                val formato = java.text.NumberFormat.getInstance(java.util.Locale("es", "CO"))
                val saldoFormateado = formato.format(saldo.toInt())
                findViewById<TextView>(R.id.tvSaldo).text = "$$saldoFormateado"
            } catch (e: Exception) {
                // Si falla, se queda con el valor que tenga el XML (silencioso, no bloquea el Home)
            }
        }
    }
}