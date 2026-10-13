package com.example.budgetai.util

import kotlinx.coroutines.TimeoutCancellationException

/** Traduce cualquier excepción (de Firebase u otra) a un mensaje simple en español. */
fun Throwable.mensajeAmigable(): String {
    val texto = (message ?: "").lowercase()
    return when {
        this is TimeoutCancellationException ->
            "Sin conexión a internet. Verifica tu conexión e intenta de nuevo."

        texto.contains("network") || texto.contains("unavailable") || texto.contains("timeout") ->
            "Sin conexión a internet. Intenta de nuevo."

        texto.contains("password is invalid") ||
                texto.contains("invalid_login_credentials") ||
                texto.contains("supplied auth credential is incorrect") ||
                texto.contains("malformed or has expired") ->
            "La contraseña actual es incorrecta."

        texto.contains("email address is already in use") ->
            "Este correo ya está registrado."

        texto.contains("no user record") || texto.contains("user-not-found") ->
            "No existe una cuenta con estos datos."

        texto.contains("weak-password") || texto.contains("password should be at least") ->
            "La contraseña es demasiado débil."

        texto.contains("too-many-requests") ->
            "Demasiados intentos. Espera unos minutos e intenta de nuevo."

        texto.contains("requires-recent-login") || texto.contains("recent login") ->
            "Por seguridad, vuelve a ingresar tu contraseña actual."

        texto.contains("permission") || texto.contains("insufficient") ->
            "No tienes permisos para realizar esta acción."

        else -> "Ocurrió un error. Intenta de nuevo."
    }
}