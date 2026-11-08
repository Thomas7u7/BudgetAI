package com.example.budgetai.ui.savings

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetai.R
import com.example.budgetai.data.model.ChatConversation
import java.text.SimpleDateFormat
import java.util.Locale

class ChatConversationAdapter(
    private var conversaciones: List<ChatConversation>,
    private var idActual: String?,
    private val onClick: (ChatConversation) -> Unit,
    private val onBorrar: (ChatConversation) -> Unit
) : RecyclerView.Adapter<ChatConversationAdapter.ViewHolder>() {

    private val formatoFecha = SimpleDateFormat("d MMM, HH:mm", Locale("es", "CO"))

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitulo: TextView = view.findViewById(R.id.tvTitulo)
        val tvBadgeActual: TextView = view.findViewById(R.id.tvBadgeActual)
        val tvVistaPrevia: TextView = view.findViewById(R.id.tvVistaPrevia)
        val tvFecha: TextView = view.findViewById(R.id.tvFecha)
        val ivBorrar: ImageView = view.findViewById(R.id.ivBorrar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_conversation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val conversacion = conversaciones[position]
        holder.tvTitulo.text = conversacion.titulo
        holder.tvBadgeActual.visibility =
            if (conversacion.id == idActual) View.VISIBLE else View.GONE
        holder.tvVistaPrevia.text =
            conversacion.ultimoMensaje.ifBlank { "Sin mensajes todavía" }
        holder.tvFecha.text = conversacion.actualizado
            ?.let { formatoFecha.format(it.toDate()) }
            .orEmpty()

        holder.itemView.setOnClickListener { onClick(conversacion) }
        holder.ivBorrar.setOnClickListener { onBorrar(conversacion) }
    }

    override fun getItemCount() = conversaciones.size

    @SuppressLint("NotifyDataSetChanged")
    fun actualizarLista(nuevas: List<ChatConversation>) {
        conversaciones = nuevas
        notifyDataSetChanged()
    }
}
