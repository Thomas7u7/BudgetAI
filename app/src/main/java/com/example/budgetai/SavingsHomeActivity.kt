package com.example.budgetai

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.budgetai.data.model.SavingsGoal
import com.example.budgetai.data.model.SavingsState
import com.example.budgetai.ui.savings.SavingsViewModel
import java.text.NumberFormat
import java.util.Locale

class SavingsHomeActivity : AppCompatActivity() {

    private val viewModel: SavingsViewModel by viewModels()

    private lateinit var tvSaldoTotal: TextView
    private lateinit var cardMetaActiva: LinearLayout
    private lateinit var emptyState: LinearLayout
    private lateinit var tvNombreMeta: TextView
    private lateinit var tvPorcentajeMeta: TextView
    private lateinit var progressMeta: ProgressBar
    private lateinit var tvAhorradoMeta: TextView
    private lateinit var tvFaltanteMeta: TextView
    private lateinit var tvConsejoIA: TextView

    private val formatoMoneda = NumberFormat.getInstance(Locale("es", "CO"))
    private var metaActivaId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_savings_home)

        tvSaldoTotal = findViewById(R.id.tvSaldoTotal)
        cardMetaActiva = findViewById(R.id.cardMetaActiva)
        emptyState = findViewById(R.id.emptyState)
        tvNombreMeta = findViewById(R.id.tvNombreMeta)
        tvPorcentajeMeta = findViewById(R.id.tvPorcentajeMeta)
        progressMeta = findViewById(R.id.progressMeta)
        tvAhorradoMeta = findViewById(R.id.tvAhorradoMeta)
        tvFaltanteMeta = findViewById(R.id.tvFaltanteMeta)
        tvConsejoIA = findViewById(R.id.tvConsejoIA)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<TextView>(R.id.btnAgregar).setOnClickListener {
            irADepositar()
        }
        findViewById<TextView>(R.id.btnRetirar).setOnClickListener {
            irARetirar()
        }
        findViewById<TextView>(R.id.btnHistorial).setOnClickListener {
            irAHistorial()
        }
        findViewById<TextView>(R.id.btnCrearMetaVacio).setOnClickListener {
            startActivity(Intent(this, CreateSavingsGoalActivity::class.java))
        }

        cardMetaActiva.setOnClickListener {
            metaActivaId?.let { id ->
                val intent = Intent(this, SavingsGoalDetailActivity::class.java)
                intent.putExtra("goalId", id)
                startActivity(intent)
            }
        }

        // Bottom nav
        findViewById<LinearLayout>(R.id.navMiAhorro).setOnClickListener {
            // ya estamos aquí
        }
        findViewById<LinearLayout>(R.id.navMetas).setOnClickListener {
            startActivity(Intent(this, SavingsGoalsListActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navAhorroIA).setOnClickListener {
            startActivity(Intent(this, SavingsAIActivity::class.java))
        }

        observarEstado()
    }

    override fun onResume() {
        super.onResume()
        viewModel.cargarMetaActiva()
    }

    private fun irADepositar() {
        if (metaActivaId == null) {
            Toast.makeText(this, "Primero crea una meta de ahorro", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, DepositSavingsActivity::class.java)
        intent.putExtra("goalId", metaActivaId)
        startActivity(intent)
    }

    private fun irARetirar() {
        if (metaActivaId == null) {
            Toast.makeText(this, "Primero crea una meta de ahorro", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, WithdrawSavingsActivity::class.java)
        intent.putExtra("goalId", metaActivaId)
        startActivity(intent)
    }

    private fun irAHistorial() {
        if (metaActivaId == null) {
            Toast.makeText(this, "Primero crea una meta de ahorro", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, SavingsHistoryActivity::class.java)
        intent.putExtra("goalId", metaActivaId)
        startActivity(intent)
    }

    private fun observarEstado() {
        viewModel.savingsState.observe(this) { estado ->
            when (estado) {
                is SavingsState.DetalleCargado -> {
                    mostrarMetaActiva(estado.meta)
                }

                is SavingsState.ListaCargada -> {
                    // Lista vacía = no hay meta activa
                    metaActivaId = null
                    cardMetaActiva.visibility = android.view.View.GONE
                    emptyState.visibility = android.view.View.VISIBLE
                    tvSaldoTotal.text = "$0 COP"
                    tvConsejoIA.text = "Crea tu primera meta para empezar a recibir consejos personalizados."
                }

                is SavingsState.Error -> {
                    Toast.makeText(this, estado.mensaje, Toast.LENGTH_LONG).show()
                }

                else -> Unit
            }
        }
    }

    private fun mostrarMetaActiva(meta: SavingsGoal) {
        metaActivaId = meta.id
        cardMetaActiva.visibility = android.view.View.VISIBLE
        emptyState.visibility = android.view.View.GONE

        tvSaldoTotal.text = "$${formatoMoneda.format(meta.montoAhorrado.toInt())} COP"
        tvNombreMeta.text = meta.nombre
        val progreso = meta.progreso()
        tvPorcentajeMeta.text = "$progreso%"
        progressMeta.progress = progreso
        tvAhorradoMeta.text = "Ahorrado\n$${formatoMoneda.format(meta.montoAhorrado.toInt())}"
        tvFaltanteMeta.text = "Dinero faltante\n$${formatoMoneda.format(meta.montoFaltante().toInt())}"

        tvConsejoIA.text = generarConsejoSimulado(meta)
    }

    /** Consejo simulado (sin IA real) basado en reglas simples sobre el progreso de la meta. */
    private fun generarConsejoSimulado(meta: SavingsGoal): String {
        val progreso = meta.progreso()
        return when {
            progreso >= 90 -> "¡Estás a un paso de lograr \"${meta.nombre}\"! Un último esfuerzo."
            progreso >= 50 -> "Vas muy bien con \"${meta.nombre}\". Sigue ahorrando ${formatoMoneda.format(meta.montoRecomendado.toInt())} por ${meta.frecuencia.lowercase()}."
            progreso > 0 -> "Buen comienzo con \"${meta.nombre}\". Mantén el ritmo para llegar a tiempo."
            else -> "Aún no has agregado dinero a \"${meta.nombre}\". ¡Haz tu primer depósito hoy!"
        }
    }
}