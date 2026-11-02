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
    private var goalIdEditar: String? = null
    private var ahorradoActual = 0.0
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

        goalIdEditar = intent.getStringExtra("goalId")
        if (goalIdEditar != null) {
            findViewById<TextView>(R.id.tvTituloPantalla).text = "Modificar meta"
            etAhorroInicial.isEnabled = false // solo cambia con "Agregar dinero"
        }

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
        goalIdEditar?.let { viewModel.cargarMeta(it) }
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
        val nombre = etNombre.text.toString().trim()
        val monto = etMonto.text.toString().toDoubleOrNull()
        val fecha = fechaElegida?.let { Timestamp(it.time) }

        val id = goalIdEditar
        if (id != null) {
            viewModel.actualizarMeta(id, nombre, categoria, monto, ahorradoActual, fecha)
        } else {
            viewModel.generarPlan(
                nombre = nombre,
                categoria = categoria,
                monto = monto,
                ahorroInicial = etAhorroInicial.text.toString().toDoubleOrNull(),
                fecha = fecha
            )
        }
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
                is GoalState.DetalleCargado -> {
                    loadingOverlay.visibility = View.GONE
                    precargarMeta(estado.meta)
                }

                is GoalState.MetaActualizada -> {
                    loadingOverlay.visibility = View.GONE
                    finish() // vuelve al detalle, que se recarga solo en onResume
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
    private fun precargarMeta(meta: com.example.budgetai.data.model.Goal) {
        etNombre.setText(meta.nombre)
        categoria = meta.categoria
        tvCategoria.text = "${SelectCategoryActivity.emojiDe(meta.categoria)} ${meta.categoria}"
        etMonto.setText(meta.montoObjetivo.toInt().toString())
        ahorradoActual = meta.montoAhorrado
        etAhorroInicial.setText(meta.montoAhorrado.toInt().toString())
        fechaElegida = Calendar.getInstance().apply { time = meta.fechaObjetivo.toDate() }
        tvFecha.text = formatoFecha.format(fechaElegida!!.time)
    }

    private fun watcher(onChanged: () -> Unit) = object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = onChanged()
        override fun afterTextChanged(s: android.text.Editable?) = Unit
    }
}