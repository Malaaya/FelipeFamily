package com.duoc.felipesfamily.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.duoc.felipesfamily.data.AppDatabase
import com.duoc.felipesfamily.data.NeedEntity
import com.duoc.felipesfamily.data.ProviderProfileEntity
import com.duoc.felipesfamily.data.Roles
import com.duoc.felipesfamily.data.ServiceEntity
import com.duoc.felipesfamily.data.UserEntity
import com.duoc.felipesfamily.util.CatalogImporter
import com.duoc.felipesfamily.util.Validators
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private val db = AppDatabase.get(app)

    private val _users = MutableStateFlow<List<UserEntity>>(emptyList())
    val users: StateFlow<List<UserEntity>> = _users.asStateFlow()

    private val _providers = MutableStateFlow(0)
    val providers: StateFlow<Int> = _providers.asStateFlow()

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
            _users.value = db.userDao().all()
            _providers.value = db.providerDao().count()
            _services.value = db.serviceDao().all()
        }
    }

    // ---------- Perfiles de usuarios ----------
    private suspend fun reloadDetail() {
        val u = _detailUser.value ?: return
        _detailNeeds.value = if (u.role == Roles.PRODUCER) db.needDao().byProducer(u.id) else emptyList()
        _detailProfile.value = if (u.role == Roles.PROVIDER) db.providerDao().profiles(u.id) else emptyList()
    }

    fun openUser(u: UserEntity) {
        _detailUser.value = u
        _detailNeeds.value = emptyList()
        _detailProfile.value = emptyList()
        viewModelScope.launch { reloadDetail() }
    }

    /** Borra una petición de un productor junto con los intereses asociados. */
    fun deleteNeed(id: Long) {
        viewModelScope.launch {
            db.interestDao().deleteByNeed(id)
            db.needDao().deleteById(id)
            reloadDetail()
            refresh()
            _message.value = "Petición eliminada."
        }
    }

    /** Borra una prestación; si era la última del prestador, borra también sus intereses. */
    fun deleteProfile(profile: ProviderProfileEntity) {
        viewModelScope.launch {
            db.providerDao().delete(profile.id)
            if (db.providerDao().countByUser(profile.userId) == 0) db.interestDao().deleteByProvider(profile.userId)
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
            db.serviceDao().insert(
                ServiceEntity(name = f.name.trim(), criteria = "zona, cultivo, capacidad, disponibilidad", radiusKm = f.radius.trim().toInt())
            )
            _form.value = ServiceForm()
            refresh()
            _message.value = "Servicio guardado en el catálogo."
        }
    }

    /** Importa servicios desde un archivo JSON elegido por el usuario (recurso nativo: archivos). */
    fun importCatalog(uri: Uri) {
        viewModelScope.launch {
            _message.value = try {
                val items = withContext(Dispatchers.IO) { CatalogImporter.read(getApplication(), uri) }
                var added = 0
                for (item in items) {
                    val exists = db.serviceDao().all().any { it.name.equals(item.name, ignoreCase = true) }
                    if (item.name.isNotBlank() && item.radiusKm in 1..200 && !exists) {
                        db.serviceDao().insert(ServiceEntity(name = item.name, criteria = item.criteria, radiusKm = item.radiusKm))
                        added++
                    }
                }
                refresh()
                "Se importaron $added de ${items.size} servicios."
            } catch (e: Exception) {
                "No se pudo importar: el archivo no tiene el formato esperado."
            }
        }
    }

    fun dismissMessage() {
        _message.value = null
    }
}
