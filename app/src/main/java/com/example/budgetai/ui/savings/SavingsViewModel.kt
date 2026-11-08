package com.example.budgetai.ui.savings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetai.data.model.SavingsGoal
import com.example.budgetai.data.model.SavingsState
import com.example.budgetai.data.repository.AIRepository
import com.example.budgetai.data.repository.ChatRepository
import com.example.budgetai.data.repository.SavingsRepository
import com.example.budgetai.util.mensajeAmigable
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SavingsViewModel : ViewModel() {

    private val repository = SavingsRepository()
    private val aiRepository = AIRepository()

    private val _savingsState = MutableLiveData<SavingsState>(SavingsState.Idle)
    val savingsState: LiveData<SavingsState> = _savingsState

    // ---------- Chat con Gemini ----------

    private val _respuestaIA = MutableLiveData<RespuestaIA?>(null)
    val respuestaIA: LiveData<RespuestaIA?> = _respuestaIA

    private val _cargandoIA = MutableLiveData(false)
    val cargandoIA: LiveData<Boolean> = _cargandoIA

    fun preguntarIA(pregunta: String, historial: String = "") {
        if (pregunta.isBlank() || _cargandoIA.value == true) return
        _cargandoIA.value = true
        viewModelScope.launch {
            try {
                _respuestaIA.value = RespuestaIA(aiRepository.responder(pregunta.trim(), historial))
            } catch (e: Exception) {
                _respuestaIA.value = RespuestaIA(
                    e.message ?: "No pude conectar con BudgetIA. Revisa tu conexión e intenta de nuevo.",
                    error = true
                )
            } finally {
                _cargandoIA.value = false
            }
        }
    }

    fun consumirRespuestaIA() {
        _respuestaIA.value = null
    }

    // ---------- Historial del chat ----------

    private val chatRepository = ChatRepository()

    /** Evita que dos guardados simultáneos creen dos conversaciones distintas. */
    private val guardadoMutex = Mutex()

    /** Conversación abierta. Null = chat nuevo que todavía no tiene ningún mensaje guardado. */
    var chatIdActual: String? = null
        private set

    private val _historialChat = MutableLiveData<List<ChatMessage>?>(null)
    val historialChat: LiveData<List<ChatMessage>?> = _historialChat

    /**
     * Abre una conversación:
     *  - nueva = true  -> chat vacío (se crea en Firestore al enviar el primer mensaje).
     *  - chatId != null -> esa conversación.
     *  - ambos vacíos   -> la última conversación donde el usuario se quedó (o vacío si no hay).
     * Si falla la lectura (sin red o sin permisos) el chat arranca vacío y sigue funcionando.
     */
    fun abrirConversacion(chatId: String?, nueva: Boolean) {
        viewModelScope.launch {
            if (nueva) {
                chatIdActual = null
                _historialChat.value = emptyList()
                return@launch
            }
            val id = chatId ?: runCatching { chatRepository.ultimaConversacionId() }.getOrNull()
            chatIdActual = id
            _historialChat.value =
                if (id == null) emptyList()
                else runCatching { chatRepository.obtenerMensajes(id) }.getOrDefault(emptyList())
        }
    }

    fun consumirHistorialChat() {
        _historialChat.value = null
    }

    /** Guardar es "mejor esfuerzo": un fallo no debe interrumpir la conversación. */
    fun guardarMensajeChat(mensaje: ChatMessage) {
        val destino = chatIdActual
        viewModelScope.launch {
            guardadoMutex.withLock {
                runCatching {
                    val id = destino ?: chatIdActual
                        ?: chatRepository.crearConversacion(mensaje.texto).also { chatIdActual = it }
                    chatRepository.guardarMensaje(id, mensaje)
                }
            }
        }
    }

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

        val montoRecomendado = calcularMontoPorPeriodo(montoObjetivo, fechaLimite, frecuencia)

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

    fun actualizarMeta(id: String, nombre: String, montoObjetivo: Double, montoAhorrado: Double, fechaLimite: Timestamp, frecuencia: String) {
        if (nombre.isBlank()) {
            _savingsState.value = SavingsState.Error("Ponle un nombre a tu meta")
            return
        }
        if (montoObjetivo <= 0) {
            _savingsState.value = SavingsState.Error("El monto objetivo debe ser mayor a cero")
            return
        }
        if (montoObjetivo < montoAhorrado) {
            _savingsState.value = SavingsState.Error("El objetivo no puede ser menor a lo que ya ahorraste ($${montoAhorrado.toInt()})")
            return
        }

        val montoRecomendado = calcularMontoPorPeriodo(
            (montoObjetivo - montoAhorrado).coerceAtLeast(0.0), fechaLimite, frecuencia
        )

        val meta = SavingsGoal(
            id = id,
            nombre = nombre,
            montoObjetivo = montoObjetivo,
            montoAhorrado = montoAhorrado,
            fechaLimite = fechaLimite,
            frecuencia = frecuencia,
            montoRecomendado = montoRecomendado,
            completada = montoAhorrado >= montoObjetivo
        )

        _savingsState.value = SavingsState.Loading
        viewModelScope.launch {
            try {
                repository.actualizarMeta(id, meta)
                _savingsState.value = SavingsState.GuardadoExitoso
            } catch (e: Exception) {
                _savingsState.value = SavingsState.Error(e.mensajeAmigable())
            }
        }
    }

    fun marcarComoActiva(id: String) {
        _savingsState.value = SavingsState.Loading
        viewModelScope.launch {
            try {
                repository.marcarComoActiva(id)
                _savingsState.value = SavingsState.GuardadoExitoso
            } catch (e: Exception) {
                _savingsState.value = SavingsState.Error(e.mensajeAmigable())
            }
        }
    }

    /** Monto sugerido por periodo (semana, quincena o mes) según la frecuencia elegida. */
    private fun calcularMontoPorPeriodo(faltante: Double, fechaLimite: Timestamp, frecuencia: String): Double {
        if (faltante <= 0) return 0.0
        val milisRestantes = fechaLimite.toDate().time - System.currentTimeMillis()
        val diasRestantes = (milisRestantes / (1000L * 60 * 60 * 24)).coerceAtLeast(1)
        val diasPorPeriodo = when (frecuencia) {
            "Semanal" -> 7
            "Quincenal" -> 15
            else -> 30
        }
        val periodos = (diasRestantes / diasPorPeriodo).coerceAtLeast(1)
        return faltante / periodos
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
    fun eliminarMeta(id: String) {
        _savingsState.value = SavingsState.Loading
        viewModelScope.launch {
            try {
                repository.eliminarMeta(id)
                _savingsState.value = SavingsState.Eliminado
            } catch (e: Exception) {
                _savingsState.value = SavingsState.Error(e.mensajeAmigable())
            }
        }
    }
}

data class RespuestaIA(val texto: String, val error: Boolean = false)
