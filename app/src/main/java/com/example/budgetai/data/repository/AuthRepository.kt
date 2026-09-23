package com.example.budgetai.data.repository

import com.example.budgetai.data.model.Usuario
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun registrar(nombreCompleto: String, correo: String, password: String): Usuario {
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

        return nuevoUsuario
    }

    suspend fun iniciarSesion(correo: String, password: String): String {
        val resultado = auth.signInWithEmailAndPassword(correo, password).await()
        val uid = resultado.user?.uid
            ?: throw IllegalStateException("No se pudo obtener el UID del usuario")

        firestore.collection("users")
            .document(uid)
            .update("ultimoAcceso", Timestamp.now())
            .await()

        return uid
    }

    suspend fun recuperarContrasena(correo: String) {
        auth.sendPasswordResetEmail(correo).await()
    }
}