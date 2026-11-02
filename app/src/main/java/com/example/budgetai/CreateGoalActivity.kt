package com.example.budgetai

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import com.example.budgetai.data.model.GoalState
import com.example.budgetai.ui.goals.GoalViewModel
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CreateGoalActivity : AppCompatActivity() {

    private val viewModel: GoalViewModel by viewModels()

    private lateinit var etNombre: EditText
    private lateinit var tvCategoria: TextView
    private lateinit var etMonto: EditText
    private lateinit var etAhorroInicial: EditText
    private lateinit var tvFecha: TextView
    private lateinit var loadingOverlay: LinearLayout

    private var categoria = ""
    private var fechaElegida: Calendar? = null
    private val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale("es", "CO"))

    private val selectorCategoria =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            result.data?.getStringExtra(SelectCategoryActivity.EXTRA_CATEGORIA)?.let {
                categoria = it
                tvCategoria.text = "${SelectCategoryActivity.emojiDe(it)} $it"
                limpiarError("categoria")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_goal)

        etNombre = findViewById(R.id.etNombre)
        tvCategoria = findViewById(R.id.tvCategoria)
        etMonto = findViewById(R.id.etMonto)
        etAhorroInicial = findViewById(R.id.etAhorroInicial)
        tvFecha = findViewById(R.id.tvFecha)
        loadingOverlay = findViewById(R.id.loadingOverlay)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        tvCategoria.setOnClickListener {
            selectorCategoria.launch(
                Intent(this, SelectCategoryActivity::class.java)
                    .putExtra(SelectCategoryActivity.EXTRA_CATEGORIA, categoria)
            )
        }
        tvFecha.setOnClickListener { mostrarSelectorFecha() }

        findViewById<TextView>(R.id.btnContinuar).setOnClickListener { continuar() }

        // El error de cada campo desaparece al corregirlo
        etNombre.addTextChangedListener(watcher { limpiarError("nombre") })
        etMonto.addTextChangedListener(watcher { limpiarError("monto") })
        etAhorroInicial.addTextChangedListener(watcher { limpiarError("ahorroInicial") })

        observarEstado()
    }

    private fun mostrarSelectorFecha() {
        val base = fechaElegida ?: Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            fechaElegida = Calendar.getInstance().apply { set(y, m, d) }
            tvFecha.text = formatoFecha.format(fechaElegida!!.time)
            limpiarError("fecha")
        }, base.get(Calendar.YEAR), base.get(Calendar.MONTH), base.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun continuar() {
        viewModel.generarPlan(
            nombre = etNombre.text.toString().trim(),
            categoria = categoria,
            monto = etMonto.text.toString().toDoubleOrNull(),
            ahorroInicial = etAhorroInicial.text.toString().toDoubleOrNull(),
            fecha = fechaElegida?.let { Timestamp(it.time) }
        )
    }

    private fun observarEstado() {
        viewModel.goalState.observe(this) { estado ->
            when (estado) {
                is GoalState.Loading -> loadingOverlay.visibility = View.VISIBLE

                is GoalState.ErrorCampos -> {
                    loadingOverlay.visibility = View.GONE
                    mostrarErrores(estado.errores)
                }

                is GoalState.PlanGenerado -> {
                    loadingOverlay.visibility = View.GONE
                    startActivity(
                        Intent(this, GoalAnalysisActivity::class.java)
                            .putExtra("nombre", etNombre.text.toString().trim())
                            .putExtra("categoria", categoria)
                            .putExtra("monto", etMonto.text.toString().toDouble())
                            .putExtra("inicial", etAhorroInicial.text.toString().toDouble())
                            .putExtra("fechaMillis", fechaElegida!!.timeInMillis)
                    )
                }

                is GoalState.Error -> loadingOverlay.visibility = View.GONE

                else -> Unit
            }
        }
    }

    // ---------- Errores por campo ----------

    private fun campo(clave: String): Pair<View, TextView>? = when (clave) {
        "nombre" -> etNombre to findViewById(R.id.errNombre)
        "categoria" -> tvCategoria to findViewById(R.id.errCategoria)
        "monto" -> etMonto to findViewById(R.id.errMonto)
        "ahorroInicial" -> etAhorroInicial to findViewById(R.id.errAhorroInicial)
        "fecha" -> tvFecha to findViewById(R.id.errFecha)
        else -> null
    }

    private fun mostrarErrores(errores: Map<String, String>) {
        errores.forEach { (clave, mensaje) ->
            campo(clave)?.let { (vista, tvError) ->
                vista.setBackgroundResource(R.drawable.bg_field_error)
                tvError.text = "⚠ $mensaje"
                tvError.visibility = View.VISIBLE
            }
        }
    }

    private fun limpiarError(clave: String) {
        campo(clave)?.let { (vista, tvError) ->
            vista.setBackgroundResource(R.drawable.bg_field)
            tvError.visibility = View.GONE
        }
    }

    private fun watcher(onChanged: () -> Unit) = object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = onChanged()
        override fun afterTextChanged(s: android.text.Editable?) = Unit
    }
}