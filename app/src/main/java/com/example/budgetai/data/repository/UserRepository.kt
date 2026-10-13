package com.example.budgetai.data.repository

import com.example.budgetai.data.model.InformacionFinanciera
import com.example.budgetai.data.model.Preferencias
import com.example.budgetai.data.model.Usuario
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

private const val TIMEOUT_MS = 15_000L

class UserRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private fun uidActual(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("No hay usuario autenticado")

    suspend fun obtenerPerfil(): Usuario = withTimeout(TIMEOUT_MS) {
        val uid = uidActual()
        val snapshot = firestore.collection("users").document(uid).get().await()
        snapshot.toObject(Usuario::class.java)
            ?: throw IllegalStateException("No se encontró el perfil del usuario")
    }

    suspend fun actualizarDatosBasicos(
        nombreCompleto: String,
        telefono: String?,
        fotoBase64: String?
    ) = withTimeout(TIMEOUT_MS) {
        val uid = uidActual()
        val cambios = mutableMapOf<String, Any?>(
            "nombreCompleto" to nombreCompleto,
            "telefono" to telefono
        )
        if (fotoBase64 != null) {
            cambios["fotoBase64"] = fotoBase64
        }
        firestore.collection("users").document(uid).update(cambios).await()
    }

    suspend fun actualizarInformacionFinanciera(info: InformacionFinanciera) = withTimeout(TIMEOUT_MS) {
        val uid = uidActual()
        firestore.collection("users").document(uid)
            .update("informacionFinanciera", info)
            .await()
    }

    suspend fun actualizarPreferencias(preferencias: Preferencias) = withTimeout(TIMEOUT_MS) {
        val uid = uidActual()
        firestore.collection("users").document(uid)
            .update("preferencias", preferencias)
            .await()
    }

    suspend fun actualizarBiometria(habilitada: Boolean) = withTimeout(TIMEOUT_MS) {
        val uid = uidActual()
        firestore.collection("users").document(uid)
            .update("seguridad.biometriaHabilitada", habilitada)
            .await()
    }

    suspend fun reautenticar(passwordActual: String) = withTimeout(TIMEOUT_MS) {
        val usuario = auth.currentUser
            ?: throw IllegalStateException("No hay usuario autenticado")
        val correo = usuario.email
            ?: throw IllegalStateException("No se encontró el correo del usuario")
        val credential = EmailAuthProvider.getCredential(correo, passwordActual)
        usuario.reauthenticate(credential).await()
    }

    suspend fun cambiarContrasena(nuevaContrasena: String) = withTimeout(TIMEOUT_MS) {
        val usuario = auth.currentUser
            ?: throw IllegalStateException("No hay usuario autenticado")
        usuario.updatePassword(nuevaContrasena).await()
    }

    suspend fun eliminarCuenta() = withTimeout(TIMEOUT_MS) {
        val uid = uidActual()
        firestore.collection("users").document(uid).delete().await()
        auth.currentUser?.delete()?.await()
    }

    fun cerrarSesion() {
        auth.signOut()
    }
}