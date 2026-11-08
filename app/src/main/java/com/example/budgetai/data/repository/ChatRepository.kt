package com.example.budgetai.data.repository

import com.example.budgetai.data.model.ChatConversation
import com.example.budgetai.ui.savings.ChatMessage
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

private const val TIMEOUT_MS = 15_000L
private const val LIMITE_MENSAJES = 100L
private const val LIMITE_CONVERSACIONES = 50L
private const val LARGO_TITULO = 40
private const val LARGO_VISTA_PREVIA = 80

/**
 * Conversaciones con BudgetIA, guardadas por usuario:
 *
 *   users/{uid}/chats/{chatId}                 -> titulo, creado, actualizado, ultimoMensaje
 *   users/{uid}/chats/{chatId}/messages/{id}   -> texto, esUsuario, fecha
 *
 * Así el usuario puede retomar el chat donde se quedó, elegir otro del historial
 * o empezar uno nuevo, aunque cierre la app o cambie de celular.
 */
class ChatRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private fun uidActual(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("No hay usuario autenticado")

    private fun chats(): CollectionReference =
        firestore.collection("users").document(uidActual()).collection("chats")

    private fun mensajes(chatId: String): CollectionReference =
        chats().document(chatId).collection("messages")

    /** Conversaciones del usuario, la más reciente primero. */
    suspend fun listarConversaciones(): List<ChatConversation> = withTimeout(TIMEOUT_MS) {
        chats()
            .orderBy("actualizado", Query.Direction.DESCENDING)
            .limit(LIMITE_CONVERSACIONES)
            .get()
            .await()
            .documents
            .map { doc ->
                ChatConversation(
                    id = doc.id,
                    titulo = doc.getString("titulo")?.takeIf { it.isNotBlank() } ?: "Conversación",
                    ultimoMensaje = doc.getString("ultimoMensaje") ?: "",
                    actualizado = doc.getTimestamp("actualizado")
                )
            }
    }

    /** Id de la conversación más reciente (donde el usuario se quedó), o null si no hay ninguna. */
    suspend fun ultimaConversacionId(): String? = withTimeout(TIMEOUT_MS) {
        chats()
            .orderBy("actualizado", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.id
    }

    /** Crea una conversación nueva; el título sale del primer mensaje del usuario. */
    suspend fun crearConversacion(primerMensaje: String): String = withTimeout(TIMEOUT_MS) {
        val ahora = Timestamp.now()
        val ref = chats().document()
        ref.set(
            mapOf(
                "titulo" to titulo(primerMensaje),
                "creado" to ahora,
                "actualizado" to ahora,
                "ultimoMensaje" to ""
            )
        ).await()
        ref.id
    }

    /** Mensajes de una conversación, del más antiguo al más reciente. */
    suspend fun obtenerMensajes(chatId: String): List<ChatMessage> = withTimeout(TIMEOUT_MS) {
        mensajes(chatId)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .limit(LIMITE_MENSAJES)
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                val texto = doc.getString("texto")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                ChatMessage(texto, doc.getBoolean("esUsuario") ?: false)
            }
            .reversed()
    }

    /** Guarda el mensaje y actualiza la vista previa y la fecha de la conversación (todo en un lote). */
    suspend fun guardarMensaje(chatId: String, mensaje: ChatMessage) = withTimeout(TIMEOUT_MS) {
        val ahora = Timestamp.now()
        val chatRef = chats().document(chatId)
        val batch = firestore.batch()
        batch.set(
            chatRef.collection("messages").document(),
            mapOf(
                "texto" to mensaje.texto,
                "esUsuario" to mensaje.esUsuario,
                "fecha" to ahora
            )
        )
        batch.update(
            chatRef,
            mapOf(
                "actualizado" to ahora,
                "ultimoMensaje" to mensaje.texto.replace('\n', ' ').take(LARGO_VISTA_PREVIA)
            )
        )
        batch.commit().await()
    }

    /** Borra una conversación completa. Firestore no borra subcolecciones en cascada: primero los mensajes. */
    suspend fun borrarConversacion(chatId: String) = withTimeout(TIMEOUT_MS * 2) {
        val coleccion = mensajes(chatId)
        while (true) {
            val docs = coleccion.limit(200).get().await().documents
            if (docs.isEmpty()) break
            val batch = firestore.batch()
            docs.forEach { batch.delete(it.reference) }
            batch.commit().await()
            if (docs.size < 200) break
        }
        chats().document(chatId).delete().await()
    }

    private fun titulo(primerMensaje: String): String {
        val limpio = primerMensaje.replace('\n', ' ').trim()
        if (limpio.isEmpty()) return "Conversación"
        return if (limpio.length > LARGO_TITULO) limpio.take(LARGO_TITULO).trimEnd() + "…" else limpio
    }
}
