package com.example.budgetai.util

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.budgetai.R

enum class TipoBanner { EXITO, ERROR, ADVERTENCIA }

fun LinearLayout.mostrarBanner(mensaje: String, tipo: TipoBanner) {
    val tvIcon = findViewById<TextView>(R.id.tvBannerIcon)
    val tvMessage = findViewById<TextView>(R.id.tvBannerMessage)

    val (icono, colorRes) = when (tipo) {
        TipoBanner.EXITO -> "✓" to R.color.green_primary
        TipoBanner.ERROR -> "⚠" to R.color.error_red
        TipoBanner.ADVERTENCIA -> "⚠" to R.color.warning_amber
    }

    val color = ContextCompat.getColor(context, colorRes)
    tvIcon.text = icono
    tvIcon.setTextColor(color)
    tvMessage.text = mensaje
    tvMessage.setTextColor(color)

    visibility = View.VISIBLE
}

fun LinearLayout.ocultarBanner() {
    visibility = View.GONE
}