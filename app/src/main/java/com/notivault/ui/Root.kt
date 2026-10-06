package com.notivault.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel

private sealed interface Screen {
    data object Home : Screen
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

    BackHandler(enabled = stack.size > 1, onBack = pop)

    when (val screen = stack.last()) {
        Screen.Home -> HomeScreen(
            vm = vm,
            onOpenDetail = { stack.add(Screen.Detail(it)) },
            onOpenApps = { stack.add(Screen.Apps) },
            onOpenSettings = { stack.add(Screen.Settings) },
        )
        Screen.Apps -> AppPickerScreen(onBack = pop)
        Screen.Settings -> SettingsScreen(vm = vm, onBack = pop)
        is Screen.Detail -> DetailScreen(vm = vm, id = screen.id, onBack = pop)
    }
}
