package com.example.budgetai

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.budgetai.data.model.SavingsGoal
import com.example.budgetai.data.model.SavingsState
import com.example.budgetai.ui.savings.SavingsViewModel
import java.text.NumberFormat
import java.util.Locale

class WithdrawSavingsActivity : AppCompatActivity() {

    private val viewModel: SavingsViewModel by viewModels()

    private lateinit var etMonto: EditText
    private lateinit var tvNombreMeta: TextView
    private lateinit var tvDisponible: TextView
    private lateinit var tvValidacion: TextView

    private var goalId: String = ""
    private var montoDisponible: Double = 0.0
    private val formatoMoneda = NumberFormat.getInstance(Locale("es", "CO"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_withdraw_savings)

        etMonto = findViewById(R.id.etMonto)
        tvNombreMeta = findViewById(R.id.tvNombreMeta)
        tvDisponible = findViewById(R.id.tvDisponible)
        tvValidacion = findViewById(R.id.tvValidacion)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        goalId = intent.getStringExtra("goalId") ?: ""
        if (goalId.isEmpty()) {
            finish()
            return
        }

        findViewById<TextView>(R.id.btnMonto100).setOnClickListener { agregarMontoRapido(100.0) }
        findViewById<TextView>(R.id.btnMonto200).setOnClickListener { agregarMontoRapido(200.0) }
        findViewById<TextView>(R.id.btnMonto500).setOnClickListener { agregarMontoRapido(500.0) }
        findViewById<TextView>(R.id.btnMonto1000).setOnClickListener { agregarMontoRapido(1000.0) }

        etMonto.addTextChangedListener(simpleWatcher { validarMonto() })

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

    private fun validarMonto(): Boolean {
        val monto = etMonto.text.toString().toDoubleOrNull() ?: 0.0
        return when {
            monto <= 0 -> {
                tvValidacion.visibility = View.GONE
                false
            }
            monto > montoDisponible -> {
                tvValidacion.text = "No puedes retirar más de $${formatoMoneda.format(montoDisponible.toInt())} disponibles"
                tvValidacion.visibility = View.VISIBLE
                false
            }
            else -> {
                tvValidacion.visibility = View.GONE
                true
            }
        }
    }

    private fun continuar() {
        val monto = etMonto.text.toString().toDoubleOrNull() ?: 0.0
        if (monto <= 0) {
            Toast.makeText(this, "Ingresa un monto válido", Toast.LENGTH_SHORT).show()
            return
        }
        if (!validarMonto()) return

        viewModel.verificarSiTienePin { tienePin ->
            val intent = Intent(this, PinActivity::class.java)
            intent.putExtra("modo", if (tienePin) "retirar" else "configurar")
            intent.putExtra("operacionPendiente", "retirar")
            intent.putExtra("goalId", goalId)
            intent.putExtra("monto", monto)
            startActivity(intent)
        }
    }

    private fun observarEstado() {
        viewModel.savingsState.observe(this) { estado ->
            if (estado is SavingsState.DetalleCargado) {
                mostrarMeta(estado.meta)
            }
        }
    }

    private fun mostrarMeta(meta: SavingsGoal) {
        tvNombreMeta.text = meta.nombre
        montoDisponible = meta.montoAhorrado
        tvDisponible.text = "Disponible: $${formatoMoneda.format(montoDisponible.toInt())}"
    }

    private fun simpleWatcher(onChanged: () -> Unit) = object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = onChanged()
        override fun afterTextChanged(s: android.text.Editable?) = Unit
    }
}