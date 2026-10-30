package com.duoc.felipesfamily.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.duoc.felipesfamily.data.Roles
import com.duoc.felipesfamily.ui.screens.*
import com.duoc.felipesfamily.viewmodel.AdminViewModel
import com.duoc.felipesfamily.viewmodel.AuthViewModel
import com.duoc.felipesfamily.viewmodel.ProducerViewModel
import com.duoc.felipesfamily.viewmodel.ProviderViewModel

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val RECOVER = "recover"
    const val P_NEEDS = "producer_needs"
    const val P_NEW = "producer_new"
    const val P_RESULTS = "producer_results"
    const val P_COMPARE = "producer_compare"
    const val P_PROFILE = "producer_profile"
    const val V_INTERESTS = "provider_interests"
    const val V_DETAIL = "provider_detail"
    const val V_SERVICES = "provider_services"
    const val V_FORM = "provider_form"
    const val V_PROFILE = "provider_profile"
    const val A_PANEL = "admin_panel"
    const val A_CATALOG = "admin_catalog"
    const val A_USERS = "admin_users"
    const val A_USER_DETAIL = "admin_user_detail"

    val authRoutes = listOf(LOGIN, REGISTER, RECOVER)

    fun homeFor(role: String?): String? = when (role) {
        Roles.PRODUCER -> P_NEEDS
        Roles.PROVIDER -> V_INTERESTS
        Roles.ADMIN -> A_PANEL
        else -> null
    }
}

private val producerItems = listOf(
    NavItem(Routes.P_NEEDS, "Necesidades", Icons.Filled.Home),
    NavItem(Routes.P_PROFILE, "Perfil", Icons.Filled.Person)
)
private val providerItems = listOf(
    NavItem(Routes.V_INTERESTS, "Interesados", Icons.Filled.Notifications),
    NavItem(Routes.V_SERVICES, "Prestaciones", Icons.Filled.Build),
    NavItem(Routes.V_PROFILE, "Perfil", Icons.Filled.Person)
)
private val adminItems = listOf(
    NavItem(Routes.A_PANEL, "Panel", Icons.Filled.Home),
    NavItem(Routes.A_CATALOG, "Catálogo", Icons.Filled.Settings),
    NavItem(Routes.A_USERS, "Usuarios", Icons.Filled.Person)
)

