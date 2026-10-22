package com.example.budgetai.ui.savings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetai.R
import com.example.budgetai.data.model.SavingsGoal
import java.text.NumberFormat
import java.util.Locale

class SavingsGoalAdapter(
    private var metas: List<SavingsGoal>,
    private val onClick: (SavingsGoal) -> Unit
) : RecyclerView.Adapter<SavingsGoalAdapter.GoalViewHolder>() {

    private val formatoMoneda = NumberFormat.getInstance(Locale("es", "CO"))

    inner class GoalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombre)
        val tvBadgeActiva: TextView = view.findViewById(R.id.tvBadgeActiva)
        val tvPorcentaje: TextView = view.findViewById(R.id.tvPorcentaje)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
        val tvDetalle: TextView = view.findViewById(R.id.tvDetalle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_savings_goal, parent, false)
        return GoalViewHolder(view)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        val meta = metas[position]
        holder.tvNombre.text = meta.nombre
        holder.tvBadgeActiva.visibility = if (meta.activa) View.VISIBLE else View.GONE
        val progreso = meta.progreso()
        holder.tvPorcentaje.text = "$progreso%"
        holder.progressBar.progress = progreso
        holder.tvDetalle.text = "$${formatoMoneda.format(meta.montoAhorrado.toInt())} de $${formatoMoneda.format(meta.montoObjetivo.toInt())}"

        holder.itemView.setOnClickListener { onClick(meta) }
    }

    override fun getItemCount() = metas.size

    fun actualizarLista(nuevaLista: List<SavingsGoal>) {
        metas = nuevaLista
        notifyDataSetChanged()
    }
}