package com.example.budgetai

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetai.data.model.Movement
import com.example.budgetai.data.model.MovementState
import com.example.budgetai.ui.movements.MovementAdapter
import com.example.budgetai.ui.movements.MovementViewModel
import java.text.NumberFormat
import java.util.Locale

class MovementsActivity : AppCompatActivity() {

    private val viewModel: MovementViewModel by viewModels()

    private lateinit var rvMovimientos: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var loadingOverlay: LinearLayout
    private lateinit var etBuscar: EditText
    private lateinit var tvTotalIngresos: TextView
    private lateinit var tvTotalGastos: TextView

    private lateinit var adapter: MovementAdapter
    private var listaCompleta: List<Movement> = emptyList()

    private val formatoMoneda = NumberFormat.getInstance(Locale("es", "CO"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movements)

        rvMovimientos = findViewById(R.id.rvMovimientos)
        emptyState = findViewById(R.id.emptyState)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        etBuscar = findViewById(R.id.etBuscar)
        tvTotalIngresos = findViewById(R.id.tvTotalIngresos)
        tvTotalGastos = findViewById(R.id.tvTotalGastos)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        adapter = MovementAdapter(emptyList()) { movimiento ->
            val intent = Intent(this, MovementDetailActivity::class.java)
            intent.putExtra("movementId", movimiento.id)
            startActivity(intent)
        }
        rvMovimientos.layoutManager = LinearLayoutManager(this)
        rvMovimientos.adapter = adapter

        findViewById<Button>(R.id.btnRegistrar).setOnClickListener {
            startActivity(Intent(this, RegisterMovementActivity::class.java))
        }
        findViewById<Button>(R.id.btnRegistrarVacio).setOnClickListener {
            startActivity(Intent(this, RegisterMovementActivity::class.java))
        }

        etBuscar.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrarLista(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) = Unit
        })

        observarEstado()
    }

    override fun onResume() {
        super.onResume()
        viewModel.cargarMovimientos()
    }

    private fun filtrarLista(query: String) {
        val filtrada = if (query.isBlank()) {
            listaCompleta
        } else {
            listaCompleta.filter {
                it.categoria.contains(query, ignoreCase = true) ||
                        it.descripcion.contains(query, ignoreCase = true)
            }
        }
        adapter.actualizarLista(filtrada)
    }

    private fun observarEstado() {
        viewModel.movementState.observe(this) { estado ->
            when (estado) {
                is MovementState.Loading -> {
                    loadingOverlay.visibility = View.VISIBLE
                }

                is MovementState.ListaCargada -> {
                    loadingOverlay.visibility = View.GONE
                    listaCompleta = estado.movimientos
                    adapter.actualizarLista(listaCompleta)

                    val hayMovimientos = listaCompleta.isNotEmpty()
                    emptyState.visibility = if (hayMovimientos) View.GONE else View.VISIBLE
                    rvMovimientos.visibility = if (hayMovimientos) View.VISIBLE else View.GONE

                    val totalIngresos = listaCompleta.filter { it.tipo == "ingreso" }.sumOf { it.monto }
                    val totalGastos = listaCompleta.filter { it.tipo == "gasto" }.sumOf { it.monto }
                    tvTotalIngresos.text = "+$${formatoMoneda.format(totalIngresos.toInt())}"
                    tvTotalGastos.text = "-$${formatoMoneda.format(totalGastos.toInt())}"
                }

                is MovementState.Error -> {
                    loadingOverlay.visibility = View.GONE
                    Toast.makeText(this, estado.mensaje, Toast.LENGTH_LONG).show()
                }

                else -> Unit
            }
        }
    }
}