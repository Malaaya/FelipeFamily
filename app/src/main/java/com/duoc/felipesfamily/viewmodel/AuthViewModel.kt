package com.duoc.felipesfamily.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.duoc.felipesfamily.data.AppDatabase
import com.duoc.felipesfamily.data.Roles
import com.duoc.felipesfamily.data.SessionEntity
import com.duoc.felipesfamily.data.UserEntity
import com.duoc.felipesfamily.util.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginState(
    val alias: String = "",
    val password: String = "",
    val submitted: Boolean = false,
    val aliasError: String? = null,
    val passwordError: String? = null,
    val formError: String? = null
)

data class RegisterState(
    val role: String = Roles.PRODUCER,
    val alias: String = "",
    val zone: String = "",
    val password: String = "",
    val repeat: String = "",
    val submitted: Boolean = false,
    val aliasError: String? = null,
    val zoneError: String? = null,
    val passwordError: String? = null,
    val repeatError: String? = null,
    val success: Boolean = false
)

data class RecoverState(
    val alias: String = "",
    val error: String? = null,
    val sent: Boolean = false
)

private fun LoginState.validated() = copy(
    submitted = true,
    aliasError = Validators.required(alias, "El usuario es obligatorio"),
    passwordError = Validators.required(password, "La contraseña es obligatoria")
)

private val LoginState.hasErrors get() = aliasError != null || passwordError != null

private fun RegisterState.validated() = copy(
    submitted = true,
    aliasError = Validators.alias(alias),
    zoneError = Validators.zone(zone),
    passwordError = Validators.password(password),
    repeatError = Validators.repeat(password, repeat)
)

private val RegisterState.hasErrors
    get() = aliasError != null || zoneError != null || passwordError != null || repeatError != null

/** Maneja login, registro, recuperación y sesión. La UI solo observa estos estados. */
class AuthViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)

    private val _login = MutableStateFlow(LoginState())
    val login: StateFlow<LoginState> = _login.asStateFlow()

    private val _register = MutableStateFlow(RegisterState())
    val register: StateFlow<RegisterState> = _register.asStateFlow()

    private val _recover = MutableStateFlow(RecoverState())
    val recover: StateFlow<RecoverState> = _recover.asStateFlow()

    /** Usuario con sesión iniciada (guardada en Room). */
    private val _user = MutableStateFlow<UserEntity?>(null)
    val user: StateFlow<UserEntity?> = _user.asStateFlow()

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    init {
        viewModelScope.launch {
            db.seedIfEmpty()
            db.sessionDao().get()?.let { _user.value = db.userDao().byId(it.userId) }
            _ready.value = true
        }
    }

    // ---------- Login ----------
    fun onLoginChange(alias: String = _login.value.alias, password: String = _login.value.password) {
        val next = _login.value.copy(alias = alias, password = password, formError = null)
        _login.value = if (next.submitted) next.validated() else next
    }

    fun login() {
        val state = _login.value.validated()
        _login.value = state
        if (state.hasErrors) return
        viewModelScope.launch {
            val found = db.userDao().byAlias(state.alias.trim())
            if (found == null || found.passwordHash != Validators.hash(state.password)) {
                _login.update { it.copy(formError = "Usuario o contraseña incorrectos") }
            } else {
                db.sessionDao().save(SessionEntity(userId = found.id))
                _user.value = found
                _login.value = LoginState()
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            db.sessionDao().clear()
            _user.value = null
        }
    }

    // ---------- Registro ----------
    fun onRegisterChange(
        role: String = _register.value.role,
        alias: String = _register.value.alias,
        zone: String = _register.value.zone,
        password: String = _register.value.password,
        repeat: String = _register.value.repeat
    ) {
        val next = _register.value.copy(role = role, alias = alias, zone = zone, password = password, repeat = repeat)
        _register.value = if (next.submitted) next.validated() else next
    }

    fun register() {
        val state = _register.value.validated()
        _register.value = state
        if (state.hasErrors) return
        viewModelScope.launch {
            if (db.userDao().byAlias(state.alias.trim()) != null) {
                _register.update { it.copy(aliasError = "Ese alias ya está en uso") }
                return@launch
            }
            db.userDao().insert(
                UserEntity(
                    alias = state.alias.trim(),
                    passwordHash = Validators.hash(state.password),
                    role = state.role,
                    zone = state.zone.trim()
                )
            )
            _register.update { it.copy(success = true) }
        }
    }

    fun resetRegister() {
        _register.value = RegisterState()
    }

    // ---------- Recuperar contraseña (demostrativo) ----------
    fun onRecoverChange(alias: String) {
        _recover.value = RecoverState(alias = alias)
    }

    fun recover() {
        val alias = _recover.value.alias.trim()
        val error = Validators.required(alias, "Ingresa tu usuario")
        if (error != null) {
            _recover.update { it.copy(error = error) }
            return
        }
        viewModelScope.launch {
            if (db.userDao().byAlias(alias) == null) {
                _recover.update { it.copy(error = "No existe ese usuario") }
            } else {
                _recover.update { it.copy(sent = true) }
            }
        }
    }
}
