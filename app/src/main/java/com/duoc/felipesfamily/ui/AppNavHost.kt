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
    const val PRODUCER_NEEDS = "producer_needs"
    const val PRODUCER_NEW_NEED = "producer_new"
    const val PRODUCER_MATCH_RESULTS = "producer_results"
    const val PRODUCER_PROVIDER_COMPARISON = "producer_compare"
    const val PRODUCER_PROFILE = "producer_profile"
    const val PROVIDER_INTERESTS = "provider_interests"
    const val PROVIDER_INTEREST_DETAIL = "provider_detail"
    const val PROVIDER_SERVICES = "provider_services"
    const val PROVIDER_SERVICE_FORM = "provider_form"
    const val PROVIDER_PROFILE = "provider_profile"
    const val ADMIN_PANEL = "admin_panel"
    const val ADMIN_SERVICE_CATALOG = "admin_catalog"
    const val ADMIN_USERS = "admin_users"
    const val ADMIN_USER_DETAIL = "admin_user_detail"

    val authRoutes = listOf(LOGIN, REGISTER, RECOVER)

    fun homeFor(role: String?): String? = when (role) {
        Roles.PRODUCER -> PRODUCER_NEEDS
        Roles.PROVIDER -> PROVIDER_INTERESTS
        Roles.ADMIN -> ADMIN_PANEL
        else -> null
    }
}

private val producerItems = listOf(
    NavItem(Routes.PRODUCER_NEEDS, "Necesidades", Icons.Filled.Home),
    NavItem(Routes.PRODUCER_PROFILE, "Perfil", Icons.Filled.Person)
)
private val providerItems = listOf(
    NavItem(Routes.PROVIDER_INTERESTS, "Interesados", Icons.Filled.Notifications),
    NavItem(Routes.PROVIDER_SERVICES, "Prestaciones", Icons.Filled.Build),
    NavItem(Routes.PROVIDER_PROFILE, "Perfil", Icons.Filled.Person)
)
private val adminItems = listOf(
    NavItem(Routes.ADMIN_PANEL, "Panel", Icons.Filled.Home),
    NavItem(Routes.ADMIN_SERVICE_CATALOG, "Catálogo", Icons.Filled.Settings),
    NavItem(Routes.ADMIN_USERS, "Usuarios", Icons.Filled.Person)
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
        composable(Routes.PRODUCER_NEEDS) {
            ProducerNeedsScreen(
                producerVm,
                bar = { AppBottomBar(producerItems, Routes.PRODUCER_NEEDS, producerTab) },
                onNew = { nav.navigate(Routes.PRODUCER_NEW_NEED) },
                onOpen = { nav.navigate(Routes.PRODUCER_MATCH_RESULTS) }
            )
        }
        composable(Routes.PRODUCER_NEW_NEED) {
            NewNeedScreen(
                producerVm,
                bar = { AppBottomBar(producerItems, Routes.PRODUCER_NEEDS, producerTab) },
                onBack = { nav.popBackStack() },
                onSaved = { nav.navigate(Routes.PRODUCER_MATCH_RESULTS) { popUpTo(Routes.PRODUCER_NEEDS) } }
            )
        }
        composable(Routes.PRODUCER_MATCH_RESULTS) {
            ResultsScreen(
                producerVm,
                bar = { AppBottomBar(producerItems, Routes.PRODUCER_NEEDS, producerTab) },
                onBack = { nav.popBackStack() },
                onCompare = { nav.navigate(Routes.PRODUCER_PROVIDER_COMPARISON) }
            )
        }
        composable(Routes.PRODUCER_PROVIDER_COMPARISON) {
            CompareScreen(
                producerVm,
                bar = { AppBottomBar(producerItems, Routes.PRODUCER_NEEDS, producerTab) },
                onBack = { nav.popBackStack() },
                onDone = { nav.popBackStack(Routes.PRODUCER_NEEDS, false) }
            )
        }
        composable(Routes.PRODUCER_PROFILE) {
            ProducerProfileScreen(
                user,
                bar = { AppBottomBar(producerItems, Routes.PRODUCER_PROFILE, producerTab) },
                onLogout = vm::logout
            )
        }

        // ---------- Prestador ----------
        composable(Routes.PROVIDER_INTERESTS) {
            ProviderInterestsScreen(
                providerVm,
                bar = { AppBottomBar(providerItems, Routes.PROVIDER_INTERESTS, providerTab) },
                onOpen = { nav.navigate(Routes.PROVIDER_INTEREST_DETAIL) }
            )
        }
        composable(Routes.PROVIDER_INTEREST_DETAIL) {
            InterestDetailScreen(
                providerVm,
                bar = { AppBottomBar(providerItems, Routes.PROVIDER_INTERESTS, providerTab) },
                onBack = { nav.popBackStack() },
                onDone = { nav.popBackStack() }
            )
        }
        composable(Routes.PROVIDER_SERVICES) {
            ProviderServicesScreen(
                providerVm,
                bar = { AppBottomBar(providerItems, Routes.PROVIDER_SERVICES, providerTab) },
                onNew = { nav.navigate(Routes.PROVIDER_SERVICE_FORM) },
                onEdit = { nav.navigate(Routes.PROVIDER_SERVICE_FORM) }
            )
        }
        composable(Routes.PROVIDER_SERVICE_FORM) {
            ProviderFormScreen(
                providerVm,
                bar = { AppBottomBar(providerItems, Routes.PROVIDER_SERVICES, providerTab) },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.PROVIDER_PROFILE) {
            ProviderProfileScreen(
                user,
                bar = { AppBottomBar(providerItems, Routes.PROVIDER_PROFILE, providerTab) },
                onLogout = vm::logout
            )
        }

        // ---------- Administrador ----------
        composable(Routes.ADMIN_PANEL) {
            AdminPanelScreen(
                adminVm,
                bar = { AppBottomBar(adminItems, Routes.ADMIN_PANEL, adminTab) },
                onCatalog = { nav.navigate(Routes.ADMIN_SERVICE_CATALOG) },
                onLogout = vm::logout
            )
        }
        composable(Routes.ADMIN_SERVICE_CATALOG) {
            AdminCatalogScreen(
                adminVm,
                bar = { AppBottomBar(adminItems, Routes.ADMIN_SERVICE_CATALOG, adminTab) },
                onLogout = vm::logout
            )
        }
        composable(Routes.ADMIN_USERS) {
            AdminUsersScreen(
                adminVm,
                bar = { AppBottomBar(adminItems, Routes.ADMIN_USERS, adminTab) },
                onOpen = { nav.navigate(Routes.ADMIN_USER_DETAIL) },
                onLogout = vm::logout
            )
        }
        composable(Routes.ADMIN_USER_DETAIL) {
            AdminUserDetailScreen(
                adminVm,
                bar = { AppBottomBar(adminItems, Routes.ADMIN_USERS, adminTab) },
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
