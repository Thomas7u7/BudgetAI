package com.example.budgetai

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.budgetai.data.model.GoalState
import com.example.budgetai.data.repository.MovementRepository
import com.example.budgetai.ui.goals.GoalViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class AddGoalMoneyActivity : AppCompatActivity() {

    private val viewModel: GoalViewModel by viewModels()
    private val formato = NumberFormat.getInstance(Locale("es", "CO"))

    private lateinit var etDeposito: EditText
    private lateinit var errDeposito: TextView
    private lateinit var loadingOverlay: LinearLayout
    private var goalId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_goal_money)

        etDeposito = findViewById(R.id.etDeposito)
        errDeposito = findViewById(R.id.errDeposito)
        loadingOverlay = findViewById(R.id.loadingOverlay)

        goalId = intent.getStringExtra("goalId") ?: ""
        if (goalId.isEmpty()) { finish(); return }

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnContinuar).setOnClickListener {
            errDeposito.visibility = View.GONE
            etDeposito.setBackgroundResource(R.drawable.bg_field)
            viewModel.agregarDinero(goalId, etDeposito.text.toString().toDoubleOrNull() ?: 0.0)
        }

        // Muestra el saldo disponible del que saldrá el dinero
        lifecycleScope.launch {
            try {
                val saldo = MovementRepository().calcularSaldo()
                findViewById<TextView>(R.id.tvDisponible).text =
                    "Saldo disponible: $${formato.format(saldo.toInt())}"
            } catch (e: Exception) { /* se queda vacío */ }
        }

        viewModel.goalState.observe(this) { estado ->
            when (estado) {
                is GoalState.Loading -> loadingOverlay.visibility = View.VISIBLE
                is GoalState.DetalleCargado -> {
                    loadingOverlay.visibility = View.GONE
                    findViewById<TextView>(R.id.tvNombreMeta).text = estado.meta.nombre
                }
                is GoalState.DineroAgregado -> {
                    Toast.makeText(this, "Dinero agregado a tu meta", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is GoalState.ErrorCampos -> {
                    loadingOverlay.visibility = View.GONE
                    estado.errores["deposito"]?.let {
                        etDeposito.setBackgroundResource(R.drawable.bg_field_error)
                        errDeposito.text = "⚠ $it"
                        errDeposito.visibility = View.VISIBLE
                    }
                }
                is GoalState.Error -> {
                    loadingOverlay.visibility = View.GONE
                    Toast.makeText(this, estado.mensaje, Toast.LENGTH_LONG).show()
                }
                else -> Unit
            }
        }
        viewModel.cargarMeta(goalId)
    }
}