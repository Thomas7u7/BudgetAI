package com.example.budgetai

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.budgetai.data.model.Preferencias
import com.example.budgetai.data.model.UserState
import com.example.budgetai.ui.user.UserViewModel
import com.example.budgetai.util.TipoBanner
import com.example.budgetai.util.mostrarBanner
import com.example.budgetai.util.ocultarBanner

class PreferencesActivity : AppCompatActivity() {

    private val viewModel: UserViewModel by viewModels()

    private lateinit var spMoneda: Spinner
    private lateinit var spIdioma: Spinner
    private lateinit var switchAlertas: Switch
    private lateinit var switchRecomendaciones: Switch
    private lateinit var switchRecordatorios: Switch
    private lateinit var btnGuardar: Button
    private lateinit var loadingOverlay: LinearLayout
    private lateinit var statusBanner: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preferences)

        spMoneda = findViewById(R.id.spMoneda)
        spIdioma = findViewById(R.id.spIdioma)
        switchAlertas = findViewById(R.id.switchAlertas)
        switchRecomendaciones = findViewById(R.id.switchRecomendaciones)
        switchRecordatorios = findViewById(R.id.switchRecordatorios)
        btnGuardar = findViewById(R.id.btnGuardar)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        statusBanner = findViewById(R.id.statusBanner)

        configurarSpinnerOscuro(spMoneda, R.array.monedas)
        configurarSpinnerOscuro(spIdioma, R.array.idiomas)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        btnGuardar.setOnClickListener {
            statusBanner.ocultarBanner()
            guardarPreferencias()
        }

        observarEstado()
        viewModel.cargarPerfil()
    }

    private fun guardarPreferencias() {
        val preferencias = Preferencias(
            moneda = spMoneda.selectedItem?.toString() ?: "COP - Peso Colombiano",
            idioma = spIdioma.selectedItem?.toString() ?: "Español (Latinoamérica)",
            alertasPresupuesto = switchAlertas.isChecked,
            recomendacionesIA = switchRecomendaciones.isChecked,
            recordatoriosMetas = switchRecordatorios.isChecked
        )
        viewModel.actualizarPreferencias(preferencias)
    }

    private fun observarEstado() {
        viewModel.userState.observe(this) { estado ->
            when (estado) {
                is UserState.Loading -> {
                    loadingOverlay.visibility = View.VISIBLE
                }

                is UserState.PerfilCargado -> {
                    loadingOverlay.visibility = View.GONE
                    val preferencias = estado.usuario.preferencias

                    seleccionarEnSpinner(spMoneda, preferencias.moneda)
                    seleccionarEnSpinner(spIdioma, preferencias.idioma)
                    switchAlertas.isChecked = preferencias.alertasPresupuesto
                    switchRecomendaciones.isChecked = preferencias.recomendacionesIA
                    switchRecordatorios.isChecked = preferencias.recordatoriosMetas
                }

                is UserState.GuardadoExitoso -> {
                    loadingOverlay.visibility = View.GONE
                    statusBanner.mostrarBanner("Preferencias actualizadas", TipoBanner.EXITO)
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