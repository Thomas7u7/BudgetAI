package com.example.budgetai

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
import com.example.budgetai.data.model.InformacionFinanciera
import com.example.budgetai.data.model.UserState
import com.example.budgetai.ui.user.UserViewModel
import com.example.budgetai.util.TipoBanner
import com.example.budgetai.util.mostrarBanner
import com.example.budgetai.util.ocultarBanner

class FinancialInfoActivity : AppCompatActivity() {

    private val viewModel: UserViewModel by viewModels()

    private lateinit var etIngreso: EditText
    private lateinit var etAhorroEsperado: EditText
    private lateinit var spFrecuencia: Spinner
    private lateinit var spObjetivo: Spinner
    private lateinit var btnGuardar: Button
    private lateinit var loadingOverlay: LinearLayout
    private lateinit var statusBanner: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_financial_info)

        etIngreso = findViewById(R.id.etIngreso)
        etAhorroEsperado = findViewById(R.id.etAhorroEsperado)
        spFrecuencia = findViewById(R.id.spFrecuencia)
        spObjetivo = findViewById(R.id.spObjetivo)
        btnGuardar = findViewById(R.id.btnGuardar)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        statusBanner = findViewById(R.id.statusBanner)

        configurarSpinnerOscuro(spFrecuencia, R.array.frecuencias_ingreso)
        configurarSpinnerOscuro(spObjetivo, R.array.objetivos_financieros)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        btnGuardar.setOnClickListener {
            statusBanner.ocultarBanner()
            validarYGuardar()
        }

        observarEstado()
        viewModel.cargarPerfil()
    }

    private fun validarYGuardar() {
        val ingresoTexto = etIngreso.text.toString().trim()
        val ahorroTexto = etAhorroEsperado.text.toString().trim()
        val objetivo = spObjetivo.selectedItem?.toString() ?: ""

        val ingreso = ingresoTexto.toDoubleOrNull() ?: 0.0
        val ahorro = ahorroTexto.toDoubleOrNull() ?: 0.0

        // Validación: no permitir guardar si todos los campos están vacíos
        if (ingreso <= 0 && ahorro <= 0 && objetivo.isBlank()) {
            statusBanner.mostrarBanner(
                "Completa al menos tu ingreso mensual para continuar",
                TipoBanner.ERROR
            )
            etIngreso.setBackgroundResource(R.drawable.bg_field_error)
            return
        }

        // Validación: el ingreso mensual es obligatorio (es la base de todo lo demás)
        if (ingreso <= 0) {
            statusBanner.mostrarBanner(
                "El ingreso mensual es obligatorio",
                TipoBanner.ERROR
            )
            etIngreso.setBackgroundResource(R.drawable.bg_field_error)
            return
        }

        etIngreso.setBackgroundResource(R.drawable.bg_field)

        // Validación: el ahorro esperado no puede superar el ingreso mensual
        if (ahorro > ingreso) {
            etAhorroEsperado.setBackgroundResource(R.drawable.bg_field_error)
            statusBanner.mostrarBanner(
                "El ahorro no puede superar tu ingreso mensual ($${ingreso.toInt()})",
                TipoBanner.ERROR
            )
            return
        }

        etAhorroEsperado.setBackgroundResource(R.drawable.bg_field)

        val frecuencia = spFrecuencia.selectedItem?.toString() ?: "Mensual"

        val info = InformacionFinanciera(
            ingresoMensual = ingreso,
            frecuenciaIngresos = frecuencia,
            objetivoFinanciero = objetivo,
            ahorroMensualEsperado = ahorro
        )

        viewModel.actualizarInformacionFinanciera(info)
    }

    /** Evalúa si los 4 campos tienen datos válidos. */
    private fun infoEstaCompleta(info: InformacionFinanciera?): Boolean {
        return info != null &&
                info.ingresoMensual > 0 &&
                info.frecuenciaIngresos.isNotBlank() &&
                info.objetivoFinanciero.isNotBlank() &&
                info.ahorroMensualEsperado > 0
    }

    private fun observarEstado() {
        viewModel.userState.observe(this) { estado ->
            when (estado) {
                is UserState.Loading -> {
                    loadingOverlay.visibility = View.VISIBLE
                }

                is UserState.PerfilCargado -> {
                    loadingOverlay.visibility = View.GONE
                    val info = estado.usuario.informacionFinanciera

                    if (info != null) {
                        if (info.ingresoMensual > 0) {
                            etIngreso.setText(info.ingresoMensual.toInt().toString())
                        }
                        if (info.ahorroMensualEsperado > 0) {
                            etAhorroEsperado.setText(info.ahorroMensualEsperado.toInt().toString())
                        }
                        seleccionarEnSpinner(spFrecuencia, info.frecuenciaIngresos)
                        seleccionarEnSpinner(spObjetivo, info.objetivoFinanciero)
                    }

                    if (infoEstaCompleta(info)) {
                        statusBanner.mostrarBanner(
                            "¡BudgetAI está listo! Ya podemos generar recomendaciones personalizadas basadas en tu información financiera.",
                            TipoBanner.EXITO
                        )
                    } else {
                        statusBanner.mostrarBanner(
                            "Faltan datos por completar. Completa tu información financiera para que BudgetAI pueda personalizar tus recomendaciones.",
                            TipoBanner.ADVERTENCIA
                        )
                    }
                }

                is UserState.GuardadoExitoso -> {
                    loadingOverlay.visibility = View.GONE
                    btnGuardar.text = "Continuar"
                    statusBanner.mostrarBanner(
                        "¡BudgetAI está listo! Ya podemos generar recomendaciones personalizadas basadas en tu información financiera.",
                        TipoBanner.EXITO
                    )
                }

                is UserState.Error -> {
                    loadingOverlay.visibility = View.GONE
                    statusBanner.mostrarBanner(estado.mensaje, TipoBanner.ERROR)
                }

                else -> Unit
            }
        }
    }

    private fun configurarSpinnerOscuro(spinner: Spinner, arrayResId: Int) {
        val opciones = resources.getStringArray(arrayResId)
        val adapter = object : ArrayAdapter<String>(
            this, android.R.layout.simple_spinner_item, opciones
        ) {
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.setTextColor(resources.getColor(R.color.text_primary, theme))
                view.setPadding(0, 0, 0, 0)
                view.gravity = android.view.Gravity.CENTER_VERTICAL
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.setTextColor(resources.getColor(R.color.text_primary, theme))
                view.setBackgroundColor(resources.getColor(R.color.field_bg, theme))
                val padPx = (14 * resources.displayMetrics.density).toInt()
                val padHorizontalPx = (16 * resources.displayMetrics.density).toInt()
                view.setPadding(padHorizontalPx, padPx, padHorizontalPx, padPx)
                view.gravity = android.view.Gravity.CENTER_VERTICAL
                return view
            }
        }
        spinner.adapter = adapter
    }

    private fun seleccionarEnSpinner(spinner: Spinner, valor: String) {
        if (valor.isBlank()) return
        val adapter = spinner.adapter as? ArrayAdapter<*> ?: return
        val posicion = (0 until adapter.count).firstOrNull { adapter.getItem(it)?.toString() == valor }
        if (posicion != null) {
            spinner.setSelection(posicion)
        }
    }
}