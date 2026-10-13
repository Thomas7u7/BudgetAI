package com.example.budgetai.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetai.data.model.AuthErrorType
import com.example.budgetai.data.model.AuthState
import com.example.budgetai.data.repository.AuthRepository
import com.example.budgetai.util.mensajeAmigable
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> = _authState

    fun registrar(nombreCompleto: String, correo: String, password: String, confirmarPassword: String) {
        if (nombreCompleto.isBlank() || correo.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error(AuthErrorType.CAMPOS_VACIOS, "Completa todos los campos")
            return
        }
        if (password != confirmarPassword) {
            _authState.value = AuthState.Error(AuthErrorType.PASSWORDS_NO_COINCIDEN, "Las contraseñas no coinciden")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                repository.registrar(nombreCompleto, correo, password)
                _authState.value = AuthState.RegistroExitoso
            } catch (e: FirebaseAuthUserCollisionException) {
                _authState.value = AuthState.Error(AuthErrorType.EMAIL_YA_REGISTRADO, "Este correo ya está registrado")
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                _authState.value = AuthState.Error(AuthErrorType.EMAIL_INVALIDO, "Correo electrónico inválido")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(AuthErrorType.ERROR_DESCONOCIDO, e.mensajeAmigable())
            }
        }
    }

    fun iniciarSesion(correo: String, password: String) {
        if (correo.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error(AuthErrorType.CAMPOS_VACIOS, "Completa todos los campos")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val uid = repository.iniciarSesion(correo, password)
                _authState.value = AuthState.Success(uid)
            } catch (e: FirebaseAuthInvalidUserException) {
                _authState.value = AuthState.Error(AuthErrorType.CREDENCIALES_INCORRECTAS, "Usuario no encontrado")
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                _authState.value = AuthState.Error(AuthErrorType.CREDENCIALES_INCORRECTAS, "Correo o contraseña incorrectos")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(AuthErrorType.ERROR_DESCONOCIDO, e.mensajeAmigable())
            }
        }
    }

    fun recuperarContrasena(correo: String) {
        if (correo.isBlank()) {
            _authState.value = AuthState.Error(AuthErrorType.CAMPOS_VACIOS, "Ingresa tu correo")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                repository.recuperarContrasena(correo)
                _authState.value = AuthState.Success("")
            } catch (e: FirebaseAuthInvalidUserException) {
                _authState.value = AuthState.Error(AuthErrorType.CREDENCIALES_INCORRECTAS, "No existe una cuenta con ese correo")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(AuthErrorType.ERROR_RED, e.mensajeAmigable())
            }
        }
    }
}