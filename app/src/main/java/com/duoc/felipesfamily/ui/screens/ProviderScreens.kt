package com.duoc.felipesfamily.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
fun ProviderProfileScreen(vm: ProviderViewModel, bar: @Composable () -> Unit, onLogout: () -> Unit) {
    val f by vm.form.collectAsState()
    val saved by vm.saved.collectAsState()
    val rating by vm.ratingValue.collectAsState()
    var confirm by remember { mutableStateOf(false) }
    ScreenScaffold(title = "Mi perfil de servicios", bottomBar = bar) { padding ->
        ScrollColumn(padding) {
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
            PrimaryButton("Guardar perfil", onClick = vm::saveProfile)
            OutlineButton("Cerrar sesión", onClick = { confirm = true })
        }
    }
    if (saved) {
        AlertDialog(
            onDismissRequest = vm::dismissSaved,
            title = { Text("Perfil guardado") },
            text = { Text("Tus servicios ya pueden aparecer en el matching de los productores.") },
            confirmButton = { TextButton(onClick = vm::dismissSaved) { Text("Listo") } }
        )
    }
    if (confirm) {
        ConfirmDialog("Cerrar sesión", "¿Seguro que quieres salir?", "Salir", { confirm = false; onLogout() }, { confirm = false })
    }
}
