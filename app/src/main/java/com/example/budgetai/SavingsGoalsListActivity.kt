package com.example.budgetai

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetai.data.model.SavingsGoal
import com.example.budgetai.data.model.SavingsState
import com.example.budgetai.ui.savings.SavingsGoalAdapter
import com.example.budgetai.ui.savings.SavingsViewModel

class SavingsGoalsListActivity : AppCompatActivity() {

    private val viewModel: SavingsViewModel by viewModels()

    private lateinit var rvMetas: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var adapter: SavingsGoalAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_savings_goals_list)

        rvMetas = findViewById(R.id.rvMetas)
        emptyState = findViewById(R.id.emptyState)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        adapter = SavingsGoalAdapter(emptyList()) { meta ->
            val intent = Intent(this, SavingsGoalDetailActivity::class.java)
            intent.putExtra("goalId", meta.id)
            startActivity(intent)
        }
        rvMetas.layoutManager = LinearLayoutManager(this)
        rvMetas.adapter = adapter

        findViewById<TextView>(R.id.btnNuevaMeta).setOnClickListener {
            startActivity(Intent(this, CreateSavingsGoalActivity::class.java))
        }

        // Bottom nav
        findViewById<LinearLayout>(R.id.navMiAhorro).setOnClickListener {
            startActivity(Intent(this, SavingsHomeActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.navMetas).setOnClickListener {
            // ya estamos aquí
        }
        findViewById<LinearLayout>(R.id.navAhorroIA).setOnClickListener {
            startActivity(Intent(this, SavingsAIActivity::class.java))
        }

        observarEstado()
    }

    override fun onResume() {
        super.onResume()
        viewModel.cargarMetas()
    }

    private fun observarEstado() {
        viewModel.savingsState.observe(this) { estado ->
            when (estado) {
                is SavingsState.ListaCargada -> {
                    mostrarLista(estado.metas)
                }

                is SavingsState.Error -> {
                    Toast.makeText(this, estado.mensaje, Toast.LENGTH_LONG).show()
                }

                else -> Unit
            }
        }
    }

    private fun mostrarLista(metas: List<SavingsGoal>) {
        adapter.actualizarLista(metas)
        val hayMetas = metas.isNotEmpty()
        rvMetas.visibility = if (hayMetas) View.VISIBLE else View.GONE
        emptyState.visibility = if (hayMetas) View.GONE else View.VISIBLE
    }
}