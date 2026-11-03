package com.example.budgetai.data.repository

import com.example.budgetai.data.model.Goal
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

private const val TIMEOUT_MS = 15_000L
const val ERROR_FONDOS = "FONDOS_INSUFICIENTES"
const val ERROR_OBJETIVO = "EXCEDE_OBJETIVO"

class GoalRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val movementRepository = MovementRepository()

    private fun uidActual(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("No hay usuario autenticado")

    private fun coleccion() =
        firestore.collection("users").document(uidActual()).collection("goals")

    // ---------- READ ----------

    /** Metas visibles: todas menos las retiradas. */
    suspend fun obtenerMetas(): List<Goal> = withTimeout(TIMEOUT_MS) {
        val snapshot = coleccion()
            .orderBy("creadaEn", Query.Direction.DESCENDING)
            .get()
            .await()
        snapshot.documents
            .mapNotNull { it.toObject(Goal::class.java)?.copy(id = it.id) }
            .filter { it.estado != "retirada" }
    }

    suspend fun obtenerMeta(id: String): Goal = withTimeout(TIMEOUT_MS) {
        val doc = coleccion().document(id).get().await()
        doc.toObject(Goal::class.java)?.copy(id = doc.id)
            ?: throw IllegalStateException("No se encontró la meta")
    }

    // ---------- CREATE ----------

    suspend fun crearMeta(meta: Goal): String = withTimeout(TIMEOUT_MS) {
        if (meta.ahorroInicial > movementRepository.calcularSaldo()) {
            throw IllegalStateException(ERROR_FONDOS)
        }
        val datos = meta.copy(
            montoAhorrado = meta.ahorroInicial,
            estado = if (meta.ahorroInicial >= meta.montoObjetivo) "completada" else "activa",
            creadaEn = Timestamp.now()
        )
        coleccion().add(datos).await().id
    }

    /** Suma dinero a la meta, descontándolo del saldo disponible. */
    suspend fun agregarDinero(id: String, monto: Double) = withTimeout(TIMEOUT_MS) {
        if (monto > movementRepository.calcularSaldo()) {
            throw IllegalStateException(ERROR_FONDOS)
        }
        val ref = coleccion().document(id)
        firestore.runTransaction { tx ->
            val snap = tx.get(ref)
            val objetivo = snap.getDouble("montoObjetivo") ?: 0.0
            val actual = snap.getDouble("montoAhorrado") ?: 0.0
            if (monto <= 0 || monto > (objetivo - actual)) {
                throw IllegalStateException(ERROR_OBJETIVO)
            }
            val nuevo = actual + monto
            tx.update(ref, "montoAhorrado", nuevo)
            if (objetivo > 0 && nuevo >= objetivo) tx.update(ref, "estado", "completada")
            nuevo
        }.await()
    }

    // ---------- UPDATE ----------

    suspend fun actualizarMeta(
        id: String,
        nombre: String,
        categoria: String,
        montoObjetivo: Double,
        fechaObjetivo: Timestamp,
        ahorroMensualRecomendado: Double
    ) = withTimeout(TIMEOUT_MS) {
        val ref = coleccion().document(id)
        firestore.runTransaction { tx ->
            val ahorrado = tx.get(ref).getDouble("montoAhorrado") ?: 0.0
            tx.update(
                ref, mapOf(
                    "nombre" to nombre,
                    "categoria" to categoria,
                    "montoObjetivo" to montoObjetivo,
                    "fechaObjetivo" to fechaObjetivo,
                    "ahorroMensualRecomendado" to ahorroMensualRecomendado,
                    "estado" to if (ahorrado >= montoObjetivo) "completada" else "activa"
                )
            )
            null
        }.await()
    }

    // ---------- DELETE ----------

    /** Eliminación lógica: la meta deja de verse y su dinero vuelve al saldo. */
    suspend fun retirarMeta(id: String) = withTimeout(TIMEOUT_MS) {
        coleccion().document(id).update("estado", "retirada").await()
    }

    /** Eliminación real del documento. El dinero también vuelve al saldo. */
    suspend fun eliminarMeta(id: String) = withTimeout(TIMEOUT_MS) {
        coleccion().document(id).delete().await()
    }
}