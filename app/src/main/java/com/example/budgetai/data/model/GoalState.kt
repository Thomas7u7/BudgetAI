package com.example.budgetai.data.model

data class GoalPlan(
    val dineroRestante: Double,
    val mesesRestantes: Int,
    val ahorroMensual: Double,
    val ahorroSemanal: Double
)

sealed class GoalState {
    object Idle : GoalState()
    object Loading : GoalState()
    data class ListaCargada(val metas: List<Goal>) : GoalState()
    data class DetalleCargado(val meta: Goal) : GoalState()
    data class PlanGenerado(val plan: GoalPlan) : GoalState()
    object MetaCreada : GoalState()
    object DineroAgregado : GoalState()
    object MetaActualizada : GoalState()
    object MetaRetirada : GoalState()
    object Eliminado : GoalState()
    // Errores por campo, para pintar varios bordes rojos a la vez (como en el mockup)
    data class ErrorCampos(val errores: Map<String, String>) : GoalState()
    data class Error(val mensaje: String) : GoalState()
}