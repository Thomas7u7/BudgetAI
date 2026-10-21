package com.example.budgetai.data.model

sealed class SavingsState {
    object Idle : SavingsState()
    object Loading : SavingsState()
    data class ListaCargada(val metas: List<SavingsGoal>) : SavingsState()
    data class DetalleCargado(val meta: SavingsGoal) : SavingsState()
    data class HistorialCargado(val movimientos: List<SavingsMovement>) : SavingsState()
    object GuardadoExitoso : SavingsState()
    object PinConfigurado : SavingsState()
    object PinValido : SavingsState()
    data class PinInvalido(val intentosRestantes: Int) : SavingsState()
    object PinBloqueado : SavingsState()
    object DepositoExitoso : SavingsState()
    object RetiroExitoso : SavingsState()
    data class Error(val mensaje: String) : SavingsState()
}