@Composable
fun AppNavHost(
    vm: AuthViewModel = viewModel(),
    producerVm: ProducerViewModel = viewModel(),
    providerVm: ProviderViewModel = viewModel(),
    adminVm: AdminViewModel = viewModel()
) {
    val nav = rememberNavController()
    val user by vm.user.collectAsState()
    val ready by vm.ready.collectAsState()

    // Cambia de pestaña en la barra inferior sin apilar pantallas.
    fun tab(items: List<NavItem>): (String) -> Unit = { route ->
        nav.navigate(route) {
            popUpTo(items.first().route)
            launchSingleTop = true
        }
    }
    val producerTab = tab(producerItems)
    val providerTab = tab(providerItems)
    val adminTab = tab(adminItems)

    NavHost(
        navController = nav,
        startDestination = Routes.LOGIN,
        enterTransition = { slideInHorizontally(initialOffsetX = { it / 4 }) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 4 }) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it / 4 }) + fadeOut() }
    ) {
        // ---------- Autenticación ----------
        composable(Routes.LOGIN) {
            LoginScreen(vm, onRegister = { nav.navigate(Routes.REGISTER) }, onRecover = { nav.navigate(Routes.RECOVER) })
        }
        composable(Routes.REGISTER) {
            RegisterScreen(vm, onBack = { nav.popBackStack() }, onDone = { nav.popBackStack() })
        }
        composable(Routes.RECOVER) { RecoverScreen(vm, onBack = { nav.popBackStack() }) }

        // ---------- Productor ----------
        composable(Routes.P_NEEDS) {
            ProducerNeedsScreen(
                producerVm,
                bar = { AppBottomBar(producerItems, Routes.P_NEEDS, producerTab) },
                onNew = { nav.navigate(Routes.P_NEW) },
                onOpen = { nav.navigate(Routes.P_RESULTS) }
            )
        }
        composable(Routes.P_NEW) {
            NewNeedScreen(
                producerVm,
                bar = { AppBottomBar(producerItems, Routes.P_NEEDS, producerTab) },
                onBack = { nav.popBackStack() },
                onSaved = { nav.navigate(Routes.P_RESULTS) { popUpTo(Routes.P_NEEDS) } }
            )
        }
        composable(Routes.P_RESULTS) {
            ResultsScreen(
                producerVm,
                bar = { AppBottomBar(producerItems, Routes.P_NEEDS, producerTab) },
                onBack = { nav.popBackStack() },
                onCompare = { nav.navigate(Routes.P_COMPARE) }
            )
        }
        composable(Routes.P_COMPARE) {
            CompareScreen(
                producerVm,
                bar = { AppBottomBar(producerItems, Routes.P_NEEDS, producerTab) },
                onBack = { nav.popBackStack() },
                onDone = { nav.popBackStack(Routes.P_NEEDS, false) }
            )
        }
        composable(Routes.P_PROFILE) {
            ProducerProfileScreen(
                user,
                bar = { AppBottomBar(producerItems, Routes.P_PROFILE, producerTab) },
                onLogout = vm::logout
            )
        }

        // ---------- Prestador ----------
        composable(Routes.V_INTERESTS) {
            ProviderInterestsScreen(
                providerVm,
                bar = { AppBottomBar(providerItems, Routes.V_INTERESTS, providerTab) },
                onOpen = { nav.navigate(Routes.V_DETAIL) }
            )
        }
        composable(Routes.V_DETAIL) {
            InterestDetailScreen(
                providerVm,
                bar = { AppBottomBar(providerItems, Routes.V_INTERESTS, providerTab) },
                onBack = { nav.popBackStack() },
                onDone = { nav.popBackStack() }
            )
        }
        composable(Routes.V_SERVICES) {
            ProviderServicesScreen(
                providerVm,
                bar = { AppBottomBar(providerItems, Routes.V_SERVICES, providerTab) },
                onNew = { nav.navigate(Routes.V_FORM) },
                onEdit = { nav.navigate(Routes.V_FORM) }
            )
        }
        composable(Routes.V_FORM) {
            ProviderFormScreen(
                providerVm,
                bar = { AppBottomBar(providerItems, Routes.V_SERVICES, providerTab) },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.V_PROFILE) {
            ProviderProfileScreen(
                user,
                bar = { AppBottomBar(providerItems, Routes.V_PROFILE, providerTab) },
                onLogout = vm::logout
            )
        }

        // ---------- Administrador ----------
        composable(Routes.A_PANEL) {
            AdminPanelScreen(
                adminVm,
                bar = { AppBottomBar(adminItems, Routes.A_PANEL, adminTab) },
                onCatalog = { nav.navigate(Routes.A_CATALOG) },
                onLogout = vm::logout
            )
        }
        composable(Routes.A_CATALOG) {
            AdminCatalogScreen(
                adminVm,
                bar = { AppBottomBar(adminItems, Routes.A_CATALOG, adminTab) },
                onLogout = vm::logout
            )
        }
        composable(Routes.A_USERS) {
            AdminUsersScreen(
                adminVm,
                bar = { AppBottomBar(adminItems, Routes.A_USERS, adminTab) },
                onOpen = { nav.navigate(Routes.A_USER_DETAIL) },
                onLogout = vm::logout
            )
        }
        composable(Routes.A_USER_DETAIL) {
            AdminUserDetailScreen(
                adminVm,
                bar = { AppBottomBar(adminItems, Routes.A_USERS, adminTab) },
                onBack = { nav.popBackStack() },
                onLogout = vm::logout
            )
        }
    }

    // Cada ViewModel carga los datos del usuario que inició sesión.
    LaunchedEffect(user) {
        producerVm.setUser(user)
        providerVm.setUser(user)
        adminVm.setUser(user)
    }

    // Según la sesión guardada en Room, entra al home del perfil o vuelve al login.
    LaunchedEffect(user, ready) {
        if (!ready) return@LaunchedEffect
        val destination = Routes.homeFor(user?.role)
        val current = nav.currentDestination?.route
        if (destination != null && current in Routes.authRoutes) {
            nav.navigate(destination) { popUpTo(nav.graph.id) { inclusive = true } }
        } else if (destination == null && current !in Routes.authRoutes) {
            nav.navigate(Routes.LOGIN) { popUpTo(nav.graph.id) { inclusive = true } }
        }
    }
}
