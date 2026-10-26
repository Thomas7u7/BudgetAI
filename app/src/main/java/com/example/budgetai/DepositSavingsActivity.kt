package com.example.budgetai

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.budgetai.data.model.SavingsState
import com.example.budgetai.ui.savings.SavingsViewModel
import java.text.NumberFormat
import java.util.Locale

class DepositSavingsActivity : AppCompatActivity() {

    private val viewModel: SavingsViewModel by viewModels()

    private lateinit var etMonto: EditText
    private lateinit var tvNombreMeta: TextView
    private lateinit var tvValidacion: TextView

    private var goalId: String = ""
    private val formatoMoneda = NumberFormat.getInstance(Locale("es", "CO"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deposit_savings)

        etMonto = findViewById(R.id.etMonto)
        tvNombreMeta = findViewById(R.id.tvNombreMeta)
        tvValidacion = findViewById(R.id.tvValidacion)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        goalId = intent.getStringExtra("goalId") ?: ""
        if (goalId.isEmpty()) {
            finish()
            return
        }

        // Montos rápidos
        findViewById<TextView>(R.id.btnMonto100).setOnClickListener { agregarMontoRapido(100.0) }
        findViewById<TextView>(R.id.btnMonto200).setOnClickListener { agregarMontoRapido(200.0) }
        findViewById<TextView>(R.id.btnMonto500).setOnClickListener { agregarMontoRapido(500.0) }
        findViewById<TextView>(R.id.btnMonto1000).setOnClickListener { agregarMontoRapido(1000.0) }

        etMonto.addTextChangedListener(simpleWatcher { actualizarValidacion() })

        findViewById<TextView>(R.id.btnContinuar).setOnClickListener { continuar() }
        findViewById<TextView>(R.id.btnCancelar).setOnClickListener { finish() }

        observarEstado()
        viewModel.cargarMeta(goalId)
    }

    private fun agregarMontoRapido(valor: Double) {
        val actual = etMonto.text.toString().toDoubleOrNull() ?: 0.0
        etMonto.setText((actual + valor).toInt().toString())
        etMonto.setSelection(etMonto.text.length)
    }

    private fun actualizarValidacion() {
        val monto = etMonto.text.toString().toDoubleOrNull() ?: 0.0
        if (monto > 0) {
            tvValidacion.text = "✓ Mayor a $0,00"
            tvValidacion.setTextColor(getColor(R.color.green_primary))
        } else {
            tvValidacion.text = "✕ Ingresa un monto válido"
            tvValidacion.setTextColor(getColor(R.color.error_red))
        }
    }

    private fun continuar() {
        val monto = etMonto.text.toString().toDoubleOrNull() ?: 0.0
        if (monto <= 0) {
            Toast.makeText(this, "Ingresa un monto válido", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.verificarSiTienePin { tienePin ->
            val intent = Intent(this, PinActivity::class.java)
            intent.putExtra("modo", if (tienePin) "depositar" else "configurar")
            intent.putExtra("operacionPendiente", "depositar") // siempre indica la intención real
            intent.putExtra("goalId", goalId)
            intent.putExtra("monto", monto)
            startActivity(intent)
        }
    }

    private fun observarEstado() {
        viewModel.savingsState.observe(this) { estado ->
            if (estado is SavingsState.DetalleCargado) {
                tvNombreMeta.text = estado.meta.nombre
            }
        }
    }

    private fun simpleWatcher(onChanged: () -> Unit) = object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = onChanged()
        override fun afterTextChanged(s: android.text.Editable?) = Unit
    }
}