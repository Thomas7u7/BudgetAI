package com.example.budgetai

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetai.data.model.SavingsGoal
import com.example.budgetai.data.model.SavingsState
import com.example.budgetai.ui.savings.ChatAdapter
import com.example.budgetai.ui.savings.ChatMessage
import com.example.budgetai.ui.savings.SavingsViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class SavingsAIActivity : AppCompatActivity() {

    private val viewModel: SavingsViewModel by viewModels()

    private lateinit var rvChat: RecyclerView
    private lateinit var etMensaje: EditText
    private lateinit var adapter: ChatAdapter

    private var metaActiva: SavingsGoal? = null
    private val formatoMoneda = NumberFormat.getInstance(Locale("es", "CO"))
    private val formatoFecha = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es", "CO"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_savings_ai)

        rvChat = findViewById(R.id.rvChat)
        etMensaje = findViewById(R.id.etMensaje)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        adapter = ChatAdapter(mutableListOf())
        rvChat.layoutManager = LinearLayoutManager(this)
        rvChat.adapter = adapter

        findViewById<TextView>(R.id.btnEnviar).setOnClickListener { enviarMensaje() }
        findViewById<TextView>(R.id.btnSugerencia1).setOnClickListener {
            etMensaje.setText("¿Cómo voy con mi meta del auto y cuál es el plan de ahorro sugerido?")
            enviarMensaje()
        }
        findViewById<TextView>(R.id.btnSugerencia2).setOnClickListener {
            etMensaje.setText("¿Cuánto me falta para mi ahorro y cuándo termino?")
            enviarMensaje()
        }

        // Bottom nav
        findViewById<LinearLayout>(R.id.navMiAhorro).setOnClickListener {
            startActivity(Intent(this, SavingsHomeActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.navMetas).setOnClickListener {
            startActivity(Intent(this, SavingsGoalsListActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navAhorroIA).setOnClickListener {
            // ya estamos aquí
        }

        observarEstado()
        viewModel.cargarMetaActiva()

        mostrarMensajeBienvenida()
    }

    private fun mostrarMensajeBienvenida() {
        adapter.agregarMensaje(
            ChatMessage(
                "Hola, soy tu asistente de ahorros. Cuéntame qué necesitas saber sobre tu meta y te ayudo con sugerencias.",
                esUsuario = false
            )
        )
    }

    private fun enviarMensaje() {
        val texto = etMensaje.text.toString().trim()
        if (texto.isEmpty()) return

        adapter.agregarMensaje(ChatMessage(texto, esUsuario = true))
        etMensaje.text.clear()
        rvChat.scrollToPosition(adapter.itemCount - 1)

        val respuesta = generarRespuesta(texto)
        rvChat.postDelayed({
            adapter.agregarMensaje(ChatMessage(respuesta, esUsuario = false))
            rvChat.scrollToPosition(adapter.itemCount - 1)
        }, 500)
    }

    /**
     * Genera una respuesta simulada por reglas simples, usando los datos reales
     * de la meta activa. No es un modelo de IA real — es una simulación por palabras clave,
     * acorde al alcance definido para esta versión del proyecto.
     */
    private fun generarRespuesta(mensaje: String): String {
        val meta = metaActiva
            ?: return "Aún no tienes una meta de ahorro activa. Crea una desde \"Mi Ahorro\" para que pueda ayudarte con sugerencias."

        val textoLower = mensaje.lowercase()
        val progreso = meta.progreso()

        return when {
            textoLower.contains("cuánto") && (textoLower.contains("falta") || textoLower.contains("falto")) -> {
                "Para tu meta \"${meta.nombre}\":\n" +
                        "• Te faltan $${formatoMoneda.format(meta.montoFaltante().toInt())} COP\n" +
                        "• Ahorrando $${formatoMoneda.format(meta.montoRecomendado.toInt())} por ${meta.frecuencia.lowercase()}, tu fecha estimada de logro es el ${formatoFecha.format(meta.fechaLimite.toDate())}."
            }

            textoLower.contains("cómo voy") || textoLower.contains("progreso") || textoLower.contains("plan") -> {
                "Hola, actualmente tu meta \"${meta.nombre}\" está al $progreso% con un objetivo de $${formatoMoneda.format(meta.montoObjetivo.toInt())} COP.\n\n" +
                        "• Ahorrando $${formatoMoneda.format(meta.montoAhorrado.toInt())}, tu fecha estimada de logro es el ${formatoFecha.format(meta.fechaLimite.toDate())}\n" +
                        "• Consejo: programar depósitos pequeños de $${formatoMoneda.format(meta.montoRecomendado.toInt())} te ayudaría a construir el hábito más rápido y sin esfuerzo."
            }

            progreso >= 90 ->
                "¡Vas excelente con \"${meta.nombre}\"! Estás al $progreso% de tu meta. Un último esfuerzo y lo logras."

            progreso >= 50 ->
                "Vas muy bien con \"${meta.nombre}\" ($progreso%). Si recibes algún ingreso extra, un abono adicional ayudaría a acelerar el logro de tu meta."

            progreso > 0 ->
                "Tu meta \"${meta.nombre}\" va al $progreso%. Te recomiendo mantener depósitos de $${formatoMoneda.format(meta.montoRecomendado.toInt())} por ${meta.frecuencia.lowercase()} para llegar a tiempo."

            else ->
                "Aún no has agregado dinero a \"${meta.nombre}\". Te sugiero hacer tu primer depósito hoy mismo para empezar a construir el hábito."
        }
    }

    private fun observarEstado() {
        viewModel.savingsState.observe(this) { estado ->
            if (estado is SavingsState.DetalleCargado) {
                metaActiva = estado.meta
            }
        }
    }
}