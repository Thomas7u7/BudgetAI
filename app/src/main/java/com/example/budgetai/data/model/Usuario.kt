package com.example.budgetai.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Usuario(
    val uid: String = "",
    val nombreCompleto: String = "",
    val correo: String = "",
    val fotoUrl: String? = null,
    val moneda: String = "COP",
    @PropertyName("fechaCreacion")
    val fechaCreacion: Timestamp? = null,
    @PropertyName("ultimoAcceso")
    val ultimoAcceso: Timestamp? = null
) {
    // Firestore necesita un constructor vacío para deserializar
    constructor() : this("", "", "", null, "COP", null, null)
}