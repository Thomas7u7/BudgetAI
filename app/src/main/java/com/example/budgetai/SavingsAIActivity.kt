package com.example.budgetai

import android.content.Intent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetai.ui.savings.ChatAdapter
import com.example.budgetai.ui.savings.ChatMessage
import com.example.budgetai.ui.savings.SavingsViewModel

class SavingsAIActivity : AppCompatActivity() {

    private val viewModel: SavingsViewModel by viewModels()

    private lateinit var rvChat: RecyclerView
    private lateinit var etMensaje: EditText
    private lateinit var btnEnviar: TextView
    private lateinit var layoutSugerencias: LinearLayout
    private lateinit var adapter: ChatAdapter

    private val conversacion = mutableListOf<ChatMessage>()

    /** Hasta que no se cargue el historial guardado no se envían mensajes (evita pisar la conversación). */
    private var historialCargado = false

    companion object {
        /** Abre una conversación concreta del historial. */
        const val EXTRA_CHAT_ID = "chatId"
        /** Abre un chat vacío (la conversación se crea al enviar el primer mensaje). */
        const val EXTRA_NUEVO = "nuevo"
    }

    /** Pantalla de historial: devuelve la conversación elegida o la orden de empezar una nueva. */
    private val abrirHistorial = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        if (resultado.resultCode != RESULT_OK) return@registerForActivityResult
        val datos = resultado.data ?: return@registerForActivityResult
        cambiarConversacion(
            chatId = datos.getStringExtra(EXTRA_CHAT_ID),
            nueva = datos.getBooleanExtra(EXTRA_NUEVO, false)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_savings_ai)

        rvChat = findViewById(R.id.rvChat)
        etMensaje = findViewById(R.id.etMensaje)
        btnEnviar = findViewById(R.id.btnEnviar)
        layoutSugerencias = findViewById(R.id.layoutSugerencias)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        adapter = ChatAdapter(conversacion)
        rvChat.layoutManager = LinearLayoutManager(this)
        rvChat.adapter = adapter

        btnEnviar.setOnClickListener { enviarMensaje() }
        findViewById<TextView>(R.id.btnSugerencia1).setOnClickListener {
            etMensaje.setText("Dame 3 recomendaciones para ahorrar mejor según mis finanzas.")
            enviarMensaje()
        }
        findViewById<TextView>(R.id.btnSugerencia2).setOnClickListener {
            etMensaje.setText("¿Cómo van mis ahorros y cuánto me falta para completar mi objetivo?")
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

        findViewById<TextView>(R.id.btnHistorial).setOnClickListener { abrirPantallaHistorial() }
        findViewById<TextView>(R.id.btnNuevoChat).setOnClickListener {
            if (esperandoRespuesta()) return@setOnClickListener
            cambiarConversacion(chatId = null, nueva = true)
        }

        observarChat()

        // Retoma la conversación donde el usuario se quedó (o la que eligió en el historial).
        // Al rotar la pantalla se vuelve a la misma conversación; si era un chat nuevo sin mensajes, sigue vacío.
        val recreada = savedInstanceState != null
        val chatSolicitado = if (recreada) viewModel.chatIdActual else intent.getStringExtra(EXTRA_CHAT_ID)
        val nueva = if (recreada) viewModel.chatIdActual == null else intent.getBooleanExtra(EXTRA_NUEVO, false)
        viewModel.abrirConversacion(chatSolicitado, nueva)
        //solicitarDiagnostico()
    }

    private fun esperandoRespuesta(): Boolean {
        if (viewModel.cargandoIA.value != true) return false
        android.widget.Toast.makeText(
            this, "Espera a que BudgetIA termine de responder.", android.widget.Toast.LENGTH_SHORT
        ).show()
        return true
    }

    private fun abrirPantallaHistorial() {
        if (esperandoRespuesta()) return
        abrirHistorial.launch(
            Intent(this, ChatHistoryActivity::class.java)
                .putExtra(EXTRA_CHAT_ID, viewModel.chatIdActual)
        )
    }

    /** Cambia de conversación; no hace nada si ya es la abierta. */
    private fun cambiarConversacion(chatId: String?, nueva: Boolean) {
        if (!nueva && chatId != null && chatId == viewModel.chatIdActual) return
        historialCargado = false
        viewModel.abrirConversacion(chatId, nueva)
    }

    private fun mensajeBienvenida() = ChatMessage(
        "Hola, soy BudgetIA. Reviso tus movimientos, saldo, metas y ahorros para darte recomendaciones.",
        esUsuario = false
    )

    private fun solicitarDiagnostico() {
        viewModel.preguntarIA(
            "Haz un diagnóstico breve de mis finanzas con los datos anteriores y dame 3 recomendaciones " +
                "concretas de ahorro. No saludes, ve directo al análisis."
        )
    }

