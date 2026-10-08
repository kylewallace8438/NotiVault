package com.notivault.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

private sealed interface Screen {
    data object Home : Screen
    data object Report : Screen
    data object Apps : Screen
    data object Settings : Screen
    data class Detail(val id: Long) : Screen
}

/** Tiny back-stack navigation; enough for four screens without an extra library. */
@Composable
fun NotiVaultRoot() {
    val vm: MainViewModel = viewModel()
    val stack = remember { mutableStateListOf<Screen>(Screen.Home) }
    val pop: () -> Unit = { if (stack.size > 1) stack.removeAt(stack.lastIndex) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BackHandler(enabled = drawerState.isOpen || stack.size > 1, onBack = {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            pop()
        }
    })
    
    val currentScreen = stack.lastOrNull() ?: Screen.Home

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(windowInsets = WindowInsets.systemBars) {
                Text("NotiVault", modifier = Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 16.dp), style = MaterialTheme.typography.titleLarge)
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))
                NavigationDrawerItem(
                    label = { Text("Overview") },
                    selected = currentScreen is Screen.Home,
                    onClick = {
                        stack.clear()
                        stack.add(Screen.Home)
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Report") },
                    selected = currentScreen is Screen.Report,
                    onClick = {
                        stack.clear()
                        stack.add(Screen.Report)
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = currentScreen is Screen.Settings,
                    onClick = {
                        stack.clear()
                        stack.add(Screen.Settings)
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        when (val screen = currentScreen) {
            Screen.Home -> HomeScreen(
                vm = vm,
                onOpenDetail = { stack.add(Screen.Detail(it)) },
                onOpenApps = { stack.add(Screen.Apps) },
                onOpenDrawer = { scope.launch { drawerState.open() } }
            )
            Screen.Report -> ReportScreen(
                vm = vm,
                onOpenDrawer = { scope.launch { drawerState.open() } }
            )
            Screen.Apps -> AppPickerScreen(onBack = pop)
            Screen.Settings -> SettingsScreen(
                vm = vm,
                onOpenDrawer = { scope.launch { drawerState.open() } }
            )
            is Screen.Detail -> DetailScreen(vm = vm, id = screen.id, onBack = pop)
        }
    }
}
