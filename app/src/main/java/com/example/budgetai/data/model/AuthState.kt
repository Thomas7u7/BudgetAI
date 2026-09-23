package com.example.budgetai.data.model

sealed class AuthState {

    // Estado inicial, sin ninguna acción en curso
    object Idle : AuthState()

    // Mientras se procesa login/registro (mockups: login-loading, registro-loading)
    object Loading : AuthState()

    // Login o registro exitoso, entrega el usuario autenticado
    data class Success(val uid: String) : AuthState()

    // Registro exitoso específicamente (mockup: registro-success, con su propia animación)
    object RegistroExitoso : AuthState()

    // Cualquier error, con un tipo para diferenciar el mensaje en pantalla
    data class Error(val tipo: AuthErrorType, val mensaje: String) : AuthState()
}

enum class AuthErrorType {
    CAMPOS_VACIOS,          // login-empty-fields
    EMAIL_INVALIDO,         // login-invalid-email
    CREDENCIALES_INCORRECTAS, // login-wrong-credentials
    EMAIL_YA_REGISTRADO,    // registro-email-taken
    PASSWORDS_NO_COINCIDEN, // registro-passwords-no-match
    ERROR_RED,              // recuperar-no-internet
    ERROR_DESCONOCIDO
}