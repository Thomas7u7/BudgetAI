package com.example.budgetai

import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.NumberFormat
import java.util.Locale

class OperationResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_operation_result)

        val tipo = intent.getStringExtra("tipo") ?: "deposito_exitoso"
        val goalId = intent.getStringExtra("goalId")
        val monto = intent.getDoubleExtra("monto", 0.0)
        val mensajeError = intent.getStringExtra("mensaje")

        val tvTitulo = findViewById<TextView>(R.id.tvTitulo)
        val iconContainer = findViewById<FrameLayout>(R.id.iconContainer)
        val tvIcono = findViewById<TextView>(R.id.tvIcono)
        val tvResultado = findViewById<TextView>(R.id.tvResultado)
        val tvMensaje = findViewById<TextView>(R.id.tvMensaje)
        val cardResumen = findViewById<android.widget.LinearLayout>(R.id.cardResumen)
        val tvEtiquetaMonto = findViewById<TextView>(R.id.tvEtiquetaMonto)
        val tvMonto = findViewById<TextView>(R.id.tvMonto)
        val tvMetaDestino = findViewById<TextView>(R.id.tvMetaDestino)
        val btnPrincipal = findViewById<TextView>(R.id.btnPrincipal)
        val btnSecundario = findViewById<TextView>(R.id.btnSecundario)

        val formatoMoneda = NumberFormat.getInstance(Locale("es", "CO"))

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        when (tipo) {
            "deposito_exitoso" -> {
                tvTitulo.text = "Depósito de ahorro"
                iconContainer.setBackgroundResource(R.drawable.bg_circle_success)
                tvIcono.text = "✓"
                tvIcono.setTextColor(getColor(R.color.green_primary))
                tvResultado.text = "Depósito Exitoso"
                tvMensaje.text = "El depósito a tu meta de ahorro se ha realizado correctamente."
                cardResumen.visibility = android.view.View.VISIBLE
                tvEtiquetaMonto.text = "Monto depositado"
                tvMonto.text = "$${formatoMoneda.format(monto.toInt())} COP"
                tvMetaDestino.text = "Mi meta"
                btnPrincipal.text = "Volver al ahorro"
                btnSecundario.text = "Salir"
            }

            "retiro_exitoso" -> {
                tvTitulo.text = "Retiro de dinero"
                iconContainer.setBackgroundResource(R.drawable.bg_circle_success)
                tvIcono.text = "✓"
                tvIcono.setTextColor(getColor(R.color.green_primary))
                tvResultado.text = "Retiro Exitoso"
                tvMensaje.text = "El dinero retirado ha sido transferido a tu cuenta destino vinculada correctamente."
                cardResumen.visibility = android.view.View.VISIBLE
                tvEtiquetaMonto.text = "Monto retirado"
                tvMonto.text = "$${formatoMoneda.format(monto.toInt())} COP"
                tvMetaDestino.text = "Mi meta"
                btnPrincipal.text = "Volver al ahorro"
                btnSecundario.text = "Salir"
            }

            "deposito_fallido", "retiro_fallido" -> {
                tvTitulo.text = if (tipo == "deposito_fallido") "Depósito de ahorro" else "Retiro de dinero"
                iconContainer.setBackgroundResource(R.drawable.bg_circle_error)
                tvIcono.text = "⚠"
                tvIcono.setTextColor(getColor(R.color.error_red))
                tvResultado.text = "No se pudo procesar"
                tvMensaje.text = mensajeError
                    ?: "Lamentablemente la operación no pudo ser completada. Por favor verifica tu conexión o fondos disponibles."
                cardResumen.visibility = android.view.View.GONE
                btnPrincipal.text = "Volver a intentar"
                btnSecundario.text = "Volver al formulario"
            }
        }

        btnPrincipal.setOnClickListener {
            manejarBotonPrincipal(tipo, goalId)
        }

        btnSecundario.setOnClickListener {
            manejarBotonSecundario(tipo, goalId)
        }
    }

    private fun manejarBotonPrincipal(tipo: String, goalId: String?) {
        when (tipo) {
            "deposito_exitoso", "retiro_exitoso" -> {
                val intent = Intent(this, SavingsHomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }
            "deposito_fallido" -> {
                val intent = Intent(this, DepositSavingsActivity::class.java)
                intent.putExtra("goalId", goalId)
                startActivity(intent)
                finish()
            }
            "retiro_fallido" -> {
                val intent = Intent(this, WithdrawSavingsActivity::class.java)
                intent.putExtra("goalId", goalId)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun manejarBotonSecundario(tipo: String, goalId: String?) {
        when (tipo) {
            "deposito_exitoso", "retiro_exitoso" -> finish()
            else -> {
                val intent = Intent(this, SavingsGoalDetailActivity::class.java)
                intent.putExtra("goalId", goalId)
                startActivity(intent)
                finish()
            }
        }
    }
}