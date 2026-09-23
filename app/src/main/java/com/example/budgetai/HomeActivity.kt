package com.example.budgetai

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {

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
    }
}