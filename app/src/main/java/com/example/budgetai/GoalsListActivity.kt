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
import com.example.budgetai.data.model.GoalState
import com.example.budgetai.ui.goals.GoalAdapter
import com.example.budgetai.ui.goals.GoalViewModel

class GoalsListActivity : AppCompatActivity() {

    private val viewModel: GoalViewModel by viewModels()
    private lateinit var rvMetas: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var adapter: GoalAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goals_list)

        rvMetas = findViewById(R.id.rvMetas)
        emptyState = findViewById(R.id.emptyState)

        adapter = GoalAdapter(emptyList()) { meta ->
            startActivity(Intent(this, GoalDetailActivity::class.java).putExtra("goalId", meta.id))
        }
        rvMetas.layoutManager = LinearLayoutManager(this)
        rvMetas.adapter = adapter

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnSalir).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnCrearMeta).setOnClickListener {
            startActivity(Intent(this, CreateGoalActivity::class.java))
        }

        viewModel.goalState.observe(this) { estado ->
            when (estado) {
                is GoalState.ListaCargada -> {
                    adapter.actualizarLista(estado.metas)
                    val hay = estado.metas.isNotEmpty()
                    rvMetas.visibility = if (hay) View.VISIBLE else View.GONE
                    emptyState.visibility = if (hay) View.GONE else View.VISIBLE
                }
                is GoalState.Error ->
                    Toast.makeText(this, estado.mensaje, Toast.LENGTH_LONG).show()
                else -> Unit
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.cargarMetas()
    }
}