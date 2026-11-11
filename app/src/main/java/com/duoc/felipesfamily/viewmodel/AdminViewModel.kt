package com.duoc.felipesfamily.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.duoc.felipesfamily.data.NeedEntity
import com.duoc.felipesfamily.data.ProfileSummary
import com.duoc.felipesfamily.data.ProviderProfileEntity
import com.duoc.felipesfamily.data.Roles
import com.duoc.felipesfamily.data.ServiceEntity
import com.duoc.felipesfamily.data.UserEntity
import com.duoc.felipesfamily.repository.AdminRepository
import com.duoc.felipesfamily.util.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ServiceForm(
    val name: String = "",
    val radius: String = "",
    val submitted: Boolean = false,
    val nameError: String? = null,
    val radiusError: String? = null
)

private fun ServiceForm.validated() = copy(
    submitted = true,
    nameError = Validators.required(name, "Ingresa el nombre del servicio"),
    radiusError = Validators.intInRange(radius, 1, 200)
)

/** Flujo del administrador: ver datos ficticios y gestionar el catálogo de servicios. */
class AdminViewModel(app: Application) : AndroidViewModel(app) {
    private val adminRepository = AdminRepository(app)

    private val _users = MutableStateFlow<List<UserEntity>>(emptyList())
    val users: StateFlow<List<UserEntity>> = _users.asStateFlow()

    private val _providers = MutableStateFlow(0)
    val providers: StateFlow<Int> = _providers.asStateFlow()

    private val _profiles = MutableStateFlow<List<ProfileSummary>>(emptyList())
    val profiles: StateFlow<List<ProfileSummary>> = _profiles.asStateFlow()

    private val _services = MutableStateFlow<List<ServiceEntity>>(emptyList())
    val services: StateFlow<List<ServiceEntity>> = _services.asStateFlow()

    private val _form = MutableStateFlow(ServiceForm())
    val form: StateFlow<ServiceForm> = _form.asStateFlow()

    private val _detailUser = MutableStateFlow<UserEntity?>(null)
    val detailUser: StateFlow<UserEntity?> = _detailUser.asStateFlow()

    private val _detailNeeds = MutableStateFlow<List<NeedEntity>>(emptyList())
    val detailNeeds: StateFlow<List<NeedEntity>> = _detailNeeds.asStateFlow()

    private val _detailProfile = MutableStateFlow<List<ProviderProfileEntity>>(emptyList())
    val detailProfile: StateFlow<List<ProviderProfileEntity>> = _detailProfile.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun setUser(u: UserEntity?) {
        if (u?.role == Roles.ADMIN) refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _users.value = adminRepository.allUsers()
            _providers.value = adminRepository.providerCount()
            _profiles.value = adminRepository.allProviderSummaries()
            _services.value = adminRepository.allServices()
        }
    }

    // ---------- Perfiles de usuarios ----------
    private suspend fun reloadDetail() {
        val u = _detailUser.value ?: return
        _detailNeeds.value = if (u.role == Roles.PRODUCER) adminRepository.needsForProducer(u.id) else emptyList()
        _detailProfile.value = if (u.role == Roles.PROVIDER) adminRepository.profilesForProvider(u.id) else emptyList()
    }

    /** Abre el perfil del dueño de una prestación (para verla o eliminarla desde el detalle). */
    fun openProfileOwner(userId: Long) {
        _users.value.firstOrNull { it.id == userId }?.let { openUser(it) }
    }

    fun openUser(u: UserEntity) {
        _detailUser.value = u
        _detailNeeds.value = emptyList()
        _detailProfile.value = emptyList()
        viewModelScope.launch { reloadDetail() }
    }

    /** Borra una cuenta (no admin) junto con sus peticiones, prestaciones e intereses. */
    fun deleteUser(u: UserEntity, onDone: () -> Unit) {
        if (u.role == Roles.ADMIN) return
        viewModelScope.launch {
            adminRepository.deleteUser(u.id)
            _detailUser.value = null
            _detailNeeds.value = emptyList()
            _detailProfile.value = emptyList()
            refresh()
            onDone()
        }
    }

    /** Borra una petición de un productor junto con los intereses asociados. */
    fun deleteNeed(id: Long) {
        viewModelScope.launch {
            adminRepository.deleteNeed(id)
            reloadDetail()
            refresh()
            _message.value = "Petición eliminada."
        }
    }

    /** Borra una prestación; si era la última del prestador, borra también sus intereses. */
    fun deleteProfile(profile: ProviderProfileEntity) {
        viewModelScope.launch {
            adminRepository.deleteProfile(profile)
            reloadDetail()
            refresh()
            _message.value = "Prestación eliminada."
        }
    }

    fun onFormChange(name: String = _form.value.name, radius: String = _form.value.radius) {
        val next = _form.value.copy(name = name, radius = radius)
        _form.value = if (next.submitted) next.validated() else next
    }

    fun saveService() {
        val f = _form.value.validated()
        _form.value = f
        if (f.nameError != null || f.radiusError != null) return
        viewModelScope.launch {
            adminRepository.insertService(
                ServiceEntity(name = f.name.trim(), criteria = "zona, cultivo, capacidad, disponibilidad", radiusKm = f.radius.trim().toInt())
            )
            _form.value = ServiceForm()
            refresh()
            _message.value = "Servicio guardado en el catálogo."
        }
    }

    fun dismissMessage() {
        _message.value = null
    }
}
