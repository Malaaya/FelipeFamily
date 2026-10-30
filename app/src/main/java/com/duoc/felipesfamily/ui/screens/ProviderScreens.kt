package com.duoc.felipesfamily.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duoc.felipesfamily.data.ProviderProfileEntity
import com.duoc.felipesfamily.data.UserEntity
import com.duoc.felipesfamily.ui.theme.AccentOrange
import com.duoc.felipesfamily.ui.theme.ErrorRed
import com.duoc.felipesfamily.ui.theme.SuccessGreen
import com.duoc.felipesfamily.data.InterestStatus
import com.duoc.felipesfamily.ui.*
import com.duoc.felipesfamily.viewmodel.ProviderViewModel

@Composable
fun ProviderInterestsScreen(vm: ProviderViewModel, bar: @Composable () -> Unit, onOpen: () -> Unit) {
    val list by vm.interests.collectAsState()
    LaunchedEffect(Unit) { vm.refresh() }
    ScreenScaffold(title = "Productores interesados", bottomBar = bar) { padding ->
        ScrollColumn(padding) {
            if (list.isEmpty()) {
                Text(
                    "Aún no tienes interesados. Cuando un productor te elija, aparecerá aquí.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            list.forEach { i ->
                AppCard(
                    title = "${i.producerAlias} · ${i.service}",
                    status = i.status,
                    statusColor = statusColor(i.status),
                    lines = listOf("${fmtHa(i.surfaceHa)} ha  ·  ${i.crop}  ·  ${i.zone}", i.period),
                    onClick = { vm.select(i); onOpen() }
                )
            }
        }
    }
}

@Composable
fun InterestDetailScreen(vm: ProviderViewModel, bar: @Composable () -> Unit, onBack: () -> Unit, onDone: () -> Unit) {
    val info by vm.selected.collectAsState()
    ScreenScaffold(title = "Detalle de necesidad", onBack = onBack, bottomBar = bar) { padding ->
        ScrollColumn(padding) {
            info?.let { i ->
                AppCard(title = i.producerAlias, status = i.status, statusColor = statusColor(i.status), lines = listOf("Necesidad de ${i.service}"))
                InfoCard(
                    listOf(
                        "Servicio" to i.service,
                        "Cultivo" to i.crop,
                        "Superficie" to "${fmtHa(i.surfaceHa)} ha",
                        "Zona" to "${i.zone} (referencial)",
                        "Ventana" to i.period,
                        "Producto" to i.product
                    )
                )
                ZoneMap(listOf(MapPoint(i.producerAlias, "Necesidad de ${i.service} · ${i.zone}", i.zone, isNeed = true)))
                Text(
                    "Vinculación simulada: este MVP no incluye contratación ni pagos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (i.status == InterestStatus.SENT) {
                    PrimaryButton("Aceptar vinculación (simulada)", onClick = { vm.decide(true, onDone) }, accent = true)
                    OutlineButton("Rechazar", onClick = { vm.decide(false, onDone) })
                }
            }
        }
    }
}

@Composable
private fun ServiceCard(p: ProviderProfileEntity, onEdit: () -> Unit, onToggle: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(p.displayName.ifBlank { p.services }, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(
                    if (p.active) "ACTIVA" else "DESACTIVADA",
                    color = if (p.active) SuccessGreen else AccentOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold
                )
            }
            val lines = listOfNotNull(
                p.services.takeIf { p.displayName.isNotBlank() },
                "Cobertura ${p.coverageKm} km  ·  ${p.capacityHaDay} ha por día",
                "Disponible ${p.availability}",
                "No aparece en el matching mientras esté desactivada.".takeIf { !p.active }
            )
            lines.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onEdit) { Text("Editar") }
                TextButton(onClick = onToggle) { Text(if (p.active) "Desactivar" else "Activar") }
                TextButton(onClick = onDelete) { Text("Eliminar", color = ErrorRed) }
            }
        }
    }
}

