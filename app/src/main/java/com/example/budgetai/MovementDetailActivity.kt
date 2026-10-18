package com.example.budgetai

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.budgetai.data.model.Movement
import com.example.budgetai.data.model.MovementState
import com.example.budgetai.ui.movements.MovementViewModel
import com.example.budgetai.util.TipoBanner
import com.example.budgetai.util.mostrarBanner
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class MovementDetailActivity : AppCompatActivity() {

    private val viewModel: MovementViewModel by viewModels()

    private lateinit var tvTipo: TextView
    private lateinit var tvMonto: TextView
    private lateinit var tvDescripcion: TextView
    private lateinit var tvCategoria: TextView
    private lateinit var tvFecha: TextView
    private lateinit var btnEditar: Button
    private lateinit var btnEliminar: Button
    private lateinit var loadingOverlay: LinearLayout
    private lateinit var statusBanner: LinearLayout

    private var movementId: String = ""
    private var eliminando = false

    private val formatoMoneda = NumberFormat.getInstance(Locale("es", "CO"))
    private val formatoFecha = SimpleDateFormat("dd MMM yyyy", Locale("es", "CO"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movement_detail)

        tvTipo = findViewById(R.id.tvTipo)
        tvMonto = findViewById(R.id.tvMonto)
        tvDescripcion = findViewById(R.id.tvDescripcion)
        tvCategoria = findViewById(R.id.tvCategoria)
        tvFecha = findViewById(R.id.tvFecha)
        btnEditar = findViewById(R.id.btnEditar)
        btnEliminar = findViewById(R.id.btnEliminar)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        statusBanner = findViewById(R.id.statusBanner)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        movementId = intent.getStringExtra("movementId") ?: ""
        if (movementId.isEmpty()) {
            finish()
            return
        }

        btnEditar.setOnClickListener {
            val intent = Intent(this, RegisterMovementActivity::class.java)
            intent.putExtra("movementId", movementId)
            startActivity(intent)
        }

        btnEliminar.setOnClickListener {
            confirmarEliminar()
        }

        observarEstado()
        viewModel.cargarMovimiento(movementId)
    }

    override fun onResume() {
        super.onResume()
        // Recarga por si el usuario volvió de editar el movimiento
        if (movementId.isNotEmpty() && !eliminando) {
            viewModel.cargarMovimiento(movementId)
        }
    }

    private fun confirmarEliminar() {
        AlertDialog.Builder(this)
            .setTitle("¿Eliminar este movimiento?")
            .setMessage("Esta acción no se puede deshacer. Se eliminará \"${tvCategoria.text}\" por ${tvMonto.text}.")
            .setPositiveButton("Eliminar") { _, _ ->
                eliminando = true
                viewModel.eliminarMovimiento(movementId)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun observarEstado() {
        viewModel.movementState.observe(this) { estado ->
            when (estado) {
                is MovementState.Loading -> {
                    loadingOverlay.visibility = View.VISIBLE
                }

                is MovementState.DetalleCargado -> {
                    loadingOverlay.visibility = View.GONE
                    mostrarMovimiento(estado.movimiento)
                }

                is MovementState.Eliminado -> {
                    loadingOverlay.visibility = View.GONE
                    setResult(RESULT_OK)
                    finish()
                }

                is MovementState.Error -> {
                    loadingOverlay.visibility = View.GONE
                    statusBanner.mostrarBanner(estado.mensaje, TipoBanner.ERROR)
                }

                else -> Unit
            }
        }
    }

    private fun mostrarMovimiento(movimiento: Movement) {
        val esIngreso = movimiento.tipo == "ingreso"
        tvTipo.text = if (esIngreso) "Ingreso" else "Gasto"

        val montoFormateado = "$${formatoMoneda.format(movimiento.monto.toInt())}"
        tvMonto.text = if (esIngreso) "+$montoFormateado" else "-$montoFormateado"
        tvMonto.setTextColor(getColor(if (esIngreso) R.color.green_primary else R.color.error_red))

        tvDescripcion.text = movimiento.descripcion.ifBlank { "Sin descripción" }
        tvCategoria.text = movimiento.categoria
        tvFecha.text = formatoFecha.format(movimiento.fecha.toDate())
    }
}