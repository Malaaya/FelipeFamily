package com.duoc.felipesfamily.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.duoc.felipesfamily.data.AppDatabase
import com.duoc.felipesfamily.data.InterestInfo
import com.duoc.felipesfamily.data.InterestStatus
import com.duoc.felipesfamily.data.ProviderProfileEntity
import com.duoc.felipesfamily.data.Roles
import com.duoc.felipesfamily.data.UserEntity
import com.duoc.felipesfamily.util.NotificationHelper
import com.duoc.felipesfamily.util.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProviderForm(
    val name: String = "",
    val nameError: String? = null,
    val services: String = "",
    val coverage: String = "",
    val capacity: String = "",
    val experience: String = "",
    val availability: String = "",
    val submitted: Boolean = false,
    val servicesError: String? = null,
    val coverageError: String? = null,
    val capacityError: String? = null,
    val experienceError: String? = null,
    val availabilityError: String? = null
)

private fun ProviderForm.validated() = copy(
    submitted = true,
    nameError = if (name.trim().length > 30) "Máximo 30 caracteres" else null,
    servicesError = Validators.required(services, "Indica al menos un servicio"),
    coverageError = Validators.intInRange(coverage, 1, 500),
    capacityError = Validators.intInRange(capacity, 1, 1000),
    experienceError = Validators.intInRange(experience, 0, 60),
    availabilityError = Validators.period(availability)
)

private val ProviderForm.hasErrors
    get() = nameError != null || servicesError != null || coverageError != null || capacityError != null ||
        experienceError != null || availabilityError != null

/** Flujo del prestador: administrar sus prestaciones y responder a los productores interesados. */
class ProviderViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private var user: UserEntity? = null
    private var editing: ProviderProfileEntity? = null

    private val _form = MutableStateFlow(ProviderForm())
    val form: StateFlow<ProviderForm> = _form.asStateFlow()

    private val _interests = MutableStateFlow<List<InterestInfo>>(emptyList())
    val interests: StateFlow<List<InterestInfo>> = _interests.asStateFlow()

    private val _selected = MutableStateFlow<InterestInfo?>(null)
    val selected: StateFlow<InterestInfo?> = _selected.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    /** Prestaciones ya creadas por el prestador. */
    private val _profiles = MutableStateFlow<List<ProviderProfileEntity>>(emptyList())
    val profiles: StateFlow<List<ProviderProfileEntity>> = _profiles.asStateFlow()

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing.asStateFlow()

    private val _rating = MutableStateFlow(4.5)
    val ratingValue: StateFlow<Double> = _rating.asStateFlow()

    fun setUser(u: UserEntity?) {
        user = u?.takeIf { it.role == Roles.PROVIDER }
        editing = null
        _isEditing.value = false
        _interests.value = emptyList()
        _selected.value = null
        _profiles.value = emptyList()
        _form.value = ProviderForm()
        if (user == null) return
        viewModelScope.launch {
            loadProfiles()
            loadInterests()
        }
    }

    private suspend fun loadProfiles() {
        val u = user ?: return
        _profiles.value = db.providerDao().profiles(u.id)
    }

    private suspend fun loadInterests() {
        val u = user ?: return
        _interests.value = db.interestDao().byProvider(u.id)
    }

    fun refresh() {
        viewModelScope.launch {
            loadProfiles()
            loadInterests()
        }
    }

    /** Prepara el formulario para crear una prestación nueva. */
    fun startNew() {
        editing = null
        _isEditing.value = false
        _rating.value = 4.5
        _form.value = ProviderForm()
    }

    /** Prepara el formulario para editar una prestación existente. */
    fun startEdit(p: ProviderProfileEntity) {
        editing = p
        _isEditing.value = true
        _rating.value = p.rating
        _form.value = ProviderForm(
            name = p.displayName, services = p.services, coverage = p.coverageKm.toString(),
            capacity = p.capacityHaDay.toString(), experience = p.experienceYears.toString(),
            availability = p.availability
        )
    }

    fun onFormChange(
        name: String = _form.value.name,
        services: String = _form.value.services,
        coverage: String = _form.value.coverage,
        capacity: String = _form.value.capacity,
        experience: String = _form.value.experience,
        availability: String = _form.value.availability
    ) {
        val next = _form.value.copy(
            name = name, services = services, coverage = coverage, capacity = capacity,
            experience = experience, availability = availability
        )
        _form.value = if (next.submitted) next.validated() else next
    }

    fun saveProfile() {
        val u = user ?: return
        val f = _form.value.validated()
        _form.value = f
        if (f.hasErrors) return
        val current = editing
        viewModelScope.launch {
            db.providerDao().save(
                ProviderProfileEntity(
                    id = current?.id ?: 0, userId = u.id, services = f.services.trim(),
                    coverageKm = f.coverage.trim().toInt(), capacityHaDay = f.capacity.trim().toInt(),
                    experienceYears = f.experience.trim().toInt(), availability = f.availability.trim(),
                    rating = current?.rating ?: 4.5, active = current?.active ?: true, displayName = f.name.trim()
                )
            )
            loadProfiles()
            _saved.value = true
        }
    }

    /** Activa o desactiva una prestación (desactivada no aparece en el matching). */
    fun toggleActive(p: ProviderProfileEntity) {
        viewModelScope.launch {
            db.providerDao().setActive(p.id, !p.active)
            loadProfiles()
        }
    }

    /** Borra una prestación; si era la última, borra también los intereses recibidos. */
    fun deleteService(p: ProviderProfileEntity) {
        val u = user ?: return
        viewModelScope.launch {
            db.providerDao().delete(p.id)
            if (db.providerDao().countByUser(u.id) == 0) db.interestDao().deleteByProvider(u.id)
            loadProfiles()
            loadInterests()
            _selected.value = null
        }
    }

    fun dismissSaved() {
        _saved.value = false
    }

    fun select(info: InterestInfo) {
        _selected.value = info
    }

    fun decide(accept: Boolean, onDone: () -> Unit) {
        val info = _selected.value ?: return
        viewModelScope.launch {
            val status = if (accept) InterestStatus.ACCEPTED else InterestStatus.REJECTED
            db.interestDao().setStatus(info.interestId, status)
            _selected.value = info.copy(status = status)
            loadInterests()
            if (accept) {
                NotificationHelper.show(getApplication(), "Vinculación aceptada (simulada)", "Aceptaste a ${info.producerAlias}.")
            }
            onDone()
        }
    }
}
