package com.example.budgetai.data.model

import com.google.firebase.Timestamp

data class Goal(
    val id: String = "",
    val nombre: String = "",
    val categoria: String = "",
    val montoObjetivo: Double = 0.0,
    val ahorroInicial: Double = 0.0,
    val montoAhorrado: Double = 0.0,
    val fechaObjetivo: Timestamp = Timestamp.now(),
    val ahorroMensualRecomendado: Double = 0.0,
    val estado: String = "activa", // "activa" | "completada" | "retirada"
    val creadaEn: Timestamp? = null
) {
    constructor() : this("", "", "", 0.0, 0.0, 0.0, Timestamp.now(), 0.0, "activa", null)

    fun progreso(): Int {
        if (montoObjetivo <= 0) return 0
        return ((montoAhorrado / montoObjetivo) * 100).toInt().coerceIn(0, 100)
    }

    fun montoFaltante(): Double = (montoObjetivo - montoAhorrado).coerceAtLeast(0.0)
}