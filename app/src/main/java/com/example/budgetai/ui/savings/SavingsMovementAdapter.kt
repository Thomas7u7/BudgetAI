package com.example.budgetai.ui.savings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetai.R
import com.example.budgetai.data.model.SavingsMovement
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class SavingsMovementAdapter(
    private var movimientos: List<SavingsMovement>
) : RecyclerView.Adapter<SavingsMovementAdapter.MovementViewHolder>() {

    private val formatoMoneda = NumberFormat.getInstance(Locale("es", "CO"))
    private val formatoFecha = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("es", "CO"))

    inner class MovementViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconContainer: FrameLayout = view.findViewById(R.id.iconContainer)
        val tvIcono: TextView = view.findViewById(R.id.tvIcono)
        val tvTipo: TextView = view.findViewById(R.id.tvTipo)
        val tvFecha: TextView = view.findViewById(R.id.tvFecha)
        val tvMonto: TextView = view.findViewById(R.id.tvMonto)
        val tvSaldoResultante: TextView = view.findViewById(R.id.tvSaldoResultante)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovementViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_savings_movement, parent, false)
        return MovementViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovementViewHolder, position: Int) {
        val movimiento = movimientos[position]
        val esDeposito = movimiento.tipo == "deposito"

        holder.tvIcono.text = if (esDeposito) "↓" else "↑"
        holder.tvTipo.text = if (esDeposito) "Depósito de ahorro" else "Retiro de ahorro"
        holder.tvFecha.text = formatoFecha.format(movimiento.fecha.toDate())

        val montoFormateado = "$${formatoMoneda.format(movimiento.monto.toInt())}"
        holder.tvMonto.text = if (esDeposito) "+$montoFormateado" else "-$montoFormateado"

        val color = holder.itemView.context.getColor(
            if (esDeposito) R.color.green_primary else R.color.error_red
        )
        holder.tvMonto.setTextColor(color)
        holder.tvIcono.setTextColor(color)

        holder.tvSaldoResultante.text = "Sal. $${formatoMoneda.format(movimiento.saldoResultante.toInt())}"
    }

    override fun getItemCount() = movimientos.size

    fun actualizarLista(nuevaLista: List<SavingsMovement>) {
        movimientos = nuevaLista
        notifyDataSetChanged()
    }
}