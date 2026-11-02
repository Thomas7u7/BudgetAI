package com.example.budgetai

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.budgetai.data.model.Goal
import com.example.budgetai.data.model.GoalState
import com.example.budgetai.ui.goals.GoalViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class GoalDetailActivity : AppCompatActivity() {

    private val viewModel: GoalViewModel by viewModels()
    private val formato = NumberFormat.getInstance(Locale("es", "CO"))
    private val formatoFecha = SimpleDateFormat("dd MMM yyyy", Locale("es", "CO"))

    private lateinit var loadingOverlay: LinearLayout
    private var goalId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goal_detail)

        loadingOverlay = findViewById(R.id.loadingOverlay)

        goalId = intent.getStringExtra("goalId") ?: ""
        if (goalId.isEmpty()) { finish(); return }

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnSalir).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnAgregarDinero).setOnClickListener {
            startActivity(Intent(this, AddGoalMoneyActivity::class.java).putExtra("goalId", goalId))
        }
        findViewById<TextView>(R.id.btnCrearNueva).setOnClickListener {
            startActivity(Intent(this, CreateGoalActivity::class.java))
        }
        findViewById<TextView>(R.id.btnModificarMeta).setOnClickListener {
            startActivity(Intent(this, CreateGoalActivity::class.java).putExtra("goalId", goalId))
        }

        viewModel.goalState.observe(this) { estado ->
            when (estado) {
                is GoalState.Loading -> loadingOverlay.visibility = View.VISIBLE
                is GoalState.DetalleCargado -> {
                    loadingOverlay.visibility = View.GONE
                    mostrarMeta(estado.meta)
                }
                is GoalState.Error -> {
                    loadingOverlay.visibility = View.GONE
                    Toast.makeText(this, estado.mensaje, Toast.LENGTH_LONG).show()
                }
                else -> Unit
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.cargarMeta(goalId)
    }

    private fun mostrarMeta(meta: Goal) {
        val completada = meta.estado == "completada"

        findViewById<TextView>(R.id.tvTitulo).text =
            "${SelectCategoryActivity.emojiDe(meta.categoria)} ${meta.nombre}"
        findViewById<TextView>(R.id.tvPorcentaje).text = "${meta.progreso()}%"
        findViewById<ProgressBar>(R.id.progressBar).progress = meta.progreso()
        findViewById<TextView>(R.id.tvAhorrado).text = "$${formato.format(meta.montoAhorrado.toInt())}"
        findViewById<TextView>(R.id.tvObjetivo).text = "$${formato.format(meta.montoObjetivo.toInt())}"
        findViewById<TextView>(R.id.tvRestante).text = "$${formato.format(meta.montoFaltante().toInt())}"
        findViewById<TextView>(R.id.tvFecha).text = formatoFecha.format(meta.fechaObjetivo.toDate())
        findViewById<TextView>(R.id.tvMensual).text = "$${formato.format(meta.ahorroMensualRecomendado.toInt())}"
        findViewById<TextView>(R.id.tvConsejo).text = generarConsejo(meta)

        // Vista normal vs. vista del 100%
        findViewById<View>(R.id.cardFelicidades).visibility = if (completada) View.VISIBLE else View.GONE
        findViewById<View>(R.id.filaMensual).visibility = if (completada) View.GONE else View.VISIBLE
        findViewById<View>(R.id.btnAgregarDinero).visibility = if (completada) View.GONE else View.VISIBLE
        findViewById<View>(R.id.btnRetirarMeta).visibility = if (completada) View.VISIBLE else View.GONE
        findViewById<View>(R.id.btnCrearNueva).visibility = if (completada) View.VISIBLE else View.GONE
        findViewById<View>(R.id.cardConsejo).visibility = if (completada) View.GONE else View.VISIBLE
        findViewById<View>(R.id.btnModificarMeta).visibility = if (completada) View.GONE else View.VISIBLE
    }

    /** Consejo por reglas. Aquí se conectará la IA real al final del proyecto. */
    private fun generarConsejo(meta: Goal): String {
        val mensual = "$${formato.format(meta.ahorroMensualRecomendado.toInt())}"
        return when {
            meta.progreso() >= 75 ->
                "Estás muy cerca de \"${meta.nombre}\". Un último aporte y la logras."
            meta.progreso() >= 25 ->
                "Vas por buen camino. Programa tu ahorro de $mensual justo después de recibir tus ingresos."
            else ->
                "Programa tu ahorro justo después de recibir tus ingresos. Así avanzas primero hacia tu meta, sin depender de lo que sobre al final del mes."
        }
    }
}