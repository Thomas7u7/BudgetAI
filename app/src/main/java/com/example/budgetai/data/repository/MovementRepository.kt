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

    /** Actualiza un movimiento existente. */
    suspend fun actualizarMovimiento(id: String, movimiento: Movement) = withTimeout(TIMEOUT_MS) {
        coleccion().document(id).set(movimiento).await()
    }

    /** Elimina un movimiento. */
    suspend fun eliminarMovimiento(id: String) = withTimeout(TIMEOUT_MS) {
        coleccion().document(id).delete().await()
    }

    /** Calcula el saldo total (ingresos - gastos) de todos los movimientos. */
    suspend fun calcularSaldo(): Double = withTimeout(TIMEOUT_MS) {
        val movimientos = obtenerMovimientos()
        movimientos.sumOf { if (it.tipo == "ingreso") it.monto else -it.monto }
    }
}