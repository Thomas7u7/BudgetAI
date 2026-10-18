package com.example.budgetai.data.model

sealed class MovementState {
    object Idle : MovementState()
    object Loading : MovementState()
    data class ListaCargada(val movimientos: List<Movement>) : MovementState()
    data class DetalleCargado(val movimiento: Movement) : MovementState()
    object GuardadoExitoso : MovementState()
    object Eliminado : MovementState()
    data class Error(val mensaje: String) : MovementState()
}