package com.example.budgetai.ui.savings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetai.data.model.ChatConversation
import com.example.budgetai.data.repository.ChatRepository
import com.example.budgetai.util.mensajeAmigable
import kotlinx.coroutines.launch

/** Lista de conversaciones con BudgetIA: cargar, y borrar una. */
class ChatHistoryViewModel : ViewModel() {

    private val repository = ChatRepository()

    private val _conversaciones = MutableLiveData<List<ChatConversation>>(emptyList())
    val conversaciones: LiveData<List<ChatConversation>> = _conversaciones

    private val _cargando = MutableLiveData(false)
    val cargando: LiveData<Boolean> = _cargando

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    /** Id de la conversación que se acaba de borrar (para que la pantalla sepa si era la abierta). */
    private val _idBorrado = MutableLiveData<String?>(null)
    val idBorrado: LiveData<String?> = _idBorrado

    fun cargar() {
        _cargando.value = true
        viewModelScope.launch {
            try {
                _conversaciones.value = repository.listarConversaciones()
            } catch (e: Exception) {
                _error.value = e.mensajeAmigable()
            } finally {
                _cargando.value = false
            }
        }
    }

    fun borrar(id: String) {
        viewModelScope.launch {
            try {
                repository.borrarConversacion(id)
                _conversaciones.value = _conversaciones.value.orEmpty().filterNot { it.id == id }
                _idBorrado.value = id
            } catch (e: Exception) {
                _error.value = "No se pudo borrar la conversación. Intenta de nuevo."
            }
        }
    }

    fun consumirError() {
        _error.value = null
    }

    fun consumirIdBorrado() {
        _idBorrado.value = null
    }
}
