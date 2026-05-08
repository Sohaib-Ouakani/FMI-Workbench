package com.example.fmi_client.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.example.fmi_client.client.SimulationConfig
import com.example.fmi_client.client.runSimulation
import kotlinx.coroutines.launch

private const val MAX_SELECTED_VARIABLES = 6

class SimulationScreen(private val variables: List<String>) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()

        var searchQuery by remember { mutableStateOf("") }
        var selectedVariables by remember { mutableStateOf(setOf<String>()) }
        var stopTime by remember { mutableStateOf("10.0") }
        var stepSize by remember { mutableStateOf("0.01") }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var isRunning by remember { mutableStateOf(false) }

        val filteredVariables = remember(searchQuery, variables) {
            if (searchQuery.isBlank()) variables
            else variables.filter { it.contains(searchQuery, ignoreCase = true) }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Configure Simulation", style = MaterialTheme.typography.headlineSmall)

            // ── Simulation parameters ─────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = stopTime,
                    onValueChange = { stopTime = it },
                    label = { Text("Stop time (s)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = stepSize,
                    onValueChange = { stepSize = it },
                    label = { Text("Step size (s)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }

            // ── Variable search ───────────────────────────────────────────────
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search variables…") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Text(
                text = "Select up to $MAX_SELECTED_VARIABLES variables " +
                    "(${selectedVariables.size}/$MAX_SELECTED_VARIABLES selected)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // ── Variable list ─────────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small),
            ) {
                items(filteredVariables) { variable ->
                    val isSelected = variable in selectedVariables
                    val canSelect = isSelected || selectedVariables.size < MAX_SELECTED_VARIABLES
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = canSelect) {
                                selectedVariables = if (isSelected) {
                                    selectedVariables - variable
                                } else {
                                    selectedVariables + variable
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = null, // handled by row click
                            enabled = canSelect,
                        )
                        Text(
                            text = variable,
                            modifier = Modifier.padding(start = 8.dp),
                            color = if (canSelect) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // ── Error message ─────────────────────────────────────────────────
            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            // ── Action buttons ────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { navigator.pop() },
                    enabled = !isRunning,
                ) {
                    Text("Back")
                }

                Button(
                    modifier = Modifier.weight(1f),
                    enabled = selectedVariables.isNotEmpty() && !isRunning,
                    onClick = {
                        val stop = stopTime.toDoubleOrNull()
                        val step = stepSize.toDoubleOrNull()
                        if (stop == null || stop <= 0.0) {
                            errorMessage = "Stop time must be a positive number."
                            return@Button
                        }
                        if (step == null || step <= 0.0 || step >= stop) {
                            errorMessage = "Step size must be positive and less than stop time."
                            return@Button
                        }
                        errorMessage = null
                        isRunning = true
                        scope.launch {
                            try {
                                val config = SimulationConfig(
                                    stopTime = stop,
                                    stepSize = step,
                                    outputVariables = selectedVariables.toList(),
                                )
                                val result = runSimulation(config)
                                navigator.push(ChartScreen(result))
                            } catch (e: Exception) {
                                errorMessage = "Error: ${e.message}"
                            } finally {
                                isRunning = false
                            }
                        }
                    },
                ) {
                    Text(if (isRunning) "Running…" else "Run Simulation")
                }
            }
        }
    }
}
