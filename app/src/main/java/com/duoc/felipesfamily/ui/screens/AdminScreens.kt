package com.duoc.felipesfamily.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
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

@Composable
private fun MessageDialog(vm: AdminViewModel, message: String?) {
    message?.let {
        AlertDialog(
            onDismissRequest = vm::dismissMessage,
            title = { Text("Catálogo") },
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
    // Recurso nativo: selector de archivos del sistema (no requiere permisos).
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importCatalog(uri)
    }
    LaunchedEffect(Unit) { vm.refresh() }

    ScreenScaffold(title = "Panel administrador", bottomBar = bar) { padding ->
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
            PrimaryButton("Importar catálogo ficticio (archivo)", onClick = { picker.launch(arrayOf("*/*")) }, accent = true)
            OutlineButton("Cerrar sesión", onClick = { confirm = true })
        }
    }
    MessageDialog(vm, message)
    if (confirm) {
        ConfirmDialog("Cerrar sesión", "¿Seguro que quieres salir?", "Salir", { confirm = false; onLogout() }, { confirm = false })
    }
}

@Composable
fun AdminCatalogScreen(vm: AdminViewModel, bar: @Composable () -> Unit) {
    val services by vm.services.collectAsState()
    val f by vm.form.collectAsState()
    val message by vm.message.collectAsState()
    LaunchedEffect(Unit) { vm.refresh() }

    ScreenScaffold(title = "Catálogo y criterios", bottomBar = bar) { padding ->
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
}
