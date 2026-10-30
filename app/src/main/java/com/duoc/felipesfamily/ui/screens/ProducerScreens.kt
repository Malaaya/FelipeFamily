package com.duoc.felipesfamily.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import com.duoc.felipesfamily.util.MatchResult
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
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
    val alerts by vm.alerts.collectAsState()
    LaunchedEffect(Unit) { vm.refreshAlerts() }
    ScreenScaffold(title = "Mis necesidades", bottomBar = bar) { padding ->
        ScrollColumn(padding) {
            if (alerts.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Nuevas coincidencias (${alerts.size})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        alerts.forEach { Text(it.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer) }
                        TextButton(onClick = vm::markAlertsSeen) { Text("Marcar como leídas") }
                    }
                }
            }
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
            ZoneMap(listOf(MapPoint("Tu zona", f.zone, f.zone, isNeed = true)))
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

private fun initials(name: String): String =
    name.trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifBlank { "?" }

@Composable
private fun CriteriaChip(text: String, ok: Boolean) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (ok) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            (if (ok) "✓ " else "✗ ") + text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 12.sp,
            color = if (ok) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MatchCard(r: MatchResult, onDetail: () -> Unit, onCompare: () -> Unit) {
    val p = r.provider
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initials(p.name), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f)) {
                    Text(p.name, fontWeight = FontWeight.Bold, maxLines = 2)
                    Text(
                        "${p.experienceYears} años · ★ ${"%.1f".format(p.rating)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "${r.score}%", fontSize = 26.sp, fontWeight = FontWeight.Bold,
                    color = if (r.score >= 80) SuccessGreen else AccentOrange
                )
            }
            LinearProgressIndicator(
                progress = { r.score / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                color = if (r.score >= 80) SuccessGreen else AccentOrange
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CriteriaChip(if (r.nearZone) "Zona cercana" else "Zona", r.sameZone || r.nearZone)
                CriteriaChip("Servicio", true)
                CriteriaChip("Capacidad", r.capacityOk)
                CriteriaChip("Disponible", r.available)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onDetail, modifier = Modifier.weight(1f)) { Text("Ver detalle") }
                FilledTonalButton(onClick = onCompare, modifier = Modifier.weight(1f)) { Text("Comparar") }
            }
        }
    }
}

@Composable
fun ResultsScreen(vm: ProducerViewModel, bar: @Composable () -> Unit, onBack: () -> Unit, onCompare: () -> Unit) {
    val need by vm.selected.collectAsState()
    val results by vm.results.collectAsState()
    var filter by remember { mutableIntStateOf(0) }
    var detail by remember { mutableStateOf<MatchResult?>(null) }

    val labels = listOf("Todos", "Mi zona", "Disponibles en mi ventana", "Capacidad suficiente")
    fun matches(i: Int, r: MatchResult): Boolean = when (i) {
        1 -> r.sameZone || r.nearZone
        2 -> r.available
        3 -> r.capacityOk
        else -> true
    }
    val shown = results.filter { matches(filter, it) }

    ScreenScaffold(title = "Resultados de matching", onBack = onBack, bottomBar = bar) { padding ->
        ScrollColumn(padding) {
            need?.let {
                Text(
                    "Necesidad: ${it.service} · ${it.crop} · ${fmtHa(it.surfaceHa)} ha · ${it.zone}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                labels.forEachIndexed { i, label ->
                    FilterChip(
                        selected = filter == i,
                        onClick = { filter = i },
                        label = { Text("$label (${results.count { matches(i, it) }})") }
                    )
                }
            }
            if (results.isEmpty()) {
                Text("No hay prestadores compatibles con esta necesidad por ahora.")
            } else if (shown.isEmpty()) {
                Text("Ningún prestador cumple este filtro. Prueba con otro.")
            }
            shown.forEach { r ->
                val index = results.indexOf(r)
                MatchCard(r, onDetail = { detail = r }, onCompare = { vm.choose(index); onCompare() })
            }
            need?.let { n ->
                if (shown.isNotEmpty()) {
                    Text("Mapa de cobertura", fontWeight = FontWeight.SemiBold)
                    ZoneMap(
                        listOf(MapPoint("Tu necesidad", "${n.service} · ${n.zone}", n.zone, isNeed = true)) +
                            shown.map { r ->
                                MapPoint(r.provider.name, "${r.score}% compatible · cobertura ${r.provider.coverageKm} km", r.provider.zone, coverageKm = r.provider.coverageKm)
                            }
                    )
                }
            }
        }
    }

    detail?.let { r ->
        val p = r.provider
        AlertDialog(
            onDismissRequest = { detail = null },
            title = { Text(p.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Compatibilidad: ${r.score}%")
                    Text("Servicios: ${p.services}")
                    Text("Zona: ${p.zone} · Cobertura ${p.coverageKm} km")
                    Text("Capacidad: ${p.capacityHaDay} ha por día")
                    Text("Experiencia: ${p.experienceYears} años · ★ ${"%.1f".format(p.rating)}")
                    Text("Disponible: ${p.availability}")
                }
            },
            confirmButton = {
                TextButton(onClick = { val i = results.indexOf(r); detail = null; vm.choose(i); onCompare() }) { Text("Comparar") }
            },
            dismissButton = { TextButton(onClick = { detail = null }) { Text("Cerrar") } }
        )
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
                        TableRow(listOf("") + shown.map { it.provider.name }, header = true)
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
                    options = shown.map { it.provider.name },
                    selected = shown.getOrNull(chosen)?.provider?.name ?: "",
                    onSelect = { name -> vm.choose(shown.indexOfFirst { it.provider.name == name }) }
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
    var help by remember { mutableStateOf(false) }
    ScreenScaffold(title = "Perfil", bottomBar = bar) { padding ->
        ScrollColumn(padding) {
            InfoCard(
                listOf(
                    "Alias" to (user?.alias ?: ""),
                    "Tipo de perfil" to "Productor / agricultor",
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
