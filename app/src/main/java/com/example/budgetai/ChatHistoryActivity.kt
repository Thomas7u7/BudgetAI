package com.example.budgetai

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetai.data.model.ChatConversation
import com.example.budgetai.ui.savings.ChatConversationAdapter
import com.example.budgetai.ui.savings.ChatHistoryViewModel

/**
 * Lista de conversaciones con BudgetIA. El usuario elige una para retomarla,
 * crea una nueva o borra las que ya no quiere. El resultado vuelve a SavingsAIActivity.
 */
class ChatHistoryActivity : AppCompatActivity() {

    private val viewModel: ChatHistoryViewModel by viewModels()

    private lateinit var adapter: ChatConversationAdapter
    private lateinit var tvVacio: TextView
    private lateinit var progress: ProgressBar

    /** Conversación abierta en el chat cuando se entró aquí. */
    private var chatActualId: String? = null

    /** Si el usuario borró la conversación abierta, el chat debe pasar a uno nuevo al volver. */
    private var actualBorrada = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_history)

        chatActualId = intent.getStringExtra(SavingsAIActivity.EXTRA_CHAT_ID)
        actualBorrada = savedInstanceState?.getBoolean(ESTADO_ACTUAL_BORRADA) ?: false

        tvVacio = findViewById(R.id.tvVacio)
        progress = findViewById(R.id.progress)

        adapter = ChatConversationAdapter(
            conversaciones = emptyList(),
            idActual = chatActualId,
            onClick = { conversacion -> abrir(conversacion.id) },
            onBorrar = { conversacion -> confirmarBorrado(conversacion) }
        )
        findViewById<RecyclerView>(R.id.rvConversaciones).apply {
            layoutManager = LinearLayoutManager(this@ChatHistoryActivity)
            adapter = this@ChatHistoryActivity.adapter
        }

        findViewById<TextView>(R.id.btnBack).setOnClickListener { cerrar() }
        findViewById<TextView>(R.id.btnNuevoChat).setOnClickListener { nueva() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = cerrar()
        })

        observar()
        viewModel.cargar()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(ESTADO_ACTUAL_BORRADA, actualBorrada)
    }

    private fun observar() {
        viewModel.conversaciones.observe(this) { lista ->
            adapter.actualizarLista(lista)
            tvVacio.visibility =
                if (lista.isEmpty() && viewModel.cargando.value != true) View.VISIBLE else View.GONE
        }
        viewModel.cargando.observe(this) { cargando ->
            progress.visibility = if (cargando) View.VISIBLE else View.GONE
            if (cargando) tvVacio.visibility = View.GONE
        }
        viewModel.error.observe(this) { mensaje ->
            if (mensaje == null) return@observe
            Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
            viewModel.consumirError()
        }
        viewModel.idBorrado.observe(this) { id ->
            if (id == null) return@observe
            if (id == chatActualId) actualBorrada = true
            viewModel.consumirIdBorrado()
        }
    }

    private fun confirmarBorrado(conversacion: ChatConversation) {
        AlertDialog.Builder(this)
            .setTitle("¿Borrar esta conversación?")
            .setMessage("Se eliminarán todos los mensajes de «${conversacion.titulo}». No se puede deshacer.")
            .setPositiveButton("Borrar") { _, _ -> viewModel.borrar(conversacion.id) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /** Retoma una conversación existente. */
    private fun abrir(chatId: String) {
        setResult(RESULT_OK, Intent().putExtra(SavingsAIActivity.EXTRA_CHAT_ID, chatId))
        finish()
    }

    /** Empieza una conversación nueva. */
    private fun nueva() {
        setResult(RESULT_OK, Intent().putExtra(SavingsAIActivity.EXTRA_NUEVO, true))
        finish()
    }

    /** Vuelve al chat sin elegir nada (salvo que se haya borrado la conversación abierta). */
    private fun cerrar() {
        if (actualBorrada) {
            setResult(RESULT_OK, Intent().putExtra(SavingsAIActivity.EXTRA_NUEVO, true))
        } else {
            setResult(RESULT_CANCELED)
        }
        finish()
    }

    private companion object {
        const val ESTADO_ACTUAL_BORRADA = "actual_borrada"
    }
}
