package com.example.budgetai.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class SavingsGoal(
    val id: String = "",
    val nombre: String = "",
    val montoObjetivo: Double = 0.0,
    val montoAhorrado: Double = 0.0,
    @PropertyName("fechaLimite")
    val fechaLimite: Timestamp = Timestamp.now(),
    val frecuencia: String = "Semanal", // "Semanal" | "Quincenal" | "Mensual"
    val montoRecomendado: Double = 0.0,
    val activa: Boolean = false,
    val completada: Boolean = false,
    @PropertyName("creadaEn")
    val creadaEn: Timestamp? = null
) {
    constructor() : this("", "", 0.0, 0.0, Timestamp.now(), "Semanal", 0.0, false, false, null)

    /** Progreso de la meta, como porcentaje de 0 a 100. */
    fun progreso(): Int {
        if (montoObjetivo <= 0) return 0
        return ((montoAhorrado / montoObjetivo) * 100).toInt().coerceIn(0, 100)
    }

    /** Cuánto dinero falta para alcanzar el objetivo. */
    fun montoFaltante(): Double = (montoObjetivo - montoAhorrado).coerceAtLeast(0.0)
}

data class SavingsMovement(
    val id: String = "",
    val tipo: String = "deposito", // "deposito" | "retiro"
    val monto: Double = 0.0,
    val saldoResultante: Double = 0.0,
    @PropertyName("fecha")
    val fecha: Timestamp = Timestamp.now()
) {
    constructor() : this("", "deposito", 0.0, 0.0, Timestamp.now())
}