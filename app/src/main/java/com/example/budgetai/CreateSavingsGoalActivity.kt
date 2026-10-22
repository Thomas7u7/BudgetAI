package com.example.budgetai

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.budgetai.data.model.SavingsState
import com.example.budgetai.ui.savings.SavingsViewModel
import com.example.budgetai.util.TipoBanner
import com.example.budgetai.util.mostrarBanner
import com.example.budgetai.util.ocultarBanner
import com.google.firebase.Timestamp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CreateSavingsGoalActivity : AppCompatActivity() {

    private val viewModel: SavingsViewModel by viewModels()

    private lateinit var etNombre: EditText
    private lateinit var etMontoObjetivo: EditText
    private lateinit var tvFechaLimite: TextView
    private lateinit var btnSemanal: TextView
    private lateinit var btnQuincenal: TextView
    private lateinit var btnMensual: TextView
    private lateinit var tvPlanRecomendado: TextView
    private lateinit var tvPlanDetalle: TextView
    private lateinit var btnCrear: Button
    private lateinit var loadingOverlay: LinearLayout
    private lateinit var statusBanner: LinearLayout

    private var frecuenciaSeleccionada = "Semanal"
    private val calendarioSeleccionado = Calendar.getInstance().apply {
        add(Calendar.MONTH, 6) // fecha límite por defecto: 6 meses adelante
    }
    private val formatoFechaVisual = SimpleDateFormat("dd MMM yyyy", Locale("es", "CO"))
    private val formatoMoneda = NumberFormat.getInstance(Locale("es", "CO"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_savings_goal)

        etNombre = findViewById(R.id.etNombre)
        etMontoObjetivo = findViewById(R.id.etMontoObjetivo)
        tvFechaLimite = findViewById(R.id.tvFechaLimite)
        btnSemanal = findViewById(R.id.btnSemanal)
        btnQuincenal = findViewById(R.id.btnQuincenal)
        btnMensual = findViewById(R.id.btnMensual)
        tvPlanRecomendado = findViewById(R.id.tvPlanRecomendado)
        tvPlanDetalle = findViewById(R.id.tvPlanDetalle)
        btnCrear = findViewById(R.id.btnCrear)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        statusBanner = findViewById(R.id.statusBanner)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        tvFechaLimite.text = formatoFechaVisual.format(calendarioSeleccionado.time)
        tvFechaLimite.setOnClickListener { mostrarSelectorFecha() }

        btnSemanal.setOnClickListener { seleccionarFrecuencia("Semanal") }
        btnQuincenal.setOnClickListener { seleccionarFrecuencia("Quincenal") }
        btnMensual.setOnClickListener { seleccionarFrecuencia("Mensual") }

        etMontoObjetivo.addTextChangedListener(simpleWatcher { actualizarPlanRecomendado() })

        btnCrear.setOnClickListener { crearMeta() }

        actualizarPlanRecomendado()
        observarEstado()
    }

    private fun seleccionarFrecuencia(frecuencia: String) {
        frecuenciaSeleccionada = frecuencia
        val botones = listOf(btnSemanal to "Semanal", btnQuincenal to "Quincenal", btnMensual to "Mensual")
        botones.forEach { (boton, nombre) ->
            val seleccionado = nombre == frecuencia
            boton.setBackgroundResource(if (seleccionado) R.drawable.bg_button_primary else 0)
            boton.setTextColor(getColor(if (seleccionado) R.color.black else R.color.text_secondary))
        }
        actualizarPlanRecomendado()
    }

    private fun mostrarSelectorFecha() {
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendarioSeleccionado.set(year, month, dayOfMonth)
                tvFechaLimite.text = formatoFechaVisual.format(calendarioSeleccionado.time)
                actualizarPlanRecomendado()
            },
            calendarioSeleccionado.get(Calendar.YEAR),
            calendarioSeleccionado.get(Calendar.MONTH),
            calendarioSeleccionado.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    /** Recalcula en vivo cuánto habría que ahorrar por periodo, según el monto y la fecha elegidos. */
    private fun actualizarPlanRecomendado() {
        val monto = etMontoObjetivo.text.toString().toDoubleOrNull() ?: 0.0
        val milisRestantes = calendarioSeleccionado.timeInMillis - System.currentTimeMillis()
        val diasRestantes = (milisRestantes / (1000L * 60 * 60 * 24)).coerceAtLeast(1)

        val periodos = when (frecuenciaSeleccionada) {
            "Semanal" -> (diasRestantes / 7).coerceAtLeast(1)
            "Quincenal" -> (diasRestantes / 15).coerceAtLeast(1)
            else -> (diasRestantes / 30).coerceAtLeast(1)
        }

        val montoPorPeriodo = if (monto > 0) monto / periodos else 0.0
        val etiquetaPeriodo = when (frecuenciaSeleccionada) {
            "Semanal" -> "semana"
            "Quincenal" -> "quincena"
            else -> "mes"
        }

        tvPlanRecomendado.text = "$${formatoMoneda.format(montoPorPeriodo.toInt())} por $etiquetaPeriodo"
        tvPlanDetalle.text = "Monto calculado para $periodos ${if (etiquetaPeriodo == "mes") "meses" else "${etiquetaPeriodo}s"} restantes"
    }

    private fun crearMeta() {
        statusBanner.ocultarBanner()
        val nombre = etNombre.text.toString().trim()
        val monto = etMontoObjetivo.text.toString().toDoubleOrNull() ?: 0.0
        val fecha = Timestamp(calendarioSeleccionado.time)

        viewModel.crearMeta(nombre, monto, fecha, frecuenciaSeleccionada)
    }

    private fun observarEstado() {
        viewModel.savingsState.observe(this) { estado ->
            when (estado) {
                is SavingsState.Loading -> {
                    loadingOverlay.visibility = View.VISIBLE
                }

                is SavingsState.GuardadoExitoso -> {
                    loadingOverlay.visibility = View.GONE
                    statusBanner.mostrarBanner("¡Meta creada exitosamente!", TipoBanner.EXITO)
                    btnCrear.postDelayed({
                        val intent = Intent(this, SavingsHomeActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        startActivity(intent)
                        finish()
                    }, 1000)
                }

                is SavingsState.Error -> {
                    loadingOverlay.visibility = View.GONE
                    statusBanner.mostrarBanner(estado.mensaje, TipoBanner.ERROR)
                }

                else -> Unit
            }
        }
    }

    private fun simpleWatcher(onChanged: () -> Unit) = object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = onChanged()
        override fun afterTextChanged(s: android.text.Editable?) = Unit
    }
}