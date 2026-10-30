package com.duoc.felipesfamily.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.duoc.felipesfamily.data.ProviderProfileEntity
import com.duoc.felipesfamily.data.Roles
import com.duoc.felipesfamily.ui.*
import com.duoc.felipesfamily.viewmodel.AdminViewModel

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Botón "Salir" de la barra superior del admin (abre el diálogo de confirmación). */
@Composable
private fun LogoutAction(onClick: () -> Unit) {
    TextButton(onClick = onClick) { Text("Salir", color = Color.White, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun MessageDialog(vm: AdminViewModel, message: String?) {
    message?.let {
        AlertDialog(
            onDismissRequest = vm::dismissMessage,
            title = { Text("Aviso") },
            text = { Text(it) },
            confirmButton = { TextButton(onClick = vm::dismissMessage) { Text("Entendido") } }
        )
    }
}

@Composable
fun AdminPanelScreen(vm: AdminViewModel, bar: @Composable () -> Unit, onCatalog: () -> Unit, onLogout: () -> Unit) {
    val users by vm.users.collectAsState()
    val providers by vm.providers.collectAsState()
    val services by vm.services.collectAsState()
    val message by vm.message.collectAsState()
    var confirm by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.refresh() }

    ScreenScaffold(
        title = "Panel administrador",
        actions = { LogoutAction { confirm = true } },
        bottomBar = bar
    ) { padding ->
        ScrollColumn(padding) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("${users.size}", "Usuarios", Modifier.weight(1f))
                StatCard("$providers", "Prestadores", Modifier.weight(1f))
                StatCard("${services.size}", "Servicios", Modifier.weight(1f))
            }
            SectionTitle("Usuarios ficticios")
            users.take(5).forEach { u ->
                AppCard(title = u.alias, status = u.role, statusColor = MaterialTheme.colorScheme.primary, lines = listOf(u.zone))
            }
            OutlineButton("Gestionar servicios y criterios", onClick = onCatalog)
            OutlineButton("Cerrar sesión", onClick = { confirm = true })
        }
    }
    MessageDialog(vm, message)
    if (confirm) {
        ConfirmDialog("Cerrar sesión", "¿Seguro que quieres salir?", "Salir", { confirm = false; onLogout() }, { confirm = false })
    }
}

@Composable
fun AdminCatalogScreen(vm: AdminViewModel, bar: @Composable () -> Unit, onLogout: () -> Unit) {
    val services by vm.services.collectAsState()
    val f by vm.form.collectAsState()
    val message by vm.message.collectAsState()
    var confirm by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.refresh() }

    ScreenScaffold(
        title = "Catálogo y criterios",
        actions = { LogoutAction { confirm = true } },
        bottomBar = bar
    ) { padding ->
        ScrollColumn(padding) {
            services.forEach { s ->
                AppCard(title = s.name, lines = listOf("Criterios: ${s.criteria}", "Radio de cobertura: ${s.radiusKm} km"))
            }
            SectionTitle("Nuevo servicio")
            AppTextField(
                label = "Nombre del servicio", value = f.name, onValueChange = { vm.onFormChange(name = it) },
                placeholder = "Ej: Siembra con dron", error = f.nameError, valid = f.submitted && f.nameError == null
            )
            AppTextField(
                label = "Radio de cobertura (km)", value = f.radius, onValueChange = { vm.onFormChange(radius = it) },
                placeholder = "1 a 200", error = f.radiusError, valid = f.submitted && f.radiusError == null
            )
            PrimaryButton("Guardar servicio", onClick = vm::saveService)
        }
    }
    MessageDialog(vm, message)
    if (confirm) {
        ConfirmDialog("Cerrar sesión", "¿Seguro que quieres salir?", "Salir", { confirm = false; onLogout() }, { confirm = false })
    }
}

