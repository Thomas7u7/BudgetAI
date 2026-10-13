package com.example.budgetai.data.model

sealed class UserState {
    object Idle : UserState()
    object Loading : UserState()
    data class PerfilCargado(val usuario: Usuario) : UserState()
    object GuardadoExitoso : UserState()
    data class Error(val mensaje: String) : UserState()
}