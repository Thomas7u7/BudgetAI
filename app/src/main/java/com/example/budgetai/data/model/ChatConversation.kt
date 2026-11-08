package com.example.budgetai.data.model

import com.google.firebase.Timestamp

/** Resumen de una conversación con BudgetIA (lo que se muestra en el historial). */
data class ChatConversation(
    val id: String,
    val titulo: String,
    val ultimoMensaje: String,
    val actualizado: Timestamp?
)
