package com.notivault.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notivault.data.NotificationEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.core.chart.column.ColumnChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.entryOf
import androidx.compose.ui.graphics.Color

enum class ViewBy { DAYS, MONTHS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    vm: MainViewModel,
    onOpenDrawer: () -> Unit
) {
    val balances by vm.withAmount.collectAsStateWithLifecycle()
    var viewBy by remember { mutableStateOf(ViewBy.DAYS) }

    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                OutlinedButton(onClick = { expanded = true }) {
                    Text(if (viewBy == ViewBy.DAYS) "Days" else "Months")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Days") },
                        onClick = {
                            viewBy = ViewBy.DAYS
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Months") },
                        onClick = {
                            viewBy = ViewBy.MONTHS
                            expanded = false
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            if (balances.isEmpty()) {
                Text("No data available with amounts.", modifier = Modifier.padding(16.dp))
            } else {
                BalanceChart(balances, viewBy, modifier = Modifier.height(250.dp).fillMaxWidth().padding(horizontal = 20.dp))
            }
        }
    }
}

@Composable
fun BalanceChart(balances: List<NotificationEntity>, viewBy: ViewBy, modifier: Modifier = Modifier) {
    val zone = ZoneId.systemDefault()
    val formatter = if (viewBy == ViewBy.DAYS) DateTimeFormatter.ofPattern("MMM dd") else DateTimeFormatter.ofPattern("MMM yyyy")
    
    val grouped = balances.groupBy {
        val dt = Instant.ofEpochMilli(it.postedAt).atZone(zone)
        if (viewBy == ViewBy.DAYS) dt.toLocalDate() else dt.withDayOfMonth(1).toLocalDate()
    }
    
    val chartData = grouped.entries.sortedBy { it.key }.map { entry ->
        val gains = entry.value.filter { it.direction == "IN" }.sumOf { it.amount ?: 0.0 }
        val losses = entry.value.filter { it.direction == "OUT" }.sumOf { it.amount ?: 0.0 }
        entry.key.format(formatter) to (gains to -losses)
    }
    
    if (chartData.isEmpty()) {
        Text("Not enough data to draw a chart.", modifier = modifier)
        return
    }

    val gainsEntries = chartData.mapIndexed { index, data -> entryOf(index.toFloat(), data.second.first.toFloat()) }
    val lossesEntries = chartData.mapIndexed { index, data -> entryOf(index.toFloat(), data.second.second.toFloat()) }
    val model = entryModelOf(gainsEntries, lossesEntries)

    Chart(
        modifier = modifier,
        chart = columnChart(
            columns = listOf(
                lineComponent(color = Color(0xFF4CAF50), thickness = 10.dp), // Green for IN
                lineComponent(color = Color(0xFFF44336), thickness = 10.dp)  // Red for OUT (negative)
            ),
            mergeMode = ColumnChart.MergeMode.Stack
        ),
        model = model,
        startAxis = rememberStartAxis(
            valueFormatter = { value, _ -> 
                if (value >= 1_000_000f || value <= -1_000_000f) {
                    String.format(java.util.Locale.getDefault(), "%.1fM", value / 1_000_000f)
                } else if (value >= 1_000f || value <= -1_000f) {
                    String.format(java.util.Locale.getDefault(), "%.1fk", value / 1_000f)
                } else {
                    String.format(java.util.Locale.getDefault(), "%.0f", value)
                }
            }
        ),
        bottomAxis = rememberBottomAxis(
            valueFormatter = { value, _ -> 
                chartData.getOrNull(value.toInt())?.first ?: ""
            }
        )
    )
}
