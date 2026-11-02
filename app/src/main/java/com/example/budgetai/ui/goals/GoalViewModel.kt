package com.example.budgetai.ui.goals

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetai.data.model.Goal
import com.example.budgetai.data.model.GoalPlan
import com.example.budgetai.data.model.GoalState
import com.example.budgetai.data.repository.ERROR_FONDOS
import com.example.budgetai.data.repository.GoalRepository
import com.example.budgetai.util.mensajeAmigable
import com.google.firebase.Timestamp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GoalViewModel : ViewModel() {

    private val repository = GoalRepository()

    private val _goalState = MutableLiveData<GoalState>(GoalState.Idle)
    val goalState: LiveData<GoalState> = _goalState

    private fun ocupado() = _goalState.value is GoalState.Loading

    // ---------- READ ----------

    fun cargarMetas() {
        _goalState.value = GoalState.Loading
        viewModelScope.launch {
            try {
                _goalState.value = GoalState.ListaCargada(repository.obtenerMetas())
            } catch (e: Exception) {
                _goalState.value = GoalState.Error(e.mensajeAmigable())
            }
        }
    }

    fun cargarMeta(id: String) {
        _goalState.value = GoalState.Loading
        viewModelScope.launch {
            try {
                _goalState.value = GoalState.DetalleCargado(repository.obtenerMeta(id))
            } catch (e: Exception) {
                _goalState.value = GoalState.Error(e.mensajeAmigable())
            }
        }
    }

    // ---------- CREATE ----------

    /** Valida el formulario y calcula el plan. Aquí se conectará la IA al final del proyecto. */
    fun generarPlan(
        nombre: String, categoria: String,
        monto: Double?, ahorroInicial: Double?, fecha: Timestamp?
    ) {
        if (ocupado()) return
        val errores = validar(nombre, categoria, monto, ahorroInicial, fecha)
        if (errores.isNotEmpty()) {
            _goalState.value = GoalState.ErrorCampos(errores)
            return
        }

        _goalState.value = GoalState.Loading
        viewModelScope.launch {
            delay(1200) // simula el análisis: con la IA real esta espera será la llamada a la API
            _goalState.value = GoalState.PlanGenerado(
                calcularPlan(monto!!, ahorroInicial!!, fecha!!)
            )
        }
    }

    fun crearMeta(
        nombre: String, categoria: String,
        monto: Double, ahorroInicial: Double, fecha: Timestamp
    ) {
        if (ocupado()) return
        val plan = calcularPlan(monto, ahorroInicial, fecha)
        val meta = Goal(
            nombre = nombre,
            categoria = categoria,
            montoObjetivo = monto,
            ahorroInicial = ahorroInicial,
            fechaObjetivo = fecha,
            ahorroMensualRecomendado = plan.ahorroMensual
        )

        _goalState.value = GoalState.Loading
        viewModelScope.launch {
            try {
                repository.crearMeta(meta)
                _goalState.value = GoalState.MetaCreada
            } catch (e: Exception) {
                _goalState.value = errorDe(e, "ahorroInicial")
            }
        }
    }

    fun agregarDinero(id: String, monto: Double) {
        if (ocupado()) return // evita el doble envío que ya nos pasó en Ahorros
        if (monto <= 0) {
            _goalState.value = GoalState.ErrorCampos(mapOf("deposito" to "Ingresa un monto válido"))
            return
        }
        _goalState.value = GoalState.Loading
        viewModelScope.launch {
            try {
                repository.agregarDinero(id, monto)
                _goalState.value = GoalState.DineroAgregado
            } catch (e: Exception) {
                _goalState.value = errorDe(e, "deposito")
            }
        }
    }

    // ---------- Helpers ----------

    private fun validar(
        nombre: String, categoria: String,
        monto: Double?, ahorroInicial: Double?, fecha: Timestamp?
    ): Map<String, String> {
        val errores = mutableMapOf<String, String>()
        val obligatorio = "Este campo es obligatorio"

        if (nombre.isBlank()) errores["nombre"] = obligatorio
        if (categoria.isBlank()) errores["categoria"] = obligatorio

        if (monto == null) errores["monto"] = obligatorio
        else if (monto <= 0) errores["monto"] = "Ingresa un monto válido mayor a \$0"

        if (ahorroInicial == null) errores["ahorroInicial"] = obligatorio
        else if (monto != null && ahorroInicial > monto) {
            errores["ahorroInicial"] = "El ahorro inicial no puede superar el objetivo"
        }

        if (fecha == null) errores["fecha"] = obligatorio
        else if (fecha.toDate().time < System.currentTimeMillis()) {
            errores["fecha"] = "La fecha debe ser posterior a hoy"
        }
        return errores
    }

    private fun calcularPlan(monto: Double, ahorroInicial: Double, fecha: Timestamp): GoalPlan {
        val restante = (monto - ahorroInicial).coerceAtLeast(0.0)
        val dias = (fecha.toDate().time - System.currentTimeMillis()) / (1000L * 60 * 60 * 24)
        val meses = (dias / 30).toInt().coerceAtLeast(1)
        val mensual = restante / meses
        return GoalPlan(restante, meses, mensual, mensual / 4)
    }

    private fun errorDe(e: Exception, campoFondos: String): GoalState =
        if (e.message == ERROR_FONDOS) {
            GoalState.ErrorCampos(mapOf(campoFondos to "Fondos insuficientes, excede tu saldo actual"))
        } else {
            GoalState.Error(e.mensajeAmigable())
        }
}