    private fun enviarMensaje() {
        if (!historialCargado || viewModel.cargandoIA.value == true) return

        val texto = etMensaje.text.toString().trim()
        if (texto.isEmpty()) return

        val mensaje = ChatMessage(texto, esUsuario = true)
        agregarMensaje(mensaje)
        viewModel.guardarMensajeChat(mensaje)
        etMensaje.text.clear()
        viewModel.preguntarIA(texto, historialTexto())
    }

    private fun observarChat() {
        viewModel.respuestaIA.observe(this) { respuesta ->
            if (respuesta == null) return@observe
            if (respuesta.error) {
                agregarMensaje(ChatMessage("⚠️ ${respuesta.texto}", esUsuario = false))
            } else {
                val regex = Regex("""\[\[MOV:(\{.*?\})\]\]""", RegexOption.DOT_MATCHES_ALL)
                val etiqueta = regex.find(respuesta.texto)
                val limpio = regex.replace(respuesta.texto, "")
                val mensajeIA = ChatMessage(textoPlano(limpio), esUsuario = false)
                agregarMensaje(mensajeIA)
                viewModel.guardarMensajeChat(mensajeIA)
                etiqueta?.let { confirmarMovimiento(it.groupValues[1]) }
            }
            viewModel.consumirRespuestaIA()
        }

        viewModel.historialChat.observe(this) { guardado ->
            if (guardado == null) return@observe
            adapter.reemplazarMensajes(if (guardado.isEmpty()) listOf(mensajeBienvenida()) else guardado)
            rvChat.scrollToPosition(adapter.itemCount - 1)
            historialCargado = true
            viewModel.consumirHistorialChat()
        }

        viewModel.cargandoIA.observe(this) { cargando ->
            btnEnviar.isEnabled = !cargando
            btnEnviar.alpha = if (cargando) 0.4f else 1f
            for (i in 0 until layoutSugerencias.childCount) {
                layoutSugerencias.getChildAt(i).isEnabled = !cargando
                layoutSugerencias.getChildAt(i).alpha = if (cargando) 0.4f else 1f
            }
        }
    }
    private fun confirmarMovimiento(json: String) {
        val obj = runCatching { org.json.JSONObject(json) }.getOrNull() ?: return
        val tipo = obj.optString("tipo").lowercase()
        val monto = obj.optDouble("monto", 0.0)
        if (tipo !in listOf("gasto", "ingreso") || monto <= 0) return
        val categoria = obj.optString("categoria").ifBlank {
            if (tipo == "gasto") "Otro gasto" else "Otro ingreso"
        }
        val descripcion = obj.optString("descripcion")
        val formato = java.text.NumberFormat.getInstance(java.util.Locale("es", "CO"))

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("¿Registrar este movimiento?")
            .setMessage("${tipo.replaceFirstChar { it.uppercase() }} de $${formato.format(monto.toLong())}\n" +
                    "Categoría: $categoria\n${descripcion}")
            .setPositiveButton("Registrar") { _, _ ->
                lifecycleScope.launch {
                    try {
                        com.example.budgetai.data.repository.MovementRepository().crearMovimiento(
                            com.example.budgetai.data.model.Movement(
                                tipo = tipo, monto = monto, categoria = categoria,
                                descripcion = descripcion,
                                fecha = com.google.firebase.Timestamp.now()
                            )
                        )
                        agregarMensaje(ChatMessage("✅ Movimiento registrado.", esUsuario = false))
                    } catch (e: Exception) {
                        agregarMensaje(ChatMessage("⚠️ No pude registrarlo. Intenta de nuevo.", esUsuario = false))
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /** El TextView no interpreta Markdown: deja viñetas legibles y quita énfasis. */
    private fun textoPlano(texto: String): String = texto
        .replace("**", "")
        .replace(Regex("(?m)^#{1,6}\\s*"), "")
        .replace(Regex("(?m)^\\s*[*•-]\\s+"), "• ")
        .trim()

    private fun agregarMensaje(mensaje: ChatMessage) {
        adapter.agregarMensaje(mensaje)
        rvChat.scrollToPosition(adapter.itemCount - 1)
    }

    /** Conversación reciente para que Gemini mantenga el hilo. */
    private fun historialTexto(): String =
        conversacion
            .filterNot { it.texto.startsWith("⚠️") }          // los avisos de error no son parte del diálogo
            .takeLast(8)
            .joinToString("\n") { mensaje ->
                val autor = if (mensaje.esUsuario) "Usuario" else "BudgetIA"
                "$autor: ${mensaje.texto.take(400)}"          // 8 × ~410 caracteres cabe en el límite del proxy (4.000)
            }
}
