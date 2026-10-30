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
    servicesError = Validators.required(services, "Indica al menos un servicio"),
    coverageError = Validators.intInRange(coverage, 1, 500),
    capacityError = Validators.intInRange(capacity, 1, 1000),
    experienceError = Validators.intInRange(experience, 0, 60),
    availabilityError = Validators.period(availability)
)

private val ProviderForm.hasErrors
    get() = servicesError != null || coverageError != null || capacityError != null ||
        experienceError != null || availabilityError != null

/** Flujo del prestador: mantener su perfil y responder a los productores interesados. */
class ProviderViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private var user: UserEntity? = null
    private var rating = 4.5

    private val _form = MutableStateFlow(ProviderForm())
    val form: StateFlow<ProviderForm> = _form.asStateFlow()

    private val _interests = MutableStateFlow<List<InterestInfo>>(emptyList())
    val interests: StateFlow<List<InterestInfo>> = _interests.asStateFlow()

    private val _selected = MutableStateFlow<InterestInfo?>(null)
    val selected: StateFlow<InterestInfo?> = _selected.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    private val _hasProfile = MutableStateFlow(false)
    val hasProfile: StateFlow<Boolean> = _hasProfile.asStateFlow()

    private val _active = MutableStateFlow(true)
    val active: StateFlow<Boolean> = _active.asStateFlow()

    private val _rating = MutableStateFlow(4.5)
    val ratingValue: StateFlow<Double> = _rating.asStateFlow()

    fun setUser(u: UserEntity?) {
        user = u?.takeIf { it.role == Roles.PROVIDER }
        _interests.value = emptyList()
        _selected.value = null
        _form.value = ProviderForm()
        _hasProfile.value = false
        _active.value = true
        val current = user ?: return
        viewModelScope.launch {
            db.providerDao().profile(current.id)?.let { p ->
                rating = p.rating
                _rating.value = p.rating
                _hasProfile.value = true
                _active.value = p.active
                _form.value = ProviderForm(
                    services = p.services, coverage = p.coverageKm.toString(),
                    capacity = p.capacityHaDay.toString(), experience = p.experienceYears.toString(),
                    availability = p.availability
                )
            }
            loadInterests()
        }
    }

    private suspend fun loadInterests() {
        val u = user ?: return
        _interests.value = db.interestDao().byProvider(u.id)
    }

    fun refresh() {
        viewModelScope.launch { loadInterests() }
    }

    fun onFormChange(
        services: String = _form.value.services,
        coverage: String = _form.value.coverage,
        capacity: String = _form.value.capacity,
        experience: String = _form.value.experience,
        availability: String = _form.value.availability
    ) {
        val next = _form.value.copy(
            services = services, coverage = coverage, capacity = capacity,
            experience = experience, availability = availability
        )
        _form.value = if (next.submitted) next.validated() else next
    }

    fun saveProfile() {
        val u = user ?: return
        val f = _form.value.validated()
        _form.value = f
        if (f.hasErrors) return
        viewModelScope.launch {
            db.providerDao().save(
                ProviderProfileEntity(
                    userId = u.id, services = f.services.trim(),
                    coverageKm = f.coverage.trim().toInt(), capacityHaDay = f.capacity.trim().toInt(),
                    experienceYears = f.experience.trim().toInt(), availability = f.availability.trim(),
                    rating = rating, active = _active.value
                )
            )
            _hasProfile.value = true
            _saved.value = true
        }
    }

    /** Activa o desactiva la prestación (desactivada no aparece en el matching). */
    fun toggleActive() {
        val u = user ?: return
        if (!_hasProfile.value) return
        viewModelScope.launch {
            val next = !_active.value
            db.providerDao().setActive(u.id, next)
            _active.value = next
        }
    }

    /** Borra la prestación creada y los intereses asociados; el prestador puede crear otra después. */
    fun deleteService() {
        val u = user ?: return
        viewModelScope.launch {
            db.interestDao().deleteByProvider(u.id)
            db.providerDao().delete(u.id)
            _hasProfile.value = false
            _active.value = true
            _form.value = ProviderForm()
            _interests.value = emptyList()
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
