@file:OptIn(ExperimentalMaterial3Api::class)

package com.notivault.ui

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notivault.data.NotificationEntity
import com.notivault.data.toJson
import org.json.JSONObject

@Composable
fun DetailScreen(vm: MainViewModel, id: Long, onBack: () -> Unit) {
    val flow = remember(id) { vm.observe(id) }
    val item by flow.collectAsStateWithLifecycle(initialValue = null)
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item?.appLabel.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        item?.let {
                            clipboard.setText(AnnotatedString(it.toJson().toString(2)))
                            Toast.makeText(context, "Copied as JSON", Toast.LENGTH_SHORT).show()
                        }
                    }) { Text("Copy JSON") }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                },
            )
        },
    ) { padding ->
        val n = item
        if (n == null) {
            Box(Modifier.padding(padding))
        } else {
            DetailBody(n, Modifier.padding(padding))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this notification?") },
            text = { Text("It will be removed from the database. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.delete(id)
                    onBack()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun DetailBody(n: NotificationEntity, modifier: Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (n.parserId != null) {
            Section("Transaction details") {
                if (n.amount != null) {
                    Text(
                        Fmt.money(n.amount, n.currency, n.direction),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = amountColor(n.direction),
                    )
                }
                DirectionField(n.direction)
                Field("Counterparty", n.counterparty)
                Field("Balance after", n.balance?.let { Fmt.number(it) })
                Field("Reference", n.reference)
                Field("Parser", n.parserId)
            }
        }

        Section("Full text (what parsers read)") {
            SelectionContainer {
                Text(n.fullText, style = MaterialTheme.typography.bodyLarge)
            }
        }

        TechnicalDetails(n)
    }
}

@Composable
private fun TechnicalDetails(n: NotificationEntity) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { expanded = !expanded }) {
        Icon(
            if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
        )
        Text(if (expanded) "Hide technical details" else "Show technical details")
    }
    if (!expanded) return

    Section("Fields") {
        Field("Title", n.title)
        Field("Text", n.text)
        Field("Big text", n.bigText)
        Field("Sub text", n.subText)
        Field("Summary", n.summaryText)
        Field("Info", n.infoText)
        Field("Inbox lines", n.textLines)
        Field("Messages", n.messages)
        Field("Ticker", n.tickerText)
        Field("Package", n.packageName)
        Field("Channel", n.channelId)
        Field("Category", n.category)
        Field("Posted", Fmt.long(n.postedAt))
        Field("Stored", Fmt.long(n.receivedAt))
        Field("Notification key", n.notificationKey)
        Field("Row id", n.id.toString())
    }

    Section("Raw extras (JSON)") {
        val pretty = remember(n.rawExtras) {
            runCatching { JSONObject(n.rawExtras).toString(2) }.getOrDefault(n.rawExtras)
        }
        SelectionContainer {
            Text(
                pretty,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            )
        }
    }
}

@Composable
private fun DirectionField(direction: String?) {
    val (label, rotation, color) = when (direction) {
        "IN" -> Triple("Money in", 90f, amountColor(direction))
        "OUT" -> Triple("Money out", -90f, amountColor(direction))
        else -> return
    }
    // ArrowBack is auto-mirrored, so in RTL it starts pointing right; flip the rotation to keep up/down.
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Column {
        Text(
            "Direction",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                modifier = Modifier.graphicsLayer { rotationZ = if (rtl) -rotation else rotation },
                tint = color,
            )
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun Field(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SelectionContainer { Text(value, style = MaterialTheme.typography.bodyMedium) }
    }
}
