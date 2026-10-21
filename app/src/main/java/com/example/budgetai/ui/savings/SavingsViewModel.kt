package com.example.budgetai.ui.savings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetai.data.model.SavingsGoal
import com.example.budgetai.data.model.SavingsState
import com.example.budgetai.data.repository.SavingsRepository
import com.example.budgetai.util.mensajeAmigable
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch

class SavingsViewModel : ViewModel() {

    private val repository = SavingsRepository()

    private val _savingsState = MutableLiveData<SavingsState>(SavingsState.Idle)
    val savingsState: LiveData<SavingsState> = _savingsState

    // ---------- Metas ----------

    fun cargarMetas() {
        _savingsState.value = SavingsState.Loading
        viewModelScope.launch {
            try {
                val metas = repository.obtenerMetas()
                _savingsState.value = SavingsState.ListaCargada(metas)
            } catch (e: Exception) {
                _savingsState.value = SavingsState.Error(e.mensajeAmigable())
            }
        }
    }

    fun cargarMetaActiva() {
        _savingsState.value = SavingsState.Loading
        viewModelScope.launch {
            try {
                val meta = repository.obtenerMetaActiva()
                if (meta != null) {
                    _savingsState.value = SavingsState.DetalleCargado(meta)
                } else {
                    _savingsState.value = SavingsState.ListaCargada(emptyList())
                }
            } catch (e: Exception) {
                _savingsState.value = SavingsState.Error(e.mensajeAmigable())
            }
        }
    }

    fun cargarMeta(id: String) {
        _savingsState.value = SavingsState.Loading
        viewModelScope.launch {
            try {
                val meta = repository.obtenerMeta(id)
                _savingsState.value = SavingsState.DetalleCargado(meta)
            } catch (e: Exception) {
                _savingsState.value = SavingsState.Error(e.mensajeAmigable())
            }
        }
    }

    fun crearMeta(nombre: String, montoObjetivo: Double, fechaLimite: Timestamp, frecuencia: String) {
        if (nombre.isBlank()) {
            _savingsState.value = SavingsState.Error("Ponle un nombre a tu meta")
            return
        }
        if (montoObjetivo <= 0) {
            _savingsState.value = SavingsState.Error("El monto objetivo debe ser mayor a cero")
            return
        }

        val semanasRestantes = calcularSemanasRestantes(fechaLimite)
        val montoRecomendado = if (semanasRestantes > 0) montoObjetivo / semanasRestantes else montoObjetivo

        val meta = SavingsGoal(
            nombre = nombre,
            montoObjetivo = montoObjetivo,
            montoAhorrado = 0.0,
            fechaLimite = fechaLimite,
            frecuencia = frecuencia,
            montoRecomendado = montoRecomendado
        )

        _savingsState.value = SavingsState.Loading
        viewModelScope.launch {
            try {
                repository.crearMeta(meta)
                _savingsState.value = SavingsState.GuardadoExitoso
            } catch (e: Exception) {
                _savingsState.value = SavingsState.Error(e.mensajeAmigable())
            }
        }
    }

    private fun calcularSemanasRestantes(fechaLimite: Timestamp): Int {
        val milisRestantes = fechaLimite.toDate().time - System.currentTimeMillis()
        val semanas = milisRestantes / (1000L * 60 * 60 * 24 * 7)
        return semanas.toInt().coerceAtLeast(1)
    }

    // ---------- Depósitos y retiros ----------

    fun depositar(metaId: String, monto: Double) {
        if (monto <= 0) {
            _savingsState.value = SavingsState.Error("Ingresa un monto válido")
            return
        }
        _savingsState.value = SavingsState.Loading
        viewModelScope.launch {
            try {
                repository.depositar(metaId, monto)
                _savingsState.value = SavingsState.DepositoExitoso
            } catch (e: Exception) {
                _savingsState.value = SavingsState.Error(e.mensajeAmigable())
            }
        }
    }

    fun retirar(metaId: String, monto: Double) {
        if (monto <= 0) {
            _savingsState.value = SavingsState.Error("Ingresa un monto válido")
            return
        }
        _savingsState.value = SavingsState.Loading
        viewModelScope.launch {
            try {
                repository.retirar(metaId, monto)
                _savingsState.value = SavingsState.RetiroExitoso
            } catch (e: Exception) {
                _savingsState.value = SavingsState.Error(e.mensajeAmigable())
            }
        }
    }

    fun cargarHistorial(metaId: String) {
        _savingsState.value = SavingsState.Loading
        viewModelScope.launch {
            try {
                val movimientos = repository.obtenerHistorial(metaId)
                _savingsState.value = SavingsState.HistorialCargado(movimientos)
            } catch (e: Exception) {
                _savingsState.value = SavingsState.Error(e.mensajeAmigable())
            }
        }
    }

    // ---------- PIN ----------

    fun verificarSiTienePin(onResultado: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val tiene = repository.tienePinConfigurado()
                onResultado(tiene)
            } catch (e: Exception) {
                onResultado(false)
            }
        }
    }

    fun configurarPin(pin: String) {
        if (pin.length != 4 || !pin.all { it.isDigit() }) {
            _savingsState.value = SavingsState.Error("El PIN debe tener exactamente 4 dígitos")
            return
        }
        _savingsState.value = SavingsState.Loading
        viewModelScope.launch {
            try {
                repository.configurarPin(pin)
                _savingsState.value = SavingsState.PinConfigurado
            } catch (e: Exception) {
                _savingsState.value = SavingsState.Error(e.mensajeAmigable())
            }
        }
    }

    fun verificarPin(pin: String) {
        _savingsState.value = SavingsState.Loading
        viewModelScope.launch {
            try {
                val esCorrecto = repository.verificarPin(pin)
                if (esCorrecto) {
                    _savingsState.value = SavingsState.PinValido
                } else {
                    val intentos = repository.obtenerIntentosFallidos()
                    val restantes = (3 - intentos).coerceAtLeast(0)
                    if (restantes == 0) {
                        _savingsState.value = SavingsState.PinBloqueado
                    } else {
                        _savingsState.value = SavingsState.PinInvalido(restantes)
                    }
                }
            } catch (e: Exception) {
                if (e.message == "BLOQUEADO") {
                    _savingsState.value = SavingsState.PinBloqueado
                } else {
                    _savingsState.value = SavingsState.Error(e.mensajeAmigable())
                }
            }
        }
    }
}