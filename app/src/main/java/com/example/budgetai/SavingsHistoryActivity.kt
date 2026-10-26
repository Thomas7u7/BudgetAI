package com.example.budgetai

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetai.data.model.SavingsState
import com.example.budgetai.ui.savings.SavingsMovementAdapter
import com.example.budgetai.ui.savings.SavingsViewModel

class SavingsHistoryActivity : AppCompatActivity() {

    private val viewModel: SavingsViewModel by viewModels()

    private lateinit var rvHistorial: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var adapter: SavingsMovementAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_savings_history)

        rvHistorial = findViewById(R.id.rvHistorial)
        emptyState = findViewById(R.id.emptyState)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        adapter = SavingsMovementAdapter(emptyList())
        rvHistorial.layoutManager = LinearLayoutManager(this)
        rvHistorial.adapter = adapter

        val goalId = intent.getStringExtra("goalId") ?: ""
        if (goalId.isEmpty()) {
            finish()
            return
        }

        observarEstado()
        viewModel.cargarHistorial(goalId)
    }

    private fun observarEstado() {
        viewModel.savingsState.observe(this) { estado ->
            when (estado) {
                is SavingsState.HistorialCargado -> {
                    adapter.actualizarLista(estado.movimientos)
                    val hayMovimientos = estado.movimientos.isNotEmpty()
                    rvHistorial.visibility = if (hayMovimientos) View.VISIBLE else View.GONE
                    emptyState.visibility = if (hayMovimientos) View.GONE else View.VISIBLE
                }

                is SavingsState.Error -> {
                    Toast.makeText(this, estado.mensaje, Toast.LENGTH_LONG).show()
                }

                else -> Unit
            }
        }
    }
}