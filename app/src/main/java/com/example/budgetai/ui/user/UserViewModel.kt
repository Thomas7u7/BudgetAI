package com.example.budgetai.ui.user

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetai.data.model.InformacionFinanciera
import com.example.budgetai.data.model.Preferencias
import com.example.budgetai.data.model.UserState
import com.example.budgetai.data.repository.UserRepository
import com.example.budgetai.util.mensajeAmigable
import kotlinx.coroutines.launch

class UserViewModel : ViewModel() {

    private val repository = UserRepository()

    private val _userState = MutableLiveData<UserState>(UserState.Idle)
    val userState: LiveData<UserState> = _userState

    fun cargarPerfil() {
        _userState.value = UserState.Loading
        viewModelScope.launch {
            try {
                val usuario = repository.obtenerPerfil()
                _userState.value = UserState.PerfilCargado(usuario)
            } catch (e: Exception) {
                _userState.value = UserState.Error(e.mensajeAmigable())
            }
        }
    }

    fun actualizarDatosBasicos(nombreCompleto: String, telefono: String?, fotoBase64: String?) {
        if (nombreCompleto.isBlank()) {
            _userState.value = UserState.Error("El nombre no puede estar vacío")
            return
        }
        _userState.value = UserState.Loading
        viewModelScope.launch {
            try {
                repository.actualizarDatosBasicos(nombreCompleto, telefono, fotoBase64)
                _userState.value = UserState.GuardadoExitoso
            } catch (e: Exception) {
                _userState.value = UserState.Error(e.mensajeAmigable())
            }
        }
    }

    fun actualizarInformacionFinanciera(info: InformacionFinanciera) {
        _userState.value = UserState.Loading
        viewModelScope.launch {
            try {
                repository.actualizarInformacionFinanciera(info)
                _userState.value = UserState.GuardadoExitoso
            } catch (e: Exception) {
                _userState.value = UserState.Error(e.mensajeAmigable())
            }
        }
    }

    fun actualizarPreferencias(preferencias: Preferencias) {
        _userState.value = UserState.Loading
        viewModelScope.launch {
            try {
                repository.actualizarPreferencias(preferencias)
                _userState.value = UserState.GuardadoExitoso
            } catch (e: Exception) {
                _userState.value = UserState.Error(e.mensajeAmigable())
            }
        }
    }

    fun actualizarBiometria(habilitada: Boolean) {
        viewModelScope.launch {
            try {
                repository.actualizarBiometria(habilitada)
            } catch (e: Exception) {
                _userState.value = UserState.Error(e.mensajeAmigable())
            }
        }
    }

    fun cambiarContrasena(passwordActual: String, nuevaContrasena: String) {
        if (passwordActual.isBlank()) {
            _userState.value = UserState.Error("Ingresa tu contraseña actual")
            return
        }
        if (nuevaContrasena.length < 8) {
            _userState.value = UserState.Error("La nueva contraseña debe tener al menos 8 caracteres")
            return
        }
        _userState.value = UserState.Loading
        viewModelScope.launch {
            try {
                repository.reautenticar(passwordActual)
                repository.cambiarContrasena(nuevaContrasena)
                _userState.value = UserState.GuardadoExitoso
            } catch (e: Exception) {
                _userState.value = UserState.Error(e.mensajeAmigable())
            }
        }
    }

    fun reautenticarYEliminarCuenta(passwordActual: String) {
        if (passwordActual.isBlank()) {
            _userState.value = UserState.Error("Ingresa tu contraseña actual")
            return
        }
        _userState.value = UserState.Loading
        viewModelScope.launch {
            try {
                repository.reautenticar(passwordActual)
                repository.eliminarCuenta()
                _userState.value = UserState.GuardadoExitoso
            } catch (e: Exception) {
                _userState.value = UserState.Error(e.mensajeAmigable())
            }
        }
    }

    fun cerrarSesion() {
        repository.cerrarSesion()
    }
}