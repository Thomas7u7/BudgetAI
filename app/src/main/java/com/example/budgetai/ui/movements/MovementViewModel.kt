package com.example.budgetai.ui.movements

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetai.data.model.Movement
import com.example.budgetai.data.model.MovementState
import com.example.budgetai.data.repository.MovementRepository
import com.example.budgetai.util.mensajeAmigable
import kotlinx.coroutines.launch

class MovementViewModel : ViewModel() {

    private val repository = MovementRepository()

    private val _movementState = MutableLiveData<MovementState>(MovementState.Idle)
    val movementState: LiveData<MovementState> = _movementState

    fun cargarMovimientos() {
        _movementState.value = MovementState.Loading
        viewModelScope.launch {
            try {
                val lista = repository.obtenerMovimientos()
                _movementState.value = MovementState.ListaCargada(lista)
            } catch (e: Exception) {
                _movementState.value = MovementState.Error(e.mensajeAmigable())
            }
        }
    }

    fun cargarMovimiento(id: String) {
        _movementState.value = MovementState.Loading
        viewModelScope.launch {
            try {
                val movimiento = repository.obtenerMovimiento(id)
                _movementState.value = MovementState.DetalleCargado(movimiento)
            } catch (e: Exception) {
                _movementState.value = MovementState.Error(e.mensajeAmigable())
            }
        }
    }

    fun guardarMovimiento(
        idExistente: String?,
        tipo: String,
        monto: Double,
        categoria: String,
        descripcion: String,
        fecha: com.google.firebase.Timestamp
    ) {
        if (monto <= 0) {
            _movementState.value = MovementState.Error("El monto debe ser mayor a cero")
            return
        }
        if (categoria.isBlank()) {
            _movementState.value = MovementState.Error("Selecciona una categoría")
            return
        }

        val movimiento = Movement(
            tipo = tipo,
            monto = monto,
            categoria = categoria,
            descripcion = descripcion,
            fecha = fecha
        )

        _movementState.value = MovementState.Loading
        viewModelScope.launch {
            try {
                if (idExistente != null) {
                    repository.actualizarMovimiento(idExistente, movimiento)
                } else {
                    repository.crearMovimiento(movimiento)
                }
                _movementState.value = MovementState.GuardadoExitoso
            } catch (e: Exception) {
                _movementState.value = MovementState.Error(e.mensajeAmigable())
            }
        }
    }

    fun eliminarMovimiento(id: String) {
        _movementState.value = MovementState.Loading
        viewModelScope.launch {
            try {
                repository.eliminarMovimiento(id)
                _movementState.value = MovementState.Eliminado
            } catch (e: Exception) {
                _movementState.value = MovementState.Error(e.mensajeAmigable())
            }
        }
    }
}