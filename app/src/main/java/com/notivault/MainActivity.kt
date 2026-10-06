package com.notivault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.notivault.service.AppStatus
import com.notivault.ui.NotiVaultRoot
import com.notivault.ui.theme.NotiVaultTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotiVaultTheme {
                NotiVaultRoot()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check permissions every time you come back from the Settings app.
        AppStatus.refresh(this)
        if (AppStatus.state.value.listenerGranted) AppStatus.requestRebind(this)
    }
}
