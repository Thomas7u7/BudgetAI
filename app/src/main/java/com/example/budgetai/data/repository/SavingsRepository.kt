package com.example.budgetai.data.repository

import com.example.budgetai.data.model.SavingsGoal
import com.example.budgetai.data.model.SavingsMovement
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

private const val TIMEOUT_MS = 15_000L
private const val MAX_INTENTOS_PIN = 3
private const val MINUTOS_BLOQUEO = 15L
private const val PIN_ITERACIONES = 100_000
private const val PIN_PREFIJO = "pbkdf2"

class SavingsRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private fun uidActual(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("No hay usuario autenticado")

    private fun coleccionMetas() =
        firestore.collection("users").document(uidActual()).collection("savingsGoals")

    private fun documentoUsuario() =
        firestore.collection("users").document(uidActual())

    // ---------- CRUD de metas ----------

    suspend fun obtenerMetas(): List<SavingsGoal> = withTimeout(TIMEOUT_MS) {
        val snapshot = coleccionMetas()
            .orderBy("creadaEn", Query.Direction.DESCENDING)
            .get()
            .await()
        snapshot.documents.mapNotNull { doc ->
            doc.toObject(SavingsGoal::class.java)?.copy(id = doc.id)
        }
    }

    suspend fun obtenerMetaActiva(): SavingsGoal? = withTimeout(TIMEOUT_MS) {
        val snapshot = coleccionMetas()
            .whereEqualTo("activa", true)
            .limit(1)
            .get()
            .await()
        snapshot.documents.firstOrNull()?.let { doc ->
            doc.toObject(SavingsGoal::class.java)?.copy(id = doc.id)
        }
    }

    suspend fun obtenerMeta(id: String): SavingsGoal = withTimeout(TIMEOUT_MS) {
        val doc = coleccionMetas().document(id).get().await()
        doc.toObject(SavingsGoal::class.java)?.copy(id = doc.id)
            ?: throw IllegalStateException("No se encontró la meta")
    }

    suspend fun crearMeta(meta: SavingsGoal): String = withTimeout(TIMEOUT_MS) {
        // Si es la primera meta del usuario, se marca como activa automáticamente
        val metasExistentes = obtenerMetas()
        val datos = meta.copy(
            creadaEn = Timestamp.now(),
            activa = metasExistentes.isEmpty()
        )
        val ref = coleccionMetas().add(datos).await()
        ref.id
    }

    suspend fun marcarComoActiva(id: String) = withTimeout(TIMEOUT_MS) {
        // Desactiva todas las demás y activa solo esta
        val metas = obtenerMetas()
        val batch = firestore.batch()
        metas.forEach { meta ->
            batch.update(coleccionMetas().document(meta.id), "activa", meta.id == id)
        }
        batch.commit().await()
    }

    suspend fun actualizarMeta(id: String, meta: SavingsGoal) = withTimeout(TIMEOUT_MS) {
        val ref = coleccionMetas().document(id)
        firestore.runTransaction { tx ->
            val existente = tx.get(ref).toObject(SavingsGoal::class.java)
                ?: throw IllegalStateException("No se encontró la meta")
            // Conserva los campos que no se editan (activa, creadaEn y el dinero realmente ahorrado).
            tx.set(
                ref,
                meta.copy(
                    id = "",
                    montoAhorrado = existente.montoAhorrado,
                    activa = existente.activa,
                    completada = existente.montoAhorrado >= meta.montoObjetivo,
                    creadaEn = existente.creadaEn
                )
            )
            null
        }.await()
    }

    suspend fun eliminarMeta(id: String) = withTimeout(TIMEOUT_MS) {
        coleccionMetas().document(id).delete().await()
    }

    // ---------- Depósitos y retiros ----------

    suspend fun depositar(metaId: String, monto: Double): Double = withTimeout(TIMEOUT_MS) {
        val metaRef = coleccionMetas().document(metaId)
        val nuevoSaldo = firestore.runTransaction { transaction ->
            val snapshot = transaction.get(metaRef)
            val saldoActual = snapshot.getDouble("montoAhorrado") ?: 0.0
            val objetivo = snapshot.getDouble("montoObjetivo") ?: 0.0
            val nuevo = saldoActual + monto
            transaction.update(metaRef, "montoAhorrado", nuevo)
            if (nuevo >= objetivo && objetivo > 0) {
                transaction.update(metaRef, "completada", true)
            }
            nuevo
        }.await()

        registrarMovimiento(metaId, "deposito", monto, nuevoSaldo)
        nuevoSaldo
    }

    suspend fun retirar(metaId: String, monto: Double): Double = withTimeout(TIMEOUT_MS) {
        val metaRef = coleccionMetas().document(metaId)
        val nuevoSaldo = firestore.runTransaction { transaction ->
            val snapshot = transaction.get(metaRef)
            val saldoActual = snapshot.getDouble("montoAhorrado") ?: 0.0
            if (monto > saldoActual) {
                throw IllegalStateException("No puedes retirar más de lo que tienes ahorrado")
            }
            val nuevo = saldoActual - monto
            transaction.update(metaRef, "montoAhorrado", nuevo)
            nuevo
        }.await()

        registrarMovimiento(metaId, "retiro", monto, nuevoSaldo)
        nuevoSaldo
    }

    private suspend fun registrarMovimiento(metaId: String, tipo: String, monto: Double, saldoResultante: Double) {
        val movimiento = SavingsMovement(
            tipo = tipo,
            monto = monto,
            saldoResultante = saldoResultante,
            fecha = Timestamp.now()
        )
        coleccionMetas().document(metaId).collection("movements").add(movimiento).await()
    }

    suspend fun obtenerHistorial(metaId: String): List<SavingsMovement> = withTimeout(TIMEOUT_MS) {
        val snapshot = coleccionMetas().document(metaId).collection("movements")
            .orderBy("fecha", Query.Direction.DESCENDING)
            .get()
            .await()
        snapshot.documents.mapNotNull { doc ->
            doc.toObject(SavingsMovement::class.java)?.copy(id = doc.id)
        }
    }

    // ---------- PIN de seguridad ----------

    /** Hash heredado sin sal; solo se usa para verificar y migrar PIN antiguos. */
    private fun sha256Legado(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hashPin(pin: String, salt: ByteArray, iteraciones: Int = PIN_ITERACIONES): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iteraciones, 256)
        val hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
        spec.clearPassword()
        return "$PIN_PREFIJO:$iteraciones:${Base64.encodeToString(salt, Base64.NO_WRAP)}:${Base64.encodeToString(hash, Base64.NO_WRAP)}"
    }

    private fun nuevoHashPin(pin: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return hashPin(pin, salt)
    }

    private fun verificarHash(pin: String, guardado: String): Boolean {
        if (!guardado.startsWith("$PIN_PREFIJO:")) return guardado == sha256Legado(pin)
        val partes = guardado.split(":")
        if (partes.size != 4) return false
        val iteraciones = partes[1].toIntOrNull() ?: return false
        return try {
            val salt = Base64.decode(partes[2], Base64.NO_WRAP)
            val esperado = Base64.decode(partes[3], Base64.NO_WRAP)
            val calculado = hashPin(pin, salt, iteraciones).split(":").last()
            MessageDigest.isEqual(esperado, Base64.decode(calculado, Base64.NO_WRAP))
        } catch (e: Exception) {
            false
        }
    }

    suspend fun tienePinConfigurado(): Boolean = withTimeout(TIMEOUT_MS) {
        val snapshot = documentoUsuario().get().await()
        val hash = snapshot.getString("seguridad.pinAhorroHash")
        !hash.isNullOrBlank()
    }

    suspend fun configurarPin(pin: String) = withTimeout(TIMEOUT_MS) {
        val hash = nuevoHashPin(pin)
        documentoUsuario().update(
            mapOf(
                "seguridad.pinAhorroHash" to hash,
                "seguridad.intentosFallidosPin" to 0,
                "seguridad.bloqueadoHasta" to null
            )
        ).await()
    }

    /**
     * Verifica el PIN ingresado. Devuelve true si es correcto.
     * Lanza excepción si la cuenta está bloqueada temporalmente.
     */
    suspend fun verificarPin(pin: String): Boolean = withTimeout(TIMEOUT_MS) {
        val snapshot = documentoUsuario().get().await()
        val hashGuardado = snapshot.getString("seguridad.pinAhorroHash")
        val bloqueadoHasta = snapshot.getTimestamp("seguridad.bloqueadoHasta")

        if (bloqueadoHasta != null && bloqueadoHasta.toDate().after(java.util.Date())) {
            throw IllegalStateException("BLOQUEADO")
        }

        val esCorrecto = hashGuardado != null && verificarHash(pin, hashGuardado)

        if (esCorrecto) {
            val actualizaciones = mutableMapOf<String, Any?>(
                "seguridad.intentosFallidosPin" to 0,
                "seguridad.bloqueadoHasta" to null
            )
            // Migra el hash heredado (SHA-256 sin sal) a PBKDF2 con sal.
            if (!hashGuardado!!.startsWith("$PIN_PREFIJO:")) {
                actualizaciones["seguridad.pinAhorroHash"] = nuevoHashPin(pin)
            }
            documentoUsuario().update(actualizaciones).await()
        } else {
            val intentosActuales = (snapshot.getLong("seguridad.intentosFallidosPin") ?: 0).toInt() + 1
            val actualizaciones = mutableMapOf<String, Any>(
                "seguridad.intentosFallidosPin" to intentosActuales
            )
            if (intentosActuales >= MAX_INTENTOS_PIN) {
                val bloqueoHasta = Timestamp(Timestamp.now().seconds + MINUTOS_BLOQUEO * 60, 0)
                actualizaciones["seguridad.bloqueadoHasta"] = bloqueoHasta
            }
            documentoUsuario().update(actualizaciones).await()
        }

        esCorrecto
    }

    suspend fun obtenerIntentosFallidos(): Int = withTimeout(TIMEOUT_MS) {
        val snapshot = documentoUsuario().get().await()
        (snapshot.getLong("seguridad.intentosFallidosPin") ?: 0).toInt()
    }
}