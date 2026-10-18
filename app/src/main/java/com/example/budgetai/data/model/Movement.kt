package com.example.budgetai.data.model


import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Movement(
    val id: String = "",
    val tipo: String = "gasto", // "ingreso" | "gasto"
    val monto: Double = 0.0,
    val categoria: String = "",
    val descripcion: String = "",
    @PropertyName("fecha")
    val fecha: Timestamp = Timestamp.now(),
    @PropertyName("creadoEn")
    val creadoEn: Timestamp? = null
) {
    constructor() : this("", "gasto", 0.0, "", "", Timestamp.now(), null)
}