@Composable
fun AdminUsersScreen(vm: AdminViewModel, bar: @Composable () -> Unit, onOpen: () -> Unit, onLogout: () -> Unit) {
    val users by vm.users.collectAsState()
    var confirm by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.refresh() }

    ScreenScaffold(title = "Usuarios", actions = { LogoutAction { confirm = true } }, bottomBar = bar) { padding ->
        ScrollColumn(padding) {
            val list = users.filter { it.role != Roles.ADMIN }
            if (list.isEmpty()) Text("Aún no hay usuarios registrados.")
            list.forEach { u ->
                AppCard(
                    title = u.alias, status = u.role, statusColor = MaterialTheme.colorScheme.primary,
                    lines = listOf(u.zone), onClick = { vm.openUser(u); onOpen() }
                )
            }
        }
    }
    if (confirm) {
        ConfirmDialog("Cerrar sesión", "¿Seguro que quieres salir?", "Salir", { confirm = false; onLogout() }, { confirm = false })
    }
}

@Composable
fun AdminUserDetailScreen(vm: AdminViewModel, bar: @Composable () -> Unit, onBack: () -> Unit, onLogout: () -> Unit) {
    val user by vm.detailUser.collectAsState()
    val needs by vm.detailNeeds.collectAsState()
    val profiles by vm.detailProfile.collectAsState()
    val message by vm.message.collectAsState()
    var confirm by remember { mutableStateOf(false) }
    var pendingNeed by remember { mutableStateOf<Long?>(null) }
    var pendingProfile by remember { mutableStateOf<ProviderProfileEntity?>(null) }
    var deleteAccount by remember { mutableStateOf(false) }

    ScreenScaffold(title = "Perfil de usuario", onBack = onBack, actions = { LogoutAction { confirm = true } }, bottomBar = bar) { padding ->
        ScrollColumn(padding) {
            user?.let { u ->
                InfoCard(listOf("Alias" to u.alias, "Rol" to u.role, "Zona referencial" to u.zone))
                if (u.role == Roles.PROVIDER) {
                    SectionTitle("Prestación de servicios")
                    if (profiles.isEmpty()) Text("Este prestador no tiene prestaciones creadas.")
                    profiles.forEach { p ->
                        InfoCard(
                            listOf(
                                "Nombre" to p.displayName.ifBlank { "—" },
                                "Servicios" to p.services,
                                "Estado" to if (p.active) "Activa" else "Desactivada",
                                "Cobertura" to "${p.coverageKm} km",
                                "Capacidad" to "${p.capacityHaDay} ha/día",
                                "Experiencia" to "${p.experienceYears} años",
                                "Disponibilidad" to p.availability
                            )
                        )
                        OutlineButton("Eliminar prestación", onClick = { pendingProfile = p })
                    }
                } else if (u.role == Roles.PRODUCER) {
                    SectionTitle("Peticiones")
                    if (needs.isEmpty()) Text("Este usuario no tiene peticiones.")
                    needs.forEach { n ->
                        AppCard(
                            title = n.service, status = n.status, statusColor = statusColor(n.status),
                            lines = listOf("${fmtHa(n.surfaceHa)} ha  ·  ${n.crop}  ·  ${n.zone}", n.period)
                        )
                        OutlineButton("Eliminar petición", onClick = { pendingNeed = n.id })
                    }
                }
                OutlineButton("Eliminar cuenta", onClick = { deleteAccount = true })
            }
        }
    }
    MessageDialog(vm, message)
    if (deleteAccount) {
        ConfirmDialog(
            "Eliminar cuenta",
            "Se borrará la cuenta de ${user?.alias ?: ""} con todas sus peticiones, prestaciones e intereses. No se puede deshacer.",
            "Eliminar",
            { deleteAccount = false; user?.let { vm.deleteUser(it, onBack) } },
            { deleteAccount = false }
        )
    }
    pendingNeed?.let { id ->
        ConfirmDialog(
            "Eliminar petición", "Se borrará la petición y los intereses asociados.", "Eliminar",
            { pendingNeed = null; vm.deleteNeed(id) }, { pendingNeed = null }
        )
    }
    pendingProfile?.let { p ->
        ConfirmDialog(
            "Eliminar prestación", "Se borrará esta prestación del prestador.", "Eliminar",
            { pendingProfile = null; vm.deleteProfile(p) }, { pendingProfile = null }
        )
    }
    if (confirm) {
        ConfirmDialog("Cerrar sesión", "¿Seguro que quieres salir?", "Salir", { confirm = false; onLogout() }, { confirm = false })
    }
}
