package com.example.budgetai.data.repository

import com.example.budgetai.data.model.Movement
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

private const val TIMEOUT_MS = 15_000L

class MovementRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private fun uidActual(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("No hay usuario autenticado")

    private fun coleccion() =
        firestore.collection("users").document(uidActual()).collection("movements")

    /** Trae todos los movimientos del usuario, ordenados del más reciente al más antiguo. */
    suspend fun obtenerMovimientos(): List<Movement> = withTimeout(TIMEOUT_MS) {
        val snapshot = coleccion()
            .orderBy("fecha", Query.Direction.DESCENDING)
            .get()
            .await()

        snapshot.documents.mapNotNull { doc ->
            doc.toObject(Movement::class.java)?.copy(id = doc.id)
        }
    }

    /** Trae un movimiento específico por su id. */
    suspend fun obtenerMovimiento(id: String): Movement = withTimeout(TIMEOUT_MS) {
        val doc = coleccion().document(id).get().await()
        doc.toObject(Movement::class.java)?.copy(id = doc.id)
            ?: throw IllegalStateException("No se encontró el movimiento")
    }

    /** Crea un nuevo movimiento. */
    suspend fun crearMovimiento(movimiento: Movement) = withTimeout(TIMEOUT_MS) {
        val datos = movimiento.copy(creadoEn = Timestamp.now())
        coleccion().add(datos).await()
    }

    /** Actualiza un movimiento existente sin perder creadoEn. */
    suspend fun actualizarMovimiento(id: String, movimiento: Movement) = withTimeout(TIMEOUT_MS) {
        coleccion().document(id).update(
            mapOf(
                "tipo" to movimiento.tipo,
                "monto" to movimiento.monto,
                "categoria" to movimiento.categoria,
                "descripcion" to movimiento.descripcion,
                "fecha" to movimiento.fecha
            )
        ).await()
    }

    /** Elimina un movimiento. */
    suspend fun eliminarMovimiento(id: String) = withTimeout(TIMEOUT_MS) {
        coleccion().document(id).delete().await()
    }

    /** Saldo disponible = ingresos - gastos. */
    /** Saldo disponible = ingresos - gastos - dinero apartado en metas activas/completadas. */
    suspend fun calcularSaldo(): Double = withTimeout(TIMEOUT_MS) {
        val movimientos = obtenerMovimientos()
        val totalIngresos = movimientos.filter { it.tipo.equals("ingreso", ignoreCase = true) }.sumOf { it.monto }
        val totalGastos = movimientos.filter { it.tipo.equals("gasto", ignoreCase = true) }.sumOf { it.monto }

        val usuarioRef = firestore.collection("users").document(uidActual())
        val perfilSnap = usuarioRef.get().await()
        val ingresoPerfil = perfilSnap.getDouble("informacionFinanciera.ingresoMensual") ?: 0.0

        // Dinero que ya está dentro de las metas (las retiradas devuelven su dinero al saldo)
        val metasSnap = usuarioRef.collection("goals").get().await()
        val totalEnMetas = metasSnap.documents
            .filter { it.getString("estado") != "retirada" }
            .sumOf { it.getDouble("montoAhorrado") ?: 0.0 }

        val ingresosBase = if (totalIngresos > 0) totalIngresos else ingresoPerfil
        val neto = ingresosBase - totalGastos - totalEnMetas
        if (neto < 0 && totalIngresos == 0.0) 0.0 else neto
    }
}