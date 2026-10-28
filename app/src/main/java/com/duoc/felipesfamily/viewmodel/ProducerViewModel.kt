package com.duoc.felipesfamily.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.duoc.felipesfamily.data.AppDatabase
import com.duoc.felipesfamily.data.InterestEntity
import com.duoc.felipesfamily.data.NeedEntity
import com.duoc.felipesfamily.data.NeedStatus
import com.duoc.felipesfamily.data.Roles
import com.duoc.felipesfamily.data.ServiceEntity
import com.duoc.felipesfamily.data.UserEntity
import com.duoc.felipesfamily.util.Matching
import com.duoc.felipesfamily.util.MatchResult
import com.duoc.felipesfamily.util.NotificationHelper
import com.duoc.felipesfamily.util.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NeedForm(
    val service: String = "",
    val crop: String = "",
    val surface: String = "",
    val zone: String = "",
    val period: String = "",
    val product: String = "Granulado",
    val submitted: Boolean = false,
    val serviceError: String? = null,
    val cropError: String? = null,
    val surfaceError: String? = null,
    val zoneError: String? = null,
    val periodError: String? = null
)

private fun NeedForm.validated() = copy(
    submitted = true,
    serviceError = Validators.required(service, "Selecciona un tipo de servicio"),
    cropError = Validators.required(crop, "Ingresa el tipo de cultivo"),
    surfaceError = Validators.surface(surface),
    zoneError = Validators.zone(zone),
    periodError = Validators.period(period)
)

private val NeedForm.hasErrors
    get() = serviceError != null || cropError != null || surfaceError != null ||
        zoneError != null || periodError != null

/** Flujo del productor: registrar necesidad, ver matching, comparar y manifestar interés. */
class ProducerViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private var user: UserEntity? = null

    private val _needs = MutableStateFlow<List<NeedEntity>>(emptyList())
    val needs: StateFlow<List<NeedEntity>> = _needs.asStateFlow()

    private val _services = MutableStateFlow<List<ServiceEntity>>(emptyList())
    val services: StateFlow<List<ServiceEntity>> = _services.asStateFlow()

    private val _form = MutableStateFlow(NeedForm())
    val form: StateFlow<NeedForm> = _form.asStateFlow()

    private val _confirming = MutableStateFlow(false)
    val confirming: StateFlow<Boolean> = _confirming.asStateFlow()

    private val _selected = MutableStateFlow<NeedEntity?>(null)
    val selected: StateFlow<NeedEntity?> = _selected.asStateFlow()

    private val _results = MutableStateFlow<List<MatchResult>>(emptyList())
    val results: StateFlow<List<MatchResult>> = _results.asStateFlow()

    private val _chosen = MutableStateFlow(0)
    val chosen: StateFlow<Int> = _chosen.asStateFlow()

    private val _notify = MutableStateFlow(false)
    val notify: StateFlow<Boolean> = _notify.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun setUser(u: UserEntity?) {
        user = u?.takeIf { it.role == Roles.PRODUCER }
        _needs.value = emptyList()
        _selected.value = null
        _results.value = emptyList()
        _form.value = NeedForm()
        if (user != null) viewModelScope.launch { loadCatalog(); loadNeeds() }
    }

    private suspend fun loadNeeds() {
        val u = user ?: return
        _needs.value = db.needDao().byProducer(u.id)
    }

    private suspend fun loadCatalog() {
        val list = db.serviceDao().all()
        _services.value = list
        if (_form.value.service.isBlank() && list.isNotEmpty()) {
            _form.update { it.copy(service = list.first().name) }
        }
    }

    fun onFormChange(
        service: String = _form.value.service,
        crop: String = _form.value.crop,
        surface: String = _form.value.surface,
        zone: String = _form.value.zone,
        period: String = _form.value.period,
        product: String = _form.value.product
    ) {
        val next = _form.value.copy(service = service, crop = crop, surface = surface, zone = zone, period = period, product = product)
        _form.value = if (next.submitted) next.validated() else next
    }

    /** Valida el formulario; si está bien, pide confirmación (modal). */
    fun submitNeed() {
        val f = _form.value.validated()
        _form.value = f
        if (!f.hasErrors) _confirming.value = true
    }

    fun cancelConfirm() {
        _confirming.value = false
    }

    fun saveNeed(onDone: () -> Unit) {
        val u = user ?: return
        val f = _form.value
        viewModelScope.launch {
            val need = NeedEntity(
                producerId = u.id, service = f.service, crop = f.crop.trim(),
                surfaceHa = Validators.parseSurface(f.surface) ?: 0.0,
                zone = f.zone.trim(), period = f.period.trim(), product = f.product
            )
            val id = db.needDao().insert(need)
            select(need.copy(id = id))
            _form.value = NeedForm(service = f.service)
            _confirming.value = false
            loadNeeds()
            onDone()
        }
    }

    private suspend fun select(need: NeedEntity) {
        val found = Matching.compute(need, db.providerDao().all())
        var current = need
        if (found.isNotEmpty() && need.status == NeedStatus.NEW) {
            current = need.copy(status = NeedStatus.MATCHED)
            db.needDao().update(current)
        }
        _selected.value = current
        _results.value = found
        _chosen.value = 0
    }

    fun selectNeed(need: NeedEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            select(need)
            loadNeeds()
            onDone()
        }
    }

    fun choose(index: Int) {
        _chosen.value = index
    }

    fun setNotify(enabled: Boolean) {
        _notify.value = enabled
    }

    fun sendInterest() {
        val need = _selected.value ?: return
        val match = _results.value.getOrNull(_chosen.value) ?: return
        viewModelScope.launch {
            db.interestDao().insert(InterestEntity(needId = need.id, providerId = match.provider.userId))
            val updated = need.copy(status = NeedStatus.INTEREST)
            db.needDao().update(updated)
            _selected.value = updated
            loadNeeds()
            val text = "Enviamos tu interés a ${match.provider.alias} (simulado)."
            _message.value = text
            if (_notify.value) NotificationHelper.show(getApplication(), "Interés enviado", text)
        }
    }

    fun dismissMessage() {
        _message.value = null
    }
}
