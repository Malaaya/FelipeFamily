package com.duoc.felipesfamily.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.duoc.felipesfamily.ui.theme.AccentOrange
import com.duoc.felipesfamily.ui.theme.DarkGreen
import com.duoc.felipesfamily.ui.theme.OnAccent
import com.duoc.felipesfamily.ui.theme.SuccessGreen

/** Campo de texto con ícono de estado (error / válido) y mensaje animado. */
@Composable
fun AppTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    valid: Boolean = false,
    isPassword: Boolean = false,
    leadingIcon: ImageVector? = null,
    placeholder: String = ""
) {
    Column(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            placeholder = if (placeholder.isNotEmpty()) { { Text(placeholder) } } else null,
            singleLine = true,
            isError = error != null,
            leadingIcon = if (leadingIcon != null) { { Icon(leadingIcon, contentDescription = null) } } else null,
            trailingIcon = when {
                error != null -> { { Icon(Icons.Filled.Warning, "Error", tint = MaterialTheme.colorScheme.error) } }
                valid -> { { Icon(Icons.Filled.CheckCircle, "Campo válido", tint = SuccessGreen) } }
                else -> null
            },
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text),
            shape = RoundedCornerShape(12.dp)
        )
        AnimatedVisibility(visible = error != null) {
            Text(
                text = error ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, accent: Boolean = false) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(50),
        colors = if (accent) ButtonDefaults.buttonColors(containerColor = AccentOrange, contentColor = OnAccent)
        else ButtonDefaults.buttonColors()
    ) { Text(text, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun OutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(50)
    ) { Text(text, fontWeight = FontWeight.SemiBold) }
}

/** Alternador de dos o más opciones: la elegida se ve rellena y las otras con borde. */
@Composable
fun ToggleButtons(options: List<String>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val itemModifier = Modifier.weight(1f).height(44.dp)
            if (option == selected) {
                Button(onClick = { onSelect(option) }, modifier = itemModifier, shape = RoundedCornerShape(50)) {
                    Text(option, fontWeight = FontWeight.SemiBold)
                }
            } else {
                OutlinedButton(onClick = { onSelect(option) }, modifier = itemModifier, shape = RoundedCornerShape(50)) {
                    Text(option)
                }
            }
        }
    }
}

/** Estructura común de pantalla: barra superior verde + contenido. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkGreen,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = bottomBar,
        content = content
    )
}

/** Modal de confirmación reutilizable. */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

/** Centro de ayuda simulado: el usuario escribe un mensaje y "se envía" a soporte (no sale de la app). */
@Composable
fun SupportDialog(onDismiss: () -> Unit) {
    var message by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var sent by remember { mutableStateOf(false) }
    if (sent) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Mensaje enviado") },
            text = { Text("Recibimos tu mensaje en el centro de ayuda (simulado). En este MVP no hay respuesta real de soporte.") },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Entendido") } }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Ayuda y soporte") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Cuéntanos tu problema o consulta y te contactaremos.")
                    AppTextField(label = "Tu mensaje", value = message, onValueChange = { message = it; error = null }, error = error)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (message.trim().length < 10) error = "Escribe al menos 10 caracteres" else sent = true
                }) { Text("Enviar") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
        )
    }
}
