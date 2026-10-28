package com.duoc.felipesfamily.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duoc.felipesfamily.data.UserEntity
import com.duoc.felipesfamily.ui.*
import com.duoc.felipesfamily.ui.theme.AccentOrange
import com.duoc.felipesfamily.ui.theme.SuccessGreen
import com.duoc.felipesfamily.util.NotificationHelper
import com.duoc.felipesfamily.viewmodel.ProducerViewModel

@Composable
fun ProducerNeedsScreen(vm: ProducerViewModel, bar: @Composable () -> Unit, onNew: () -> Unit, onOpen: () -> Unit) {
    val needs by vm.needs.collectAsState()
    ScreenScaffold(title = "Mis necesidades", bottomBar = bar) { padding ->
        ScrollColumn(padding) {
            if (needs.isEmpty()) {
                Text(
                    "Aún no registras necesidades. Crea la primera con el botón de abajo.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            needs.forEach { need ->
                AppCard(
                    title = "${need.service} · ${need.crop}",
                    status = need.status,
                    statusColor = statusColor(need.status),
                    lines = listOf("${fmtHa(need.surfaceHa)} ha  ·  ${need.zone}  ·  ${need.period}"),
                    onClick = { vm.selectNeed(need, onOpen) }
                )
            }
            PrimaryButton("+ Nueva necesidad", onClick = onNew, accent = true)
        }
    }
}

@Composable
fun NewNeedScreen(vm: ProducerViewModel, bar: @Composable () -> Unit, onBack: () -> Unit, onSaved: () -> Unit) {
    val f by vm.form.collectAsState()
    val services by vm.services.collectAsState()
    val confirming by vm.confirming.collectAsState()
    ScreenScaffold(title = "Nueva necesidad", onBack = onBack, bottomBar = bar) { padding ->
        ScrollColumn(padding) {
            ChoiceChips("Tipo de servicio", services.map { it.name }, f.service, { vm.onFormChange(service = it) }, f.serviceError)
            AppTextField(
                label = "Tipo de cultivo", value = f.crop, onValueChange = { vm.onFormChange(crop = it) },
                error = f.cropError, valid = f.submitted && f.cropError == null, placeholder = "Ej: Trigo"
            )
            AppTextField(
                label = "Superficie (ha)", value = f.surface, onValueChange = { vm.onFormChange(surface = it) },
                error = f.surfaceError, valid = f.submitted && f.surfaceError == null, placeholder = "Ej: 10"
            )
            AppTextField(
                label = "Zona referencial (manual)", value = f.zone, onValueChange = { vm.onFormChange(zone = it) },
                error = f.zoneError, valid = f.submitted && f.zoneError == null, placeholder = "Ej: Zona Sur-2",
                leadingIcon = Icons.Filled.LocationOn
            )
            AppTextField(
                label = "Ventana de disponibilidad", value = f.period, onValueChange = { vm.onFormChange(period = it) },
                error = f.periodError, valid = f.submitted && f.periodError == null, placeholder = "dd/mm - dd/mm",
                leadingIcon = Icons.Filled.DateRange
            )
            ChoiceChips("Tipo de producto", listOf("Granulado", "Líquido"), f.product, { vm.onFormChange(product = it) })
            PrimaryButton("Registrar necesidad", onClick = vm::submitNeed)
        }
    }
    if (confirming) {
        ConfirmDialog(
            title = "¿Registrar necesidad?",
            message = "Buscaremos prestadores compatibles según tu zona, cultivo, superficie y fechas.",
            confirmText = "Registrar",
            onConfirm = { vm.saveNeed(onSaved) },
            onDismiss = vm::cancelConfirm
        )
    }
}

@Composable
fun ResultsScreen(vm: ProducerViewModel, bar: @Composable () -> Unit, onBack: () -> Unit, onCompare: () -> Unit) {
    val need by vm.selected.collectAsState()
    val results by vm.results.collectAsState()
    ScreenScaffold(title = "Resultados de matching", onBack = onBack, bottomBar = bar) { padding ->
        ScrollColumn(padding) {
            need?.let {
                Text(
                    "Necesidad: ${it.service} · ${it.crop} · ${fmtHa(it.surfaceHa)} ha · ${it.zone}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (results.isEmpty()) {
                Text("No hay prestadores compatibles con esta necesidad por ahora.")
            }
            results.forEachIndexed { index, r ->
                AppCard(
                    title = r.provider.alias,
                    status = "${r.score}% compatible",
                    statusColor = if (r.score >= 80) SuccessGreen else AccentOrange,
                    lines = listOf(
                        "${r.provider.services}  ·  Cobertura ${r.provider.coverageKm} km",
                        "Disponible ${r.provider.availability}"
                    ),
                    onClick = { vm.choose(index); onCompare() }
                )
            }
            if (results.isNotEmpty()) {
                PrimaryButton("Comparar prestadores", onClick = { vm.choose(0); onCompare() }, accent = true)
            }
        }
    }
}

@Composable
private fun TableRow(cells: List<String>, header: Boolean = false) {
    Row(Modifier.fillMaxWidth()) {
        cells.forEachIndexed { i, text ->
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                fontSize = 11.sp,
                fontWeight = if (header || i == 0) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    header -> MaterialTheme.colorScheme.primary
                    i == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

private fun shortName(alias: String) = alias.removePrefix("prestador_")

@Composable
fun CompareScreen(vm: ProducerViewModel, bar: @Composable () -> Unit, onBack: () -> Unit, onDone: () -> Unit) {
    val results by vm.results.collectAsState()
    val chosen by vm.chosen.collectAsState()
    val notify by vm.notify.collectAsState()
    val message by vm.message.collectAsState()
    val context = LocalContext.current
    var askPermission by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.setNotify(granted)
    }
    val shown = results.take(3)

    ScreenScaffold(title = "Comparar prestadores", onBack = onBack, bottomBar = bar) { padding ->
        ScrollColumn(padding) {
            if (shown.isEmpty()) {
                Text("Primero necesitas resultados de matching para comparar.")
            } else {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        TableRow(listOf("") + shown.map { shortName(it.provider.alias) }, header = true)
                        TableRow(listOf("Compatib.") + shown.map { "${it.score}%" })
                        TableRow(listOf("Servicio") + shown.map { it.provider.services.substringBefore(',') })
                        TableRow(listOf("Cobertura") + shown.map { "${it.provider.coverageKm} km" })
                        TableRow(listOf("Experiencia") + shown.map { "${it.provider.experienceYears} años" })
                        TableRow(listOf("Capacidad") + shown.map { "${it.provider.capacityHaDay} ha/día" })
                        TableRow(listOf("Disponible") + shown.map { it.provider.availability })
                        TableRow(listOf("Calif. (demo)") + shown.map { "%.1f ★".format(it.provider.rating) })
                    }
                }
                ChoiceChips(
                    label = "Prestador a contactar",
                    options = shown.map { shortName(it.provider.alias) },
                    selected = shortName(shown.getOrNull(chosen)?.provider?.alias ?: ""),
                    onSelect = { name -> vm.choose(shown.indexOfFirst { shortName(it.provider.alias) == name }) }
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Notificarme cuando respondan")
                    Switch(
                        checked = notify,
                        onCheckedChange = { on ->
                            if (!on) vm.setNotify(false)
                            else if (NotificationHelper.canNotify(context)) vm.setNotify(true)
                            else askPermission = true
                        }
                    )
                }
                PrimaryButton("Manifestar interés", onClick = vm::sendInterest, accent = true)
            }
        }
    }
    if (askPermission) {
        ConfirmDialog(
            title = "Permitir notificaciones",
            message = "Te avisaremos cuando un prestador responda a tu interés.",
            confirmText = "Permitir",
            onConfirm = {
                askPermission = false
                if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else vm.setNotify(true)
            },
            onDismiss = { askPermission = false }
        )
    }
    message?.let { text ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Interés enviado (simulado)") },
            text = { Text("$text\nEn este MVP no hay contratación ni pagos.") },
            confirmButton = { TextButton(onClick = { vm.dismissMessage(); onDone() }) { Text("Entendido") } }
        )
    }
}

@Composable
fun ProducerProfileScreen(user: UserEntity?, bar: @Composable () -> Unit, onLogout: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    ScreenScaffold(title = "Perfil", bottomBar = bar) { padding ->
        ScrollColumn(padding) {
            InfoCard(
                listOf(
                    "Alias ficticio" to (user?.alias ?: ""),
                    "Tipo de perfil" to "Productor / agricultor",
                    "Zona referencial" to (user?.zone ?: "")
                )
            )
            OutlineButton("Cerrar sesión", onClick = { confirm = true })
        }
    }
    if (confirm) {
        ConfirmDialog("Cerrar sesión", "¿Seguro que quieres salir?", "Salir", { confirm = false; onLogout() }, { confirm = false })
    }
}
