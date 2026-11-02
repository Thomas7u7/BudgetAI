package com.example.budgetai.ui.goals

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetai.R
import com.example.budgetai.SelectCategoryActivity
import com.example.budgetai.data.model.Goal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class GoalAdapter(
    private var metas: List<Goal>,
    private val onClick: (Goal) -> Unit
) : RecyclerView.Adapter<GoalAdapter.GoalViewHolder>() {

    private val formato = NumberFormat.getInstance(Locale("es", "CO"))
    private val formatoFecha = SimpleDateFormat("dd MMM yyyy", Locale("es", "CO"))

    inner class GoalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombre)
        val tvPorcentaje: TextView = view.findViewById(R.id.tvPorcentaje)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
        val tvDetalle: TextView = view.findViewById(R.id.tvDetalle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder =
        GoalViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_goal, parent, false))

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        val meta = metas[position]
        holder.tvNombre.text = "${SelectCategoryActivity.emojiDe(meta.categoria)} ${meta.nombre}"
        holder.tvPorcentaje.text = "${meta.progreso()}%"
        holder.progressBar.progress = meta.progreso()
        holder.tvDetalle.text =
            "Dinero ahorrado\n$${formato.format(meta.montoAhorrado.toInt())}\n" +
                    "Objetivo: $${formato.format(meta.montoObjetivo.toInt())}\n" +
                    "Dinero restante: $${formato.format(meta.montoFaltante().toInt())}\n" +
                    "Fecha objetivo: ${formatoFecha.format(meta.fechaObjetivo.toDate())}"
        holder.itemView.setOnClickListener { onClick(meta) }
    }

    override fun getItemCount() = metas.size

    fun actualizarLista(nueva: List<Goal>) {
        metas = nueva
        notifyDataSetChanged()
    }
}