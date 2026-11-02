package com.example.budgetai

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.budgetai.data.model.GoalState
import com.example.budgetai.ui.goals.GoalViewModel
import com.google.firebase.Timestamp
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

class GoalAnalysisActivity : AppCompatActivity() {

    private val viewModel: GoalViewModel by viewModels()
    private val formato = NumberFormat.getInstance(Locale("es", "CO"))

    private lateinit var loadingOverlay: LinearLayout
    private lateinit var tvError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goal_analysis)

        loadingOverlay = findViewById(R.id.loadingOverlay)
        tvError = findViewById(R.id.tvError)

        val nombre = intent.getStringExtra("nombre") ?: ""
        val categoria = intent.getStringExtra("categoria") ?: ""
        val monto = intent.getDoubleExtra("monto", 0.0)
        val inicial = intent.getDoubleExtra("inicial", 0.0)
        val fecha = Timestamp(Date(intent.getLongExtra("fechaMillis", 0L)))

        val restante = (monto - inicial).coerceAtLeast(0.0)
        val dias = (fecha.toDate().time - System.currentTimeMillis()) / (1000L * 60 * 60 * 24)
        val meses = (dias / 30).toInt().coerceAtLeast(1)
        val mensual = restante / meses

        findViewById<TextView>(R.id.tvObjetivo).text = "$${formato.format(monto.toInt())}"
        findViewById<TextView>(R.id.tvInicial).text = "$${formato.format(inicial.toInt())}"
        findViewById<TextView>(R.id.tvRestante).text = "$${formato.format(restante.toInt())}"
        findViewById<TextView>(R.id.tvTiempo).text = if (meses == 1) "1 mes" else "$meses meses"
        findViewById<TextView>(R.id.tvMensual).text = "$${formato.format(mensual.toInt())} / mes"
        findViewById<TextView>(R.id.tvSemanal).text = "o $${formato.format((mensual / 4).toInt())} por semana"

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnModificar).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnAceptar).setOnClickListener {
            viewModel.crearMeta(nombre, categoria, monto, inicial, fecha)
        }

        viewModel.goalState.observe(this) { estado ->
            when (estado) {
                is GoalState.Loading -> loadingOverlay.visibility = View.VISIBLE
                is GoalState.MetaCreada -> {
                    startActivity(
                        Intent(this, GoalsListActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    )
                    finish()
                }
                is GoalState.ErrorCampos -> {
                    loadingOverlay.visibility = View.GONE
                    tvError.text = estado.errores.values.firstOrNull() ?: "Error"
                    tvError.visibility = View.VISIBLE
                }
                is GoalState.Error -> {
                    loadingOverlay.visibility = View.GONE
                    tvError.text = estado.mensaje
                    tvError.visibility = View.VISIBLE
                }
                else -> Unit
            }
        }
    }
}