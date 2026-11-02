package com.example.budgetai

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SelectCategoryActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CATEGORIA = "categoria"

        fun emojiDe(categoria: String): String = when (categoria) {
            "Viajes" -> "✈️"
            "Tecnología" -> "💻"
            "Hogar" -> "🏠"
            "Vehículo" -> "🚗"
            "Educación" -> "🎓"
            "Salud" -> "🏥"
            "Emergencia" -> "🚨"
            "Eventos" -> "🎉"
            "Compras personales" -> "🛍️"
            "Mascotas" -> "🐾"
            "Inversión" -> "📈"
            "Ahorro libre" -> "💰"
            else -> "✏️"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_category)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        val seleccionada = intent.getStringExtra(EXTRA_CATEGORIA) ?: ""
        val categorias = resources.getStringArray(R.array.categorias_meta).toList()

        val rv = findViewById<RecyclerView>(R.id.rvCategorias)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                object : RecyclerView.ViewHolder(
                    LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
                ) {}

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val nombre = categorias[position]
                val v = holder.itemView
                v.findViewById<TextView>(R.id.tvEmoji).text = emojiDe(nombre)
                v.findViewById<TextView>(R.id.tvNombre).text = nombre
                v.findViewById<TextView>(R.id.tvCheck).visibility =
                    if (nombre == seleccionada) View.VISIBLE else View.GONE
                v.setOnClickListener {
                    setResult(RESULT_OK, Intent().putExtra(EXTRA_CATEGORIA, nombre))
                    finish()
                }
            }

            override fun getItemCount() = categorias.size
        }
    }
}