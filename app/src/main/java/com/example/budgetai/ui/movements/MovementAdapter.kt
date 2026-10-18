package com.example.budgetai.ui.movements

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetai.R
import com.example.budgetai.data.model.Movement
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class MovementAdapter(
    private var movimientos: List<Movement>,
    private val onClick: (Movement) -> Unit
) : RecyclerView.Adapter<MovementAdapter.MovementViewHolder>() {

    private val formatoMoneda = NumberFormat.getInstance(Locale("es", "CO"))
    private val formatoFecha = SimpleDateFormat("dd MMM yyyy", Locale("es", "CO"))

    inner class MovementViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcono: TextView = view.findViewById(R.id.tvIcono)
        val tvCategoria: TextView = view.findViewById(R.id.tvCategoria)
        val tvFecha: TextView = view.findViewById(R.id.tvFecha)
        val tvMonto: TextView = view.findViewById(R.id.tvMonto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovementViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_movement, parent, false)
        return MovementViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovementViewHolder, position: Int) {
        val movimiento = movimientos[position]
        val esIngreso = movimiento.tipo == "ingreso"

        holder.tvIcono.text = iconoPorCategoria(movimiento.categoria)
        holder.tvCategoria.text = movimiento.categoria
        holder.tvFecha.text = formatoFecha.format(movimiento.fecha.toDate())

        val montoFormateado = "$${formatoMoneda.format(movimiento.monto.toInt())}"
        holder.tvMonto.text = if (esIngreso) "+$montoFormateado" else "-$montoFormateado"
        holder.tvMonto.setTextColor(
            holder.itemView.context.getColor(
                if (esIngreso) R.color.green_primary else R.color.error_red
            )
        )

        holder.itemView.setOnClickListener { onClick(movimiento) }
    }

    override fun getItemCount() = movimientos.size

    fun actualizarLista(nuevaLista: List<Movement>) {
        movimientos = nuevaLista
        notifyDataSetChanged()
    }

    private fun iconoPorCategoria(categoria: String): String = when (categoria) {
        "Salario" -> "💼"
        "Freelance / Proyecto" -> "💻"
        "Inversiones" -> "📈"
        "Regalo" -> "🎁"
        "Reembolso" -> "↩️"
        "Alimentación" -> "🍽️"
        "Transporte" -> "🚗"
        "Vivienda / Arriendo" -> "🏠"
        "Servicios públicos" -> "💡"
        "Salud" -> "🏥"
        "Entretenimiento" -> "🎬"
        "Educación" -> "📚"
        "Ropa" -> "👕"
        "Suscripciones" -> "📱"
        else -> "💰"
    }
}