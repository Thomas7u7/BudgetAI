package com.example.budgetai.data.repository

import com.example.budgetai.data.model.Usuario
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

private const val TIMEOUT_MS = 15_000L

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun registrar(nombreCompleto: String, correo: String, password: String): Usuario = withTimeout(TIMEOUT_MS) {
        val resultado = auth.createUserWithEmailAndPassword(correo, password).await()
        val uid = resultado.user?.uid
            ?: throw IllegalStateException("No se pudo obtener el UID del usuario creado")

        val nuevoUsuario = Usuario(
            uid = uid,
            nombreCompleto = nombreCompleto,
            correo = correo,
            fechaCreacion = Timestamp.now(),
            ultimoAcceso = Timestamp.now()
        )

        firestore.collection("users")
            .document(uid)
            .set(nuevoUsuario)
            .await()

        nuevoUsuario
    }

    suspend fun iniciarSesion(correo: String, password: String): String = withTimeout(TIMEOUT_MS) {
        val resultado = auth.signInWithEmailAndPassword(correo, password).await()
        val uid = resultado.user?.uid
            ?: throw IllegalStateException("No se pudo obtener el UID del usuario")

        firestore.collection("users")
            .document(uid)
            .update("ultimoAcceso", Timestamp.now())
            .await()

        uid
    }

    suspend fun recuperarContrasena(correo: String) = withTimeout(TIMEOUT_MS) {
        auth.sendPasswordResetEmail(correo).await()
    }
}