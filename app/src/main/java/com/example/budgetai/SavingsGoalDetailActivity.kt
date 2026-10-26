package com.example.budgetai

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.budgetai.data.model.SavingsGoal
import com.example.budgetai.data.model.SavingsState
import com.example.budgetai.ui.savings.SavingsViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class SavingsGoalDetailActivity : AppCompatActivity() {

    private val viewModel: SavingsViewModel by viewModels()

    private lateinit var tvTitulo: TextView
    private lateinit var tvPorcentaje: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvAhorrado: TextView
    private lateinit var tvObjetivo: TextView
    private lateinit var tvFaltante: TextView
    private lateinit var tvFecha: TextView
    private lateinit var tvRecomendado: TextView
    private lateinit var cardSinDinero: LinearLayout
    private lateinit var loadingOverlay: LinearLayout

    private var goalId: String = ""
    private var eliminando = false

    private val formatoMoneda = NumberFormat.getInstance(Locale("es", "CO"))
    private val formatoFecha = SimpleDateFormat("dd MMM yyyy", Locale("es", "CO"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_savings_goal_detail)

        tvTitulo = findViewById(R.id.tvTitulo)
        tvPorcentaje = findViewById(R.id.tvPorcentaje)
        progressBar = findViewById(R.id.progressBar)
        tvAhorrado = findViewById(R.id.tvAhorrado)
        tvObjetivo = findViewById(R.id.tvObjetivo)
        tvFaltante = findViewById(R.id.tvFaltante)
        tvFecha = findViewById(R.id.tvFecha)
        tvRecomendado = findViewById(R.id.tvRecomendado)
        cardSinDinero = findViewById(R.id.cardSinDinero)
        loadingOverlay = findViewById(R.id.loadingOverlay)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        goalId = intent.getStringExtra("goalId") ?: ""
        if (goalId.isEmpty()) {
            finish()
            return
        }

        findViewById<TextView>(R.id.btnAgregarDinero).setOnClickListener {
            val intent = Intent(this, DepositSavingsActivity::class.java)
            intent.putExtra("goalId", goalId)
            startActivity(intent)
        }

        findViewById<TextView>(R.id.btnEditarMeta).setOnClickListener {
            val intent = Intent(this, CreateSavingsGoalActivity::class.java)
            intent.putExtra("goalId", goalId)
            startActivity(intent)
        }

        // --- Parte DELETE ---
        findViewById<TextView>(R.id.btnEliminarMeta).setOnClickListener {
            confirmarEliminar()
        }

        observarEstado()
        viewModel.cargarMeta(goalId)
    }

    override fun onResume() {
        super.onResume()
        if (goalId.isNotEmpty() && !eliminando) {
            viewModel.cargarMeta(goalId)
        }
    }

    /** Diálogo de confirmación antes de eliminar — parte DELETE del CRUD. */
    private fun confirmarEliminar() {
        AlertDialog.Builder(this)
            .setTitle("¿Eliminar esta meta?")
            .setMessage("Esta acción no se puede deshacer. Se eliminará \"${tvTitulo.text}\" junto con todo su historial de depósitos y retiros.")
            .setPositiveButton("Eliminar") { _, _ ->
                eliminando = true
                viewModel.eliminarMeta(goalId)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun observarEstado() {
        viewModel.savingsState.observe(this) { estado ->
            when (estado) {
                is SavingsState.Loading -> {
                    loadingOverlay.visibility = View.VISIBLE
                }

                is SavingsState.DetalleCargado -> {
                    loadingOverlay.visibility = View.GONE
                    mostrarMeta(estado.meta)
                }

                is SavingsState.Eliminado -> {
                    loadingOverlay.visibility = View.GONE
                    Toast.makeText(this, "Meta eliminada", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, SavingsHomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    finish()
                }

                is SavingsState.Error -> {
                    loadingOverlay.visibility = View.GONE
                    Toast.makeText(this, estado.mensaje, Toast.LENGTH_LONG).show()
                }

                else -> Unit
            }
        }
    }

    private fun mostrarMeta(meta: SavingsGoal) {
        tvTitulo.text = meta.nombre
        val progreso = meta.progreso()
        tvPorcentaje.text = "$progreso%"
        progressBar.progress = progreso
        tvAhorrado.text = "$${formatoMoneda.format(meta.montoAhorrado.toInt())} COP"
        tvObjetivo.text = "$${formatoMoneda.format(meta.montoObjetivo.toInt())} COP"
        tvFaltante.text = "$${formatoMoneda.format(meta.montoFaltante().toInt())} COP"
        tvFecha.text = formatoFecha.format(meta.fechaLimite.toDate())
        tvRecomendado.text = "$${formatoMoneda.format(meta.montoRecomendado.toInt())} COP"

        cardSinDinero.visibility = if (meta.montoAhorrado <= 0) View.VISIBLE else View.GONE
    }
}