package com.example.budgetai.ui.savings

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetai.R

class ChatAdapter(private val mensajes: MutableList<ChatMessage>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TIPO_USUARIO = 0
        private const val TIPO_IA = 1
    }

    override fun getItemViewType(position: Int): Int =
        if (mensajes[position].esUsuario) TIPO_USUARIO else TIPO_IA

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layout = if (viewType == TIPO_USUARIO) R.layout.item_chat_user else R.layout.item_chat_ia
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return object : RecyclerView.ViewHolder(view) {}
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder.itemView.findViewById<TextView>(R.id.tvMensaje).text = mensajes[position].texto
    }

    override fun getItemCount() = mensajes.size

    fun agregarMensaje(mensaje: ChatMessage) {
        mensajes.add(mensaje)
        notifyItemInserted(mensajes.size - 1)
    }
}