@Composable
fun ProviderServicesScreen(vm: ProviderViewModel, bar: @Composable () -> Unit, onNew: () -> Unit, onEdit: () -> Unit) {
    val list by vm.profiles.collectAsState()
    var pendingDelete by remember { mutableStateOf<ProviderProfileEntity?>(null) }
    LaunchedEffect(Unit) { vm.refresh() }

    ScreenScaffold(title = "Mis prestaciones", bottomBar = bar) { padding ->
        ScrollColumn(padding) {
            PrimaryButton("Nueva prestación", onClick = { vm.startNew(); onNew() })
            if (list.isEmpty()) {
                Text("Aún no has subido prestaciones. Crea la primera para aparecer en el matching de los productores.")
            }
            list.forEach { p ->
                ServiceCard(
                    p,
                    onEdit = { vm.startEdit(p); onEdit() },
                    onToggle = { vm.toggleActive(p) },
                    onDelete = { pendingDelete = p }
                )
            }
        }
    }
    pendingDelete?.let { p ->
        ConfirmDialog(
            "Eliminar prestación", "Se borrará \"${p.displayName.ifBlank { p.services }}\". Esta acción no se puede deshacer.", "Eliminar",
            { pendingDelete = null; vm.deleteService(p) }, { pendingDelete = null }
        )
    }
}

@Composable
fun ProviderFormScreen(vm: ProviderViewModel, bar: @Composable () -> Unit, onBack: () -> Unit) {
    val f by vm.form.collectAsState()
    val saved by vm.saved.collectAsState()
    val rating by vm.ratingValue.collectAsState()
    val editing by vm.isEditing.collectAsState()
    ScreenScaffold(title = if (editing) "Editar prestación" else "Nueva prestación", onBack = onBack, bottomBar = bar) { padding ->
        ScrollColumn(padding) {
            AppTextField(
                label = "Nombre de tu servicio (opcional)", value = f.name, onValueChange = { vm.onFormChange(name = it) },
                placeholder = "Ej: Drones Valle Sur", error = f.nameError, valid = f.submitted && f.nameError == null
            )
            AppTextField(
                label = "Servicios ofrecidos", value = f.services, onValueChange = { vm.onFormChange(services = it) },
                placeholder = "Ej: Fumigación con dron", error = f.servicesError, valid = f.submitted && f.servicesError == null
            )
            AppTextField(
                label = "Cobertura (km)", value = f.coverage, onValueChange = { vm.onFormChange(coverage = it) },
                error = f.coverageError, valid = f.submitted && f.coverageError == null
            )
            AppTextField(
                label = "Capacidad (ha por día)", value = f.capacity, onValueChange = { vm.onFormChange(capacity = it) },
                error = f.capacityError, valid = f.submitted && f.capacityError == null
            )
            AppTextField(
                label = "Experiencia (años)", value = f.experience, onValueChange = { vm.onFormChange(experience = it) },
                error = f.experienceError, valid = f.submitted && f.experienceError == null
            )
            AppTextField(
                label = "Disponibilidad", value = f.availability, onValueChange = { vm.onFormChange(availability = it) },
                placeholder = "dd/mm - dd/mm", error = f.availabilityError, valid = f.submitted && f.availabilityError == null
            )
            Text("Calificación demostrativa: ${"%.1f".format(rating)} ★", color = MaterialTheme.colorScheme.secondary)
            PrimaryButton("Guardar prestación", onClick = vm::saveProfile)
        }
    }
    if (saved) {
        AlertDialog(
            onDismissRequest = { vm.dismissSaved(); onBack() },
            title = { Text("Prestación guardada") },
            text = { Text("Tu prestación ya puede aparecer en el matching de los productores.") },
            confirmButton = { TextButton(onClick = { vm.dismissSaved(); onBack() }) { Text("Listo") } }
        )
    }
}

@Composable
fun ProviderProfileScreen(user: UserEntity?, bar: @Composable () -> Unit, onLogout: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    var help by remember { mutableStateOf(false) }
    ScreenScaffold(title = "Perfil", bottomBar = bar) { padding ->
        ScrollColumn(padding) {
            InfoCard(
                listOf(
                    "Alias" to (user?.alias ?: ""),
                    "Tipo de perfil" to "Prestador de servicios",
                    "Zona referencial" to (user?.zone ?: "")
                )
            )
            OutlineButton("Ayuda y soporte", onClick = { help = true })
            OutlineButton("Cerrar sesión", onClick = { confirm = true })
        }
    }
    if (help) SupportDialog(onDismiss = { help = false })
    if (confirm) {
        ConfirmDialog("Cerrar sesión", "¿Seguro que quieres salir?", "Salir", { confirm = false; onLogout() }, { confirm = false })
    }
}
