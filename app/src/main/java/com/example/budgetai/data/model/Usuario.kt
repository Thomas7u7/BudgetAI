package com.example.budgetai.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Usuario(
    val uid: String = "",
    val nombreCompleto: String = "",
    val correo: String = "",
    val telefono: String? = null,
    val fotoBase64: String? = null,
    val moneda: String = "COP",
    @PropertyName("fechaCreacion")
    val fechaCreacion: Timestamp? = null,
    @PropertyName("ultimoAcceso")
    val ultimoAcceso: Timestamp? = null,
    val informacionFinanciera: InformacionFinanciera? = null,
    val preferencias: Preferencias = Preferencias(),
    val seguridad: Seguridad = Seguridad()
) {
    constructor() : this(
        "", "", "", null, null, "COP", null, null, null, Preferencias(), Seguridad()
    )
}

data class InformacionFinanciera(
    val ingresoMensual: Double = 0.0,
    val frecuenciaIngresos: String = "Mensual",
    val objetivoFinanciero: String = "",
    val ahorroMensualEsperado: Double = 0.0
) {
    constructor() : this(0.0, "Mensual", "", 0.0)
}

data class Preferencias(
    val moneda: String = "COP",
    val idioma: String = "Español",
    val alertasPresupuesto: Boolean = true,
    val recomendacionesIA: Boolean = true,
    val recordatoriosMetas: Boolean = false
) {
    constructor() : this("COP", "Español", true, true, false)
}

data class Seguridad(
    val biometriaHabilitada: Boolean = false,
    val pinAhorroHash: String? = null,
    val intentosFallidosPin: Int = 0,
    val bloqueadoHasta: Timestamp? = null
) {
    constructor() : this(false, null, 0, null)
}