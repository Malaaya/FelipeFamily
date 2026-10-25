package com.duoc.felipesfamily.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.duoc.felipesfamily.R
import com.duoc.felipesfamily.data.Roles
import com.duoc.felipesfamily.ui.AppTextField
import com.duoc.felipesfamily.ui.OutlineButton
import com.duoc.felipesfamily.ui.PrimaryButton
import com.duoc.felipesfamily.ui.ScreenScaffold
import com.duoc.felipesfamily.viewmodel.AuthViewModel

@Composable
fun LoginScreen(vm: AuthViewModel, onRegister: () -> Unit, onRecover: () -> Unit) {
    val s by vm.login.collectAsState()
    ScreenScaffold(title = stringResource(R.string.app_name)) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.hero_campo),
                contentDescription = "Campo agrícola con dron",
                modifier = Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
            AppTextField(
                label = "Usuario", value = s.alias, onValueChange = { vm.onLoginChange(alias = it) },
                error = s.aliasError, valid = s.submitted && s.aliasError == null, leadingIcon = Icons.Filled.Person
            )
            AppTextField(
                label = "Contraseña", value = s.password, onValueChange = { vm.onLoginChange(password = it) },
                error = s.passwordError, valid = s.submitted && s.passwordError == null,
                isPassword = true, leadingIcon = Icons.Filled.Lock
            )
            AnimatedVisibility(visible = s.formError != null) {
                Text(s.formError ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            PrimaryButton("Login", onClick = vm::login)
            TextButton(onClick = onRecover, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Recuperar contraseña")
            }
            OutlineButton("Crear una cuenta", onClick = onRegister)
            Text(
                "Cuentas demo (clave 123456): productor_demo01 · prestador_demo01 · admin_demo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(vm: AuthViewModel, onBack: () -> Unit, onDone: () -> Unit) {
    val s by vm.register.collectAsState()
    ScreenScaffold(title = "Crear tu cuenta", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Roles.PRODUCER to "Soy productor", Roles.PROVIDER to "Soy prestador").forEach { (role, label) ->
                    FilterChip(
                        selected = s.role == role,
                        onClick = { vm.onRegisterChange(role = role) },
                        label = { Text(label) }
                    )
                }
            }
            Text(
                "Usa solo datos ficticios: no ingreses RUT, teléfono ni correo reales.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AppTextField(
                label = "Alias ficticio", value = s.alias, onValueChange = { vm.onRegisterChange(alias = it) },
                placeholder = "Ej: agricultor_demo01", error = s.aliasError,
                valid = s.submitted && s.aliasError == null, leadingIcon = Icons.Filled.Person
            )
            AppTextField(
                label = "Zona referencial", value = s.zone, onValueChange = { vm.onRegisterChange(zone = it) },
                placeholder = "Ej: Zona Sur-2", error = s.zoneError,
                valid = s.submitted && s.zoneError == null, leadingIcon = Icons.Filled.LocationOn
            )
            AppTextField(
                label = "Contraseña", value = s.password, onValueChange = { vm.onRegisterChange(password = it) },
                placeholder = "Mínimo 6 caracteres", error = s.passwordError,
                valid = s.submitted && s.passwordError == null, isPassword = true, leadingIcon = Icons.Filled.Lock
            )
            AppTextField(
                label = "Repetir contraseña", value = s.repeat, onValueChange = { vm.onRegisterChange(repeat = it) },
                error = s.repeatError, valid = s.submitted && s.repeatError == null,
                isPassword = true, leadingIcon = Icons.Filled.Lock
            )
            PrimaryButton("Registrarse", onClick = vm::register)
        }
    }
    if (s.success) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Cuenta creada") },
            text = { Text("Ya puedes iniciar sesión con tu alias.") },
            confirmButton = {
                TextButton(onClick = { vm.resetRegister(); onDone() }) { Text("Ir al login") }
            }
        )
    }
}

@Composable
fun RecoverScreen(vm: AuthViewModel, onBack: () -> Unit) {
    val s by vm.recover.collectAsState()
    ScreenScaffold(title = "Recuperar contraseña", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Ingresa tu usuario ficticio para restablecer la contraseña de demostración.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AppTextField(
                label = "Usuario", value = s.alias, onValueChange = vm::onRecoverChange,
                placeholder = "agricultor_demo01", error = s.error,
                valid = s.sent, leadingIcon = Icons.Filled.Person
            )
            AnimatedVisibility(visible = s.sent) {
                Text("Listo: enviamos el enlace (simulado).", color = MaterialTheme.colorScheme.primary)
            }
            PrimaryButton("Enviar enlace", onClick = vm::recover)
        }
    }
}
