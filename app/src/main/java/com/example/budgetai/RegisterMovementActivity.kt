package com.example.budgetai

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.budgetai.data.model.Movement
import com.example.budgetai.data.model.MovementState
import com.example.budgetai.ui.movements.MovementViewModel
import com.example.budgetai.util.TipoBanner
import com.example.budgetai.util.mostrarBanner
import com.example.budgetai.util.ocultarBanner
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RegisterMovementActivity : AppCompatActivity() {

    private val viewModel: MovementViewModel by viewModels()

    private lateinit var tvTitulo: TextView
    private lateinit var btnTipoIngreso: TextView
    private lateinit var btnTipoGasto: TextView
    private lateinit var etMonto: EditText
    private lateinit var spCategoria: Spinner
    private lateinit var etDescripcion: EditText
    private lateinit var tvFecha: TextView
    private lateinit var btnGuardar: Button
    private lateinit var loadingOverlay: LinearLayout
    private lateinit var tvLoadingText: TextView
    private lateinit var statusBanner: LinearLayout

    private var tipoSeleccionado = "ingreso"
    private val calendarioSeleccionado = Calendar.getInstance()
    private val formatoFechaVisual = SimpleDateFormat("dd MMM yyyy", Locale("es", "CO"))

    private var movementIdEditar: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_movement)

        tvTitulo = findViewById(R.id.tvTitulo)
        btnTipoIngreso = findViewById(R.id.btnTipoIngreso)
        btnTipoGasto = findViewById(R.id.btnTipoGasto)
        etMonto = findViewById(R.id.etMonto)
        spCategoria = findViewById(R.id.spCategoria)
        etDescripcion = findViewById(R.id.etDescripcion)
        tvFecha = findViewById(R.id.tvFecha)
        btnGuardar = findViewById(R.id.btnGuardar)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        tvLoadingText = findViewById(R.id.tvLoadingText)
        statusBanner = findViewById(R.id.statusBanner)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        movementIdEditar = intent.getStringExtra("movementId")
        if (movementIdEditar != null) {
            tvTitulo.text = "Editar movimiento"
            btnGuardar.text = "Guardar cambios"
            tvLoadingText.text = "Guardando cambios..."
        }

        btnTipoIngreso.setOnClickListener { seleccionarTipo("ingreso") }
        btnTipoGasto.setOnClickListener { seleccionarTipo("gasto") }

        tvFecha.text = formatoFechaVisual.format(calendarioSeleccionado.time)
        tvFecha.setOnClickListener { mostrarSelectorFecha() }

        btnGuardar.setOnClickListener { guardar() }

        actualizarCategorias()
        observarEstado()

        if (movementIdEditar != null) {
            viewModel.cargarMovimiento(movementIdEditar!!)
        }
    }

    private fun seleccionarTipo(tipo: String) {
        tipoSeleccionado = tipo
        val esIngreso = tipo == "ingreso"

        btnTipoIngreso.setBackgroundResource(if (esIngreso) R.drawable.bg_button_primary else 0)
        btnTipoIngreso.setTextColor(getColor(if (esIngreso) R.color.black else R.color.text_secondary))

        btnTipoGasto.setBackgroundResource(if (!esIngreso) R.drawable.bg_button_primary else 0)
        btnTipoGasto.setTextColor(getColor(if (!esIngreso) R.color.black else R.color.text_secondary))

        actualizarCategorias()
    }

    private fun actualizarCategorias() {
        val arrayId = if (tipoSeleccionado == "ingreso") R.array.categorias_ingreso else R.array.categorias_gasto
        val opciones = resources.getStringArray(arrayId)
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, opciones) {
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.setTextColor(getColor(R.color.text_primary))
                view.setPadding(0, 0, 0, 0)
                view.gravity = android.view.Gravity.CENTER_VERTICAL
                return view
            }
            override fun getDropDownView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.setTextColor(getColor(R.color.text_primary))
                view.setBackgroundColor(getColor(R.color.field_bg))
                val padPx = (14 * resources.displayMetrics.density).toInt()
                val padHorizontalPx = (16 * resources.displayMetrics.density).toInt()
                view.setPadding(padHorizontalPx, padPx, padHorizontalPx, padPx)
                view.gravity = android.view.Gravity.CENTER_VERTICAL
                return view
            }
        }
        spCategoria.adapter = adapter
    }

    private fun mostrarSelectorFecha() {
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendarioSeleccionado.set(year, month, dayOfMonth)
                tvFecha.text = formatoFechaVisual.format(calendarioSeleccionado.time)
            },
            calendarioSeleccionado.get(Calendar.YEAR),
            calendarioSeleccionado.get(Calendar.MONTH),
            calendarioSeleccionado.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun guardar() {
        statusBanner.ocultarBanner()
        val montoTexto = etMonto.text.toString().trim()
        val monto = montoTexto.toDoubleOrNull() ?: 0.0
        val categoria = spCategoria.selectedItem?.toString() ?: ""
        val descripcion = etDescripcion.text.toString().trim()
        val fecha = Timestamp(calendarioSeleccionado.time)

        viewModel.guardarMovimiento(
            idExistente = movementIdEditar,
            tipo = tipoSeleccionado,
            monto = monto,
            categoria = categoria,
            descripcion = descripcion,
            fecha = fecha
        )
    }

    private fun observarEstado() {
        viewModel.movementState.observe(this) { estado ->
            when (estado) {
                is MovementState.Loading -> {
                    loadingOverlay.visibility = View.VISIBLE
                }

                is MovementState.DetalleCargado -> {
                    loadingOverlay.visibility = View.GONE
                    precargarMovimiento(estado.movimiento)
                }

                is MovementState.GuardadoExitoso -> {
                    loadingOverlay.visibility = View.GONE
                    statusBanner.mostrarBanner(
                        if (movementIdEditar != null) "Cambios guardados correctamente" else "Movimiento registrado correctamente",
                        TipoBanner.EXITO
                    )
                    btnGuardar.postDelayed({ finish() }, 1200)
                }

                is MovementState.Error -> {
                    loadingOverlay.visibility = View.GONE
                    statusBanner.mostrarBanner(estado.mensaje, TipoBanner.ERROR)
                }

                else -> Unit
            }
        }
    }

    private fun precargarMovimiento(movimiento: Movement) {
        seleccionarTipo(movimiento.tipo)
        etMonto.setText(movimiento.monto.toInt().toString())
        etDescripcion.setText(movimiento.descripcion)
        calendarioSeleccionado.time = movimiento.fecha.toDate()
        tvFecha.text = formatoFechaVisual.format(calendarioSeleccionado.time)

        // Espera a que el adapter de categorías esté listo antes de seleccionar
        spCategoria.post {
            val adapter = spCategoria.adapter as? ArrayAdapter<String> ?: return@post
            val posicion = (0 until adapter.count).firstOrNull { adapter.getItem(it) == movimiento.categoria }
            if (posicion != null) spCategoria.setSelection(posicion)
        }
    }

}