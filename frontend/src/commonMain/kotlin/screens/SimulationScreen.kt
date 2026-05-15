package screens

import AccentCyan
import AccentGreen
import AccentAmber
import AccentRed
import BgElevated
import BgSurface
import BorderSubtle
import TextPrimary
import TextSecondary
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        val scope     = rememberCoroutineScope()

        var searchQuery       by remember { mutableStateOf("") }
        var selectedVariables by remember { mutableStateOf(setOf<String>()) }
        var stopTime          by remember { mutableStateOf("10.0") }
        var stepSize          by remember { mutableStateOf("0.01") }
        var startTime         by remember { mutableStateOf("0.0") }
        var errorMessage      by remember { mutableStateOf<String?>(null) }
        var isRunning         by remember { mutableStateOf(false) }

        val filteredVariables = remember(searchQuery, variables) {
            if (searchQuery.isBlank()) variables
            else variables.filter { it.contains(searchQuery, ignoreCase = true) }
        }

        // Derived validation
        val stopVal  = stopTime.toDoubleOrNull()
        val stepVal  = stepSize.toDoubleOrNull()
        val startVal = startTime.toDoubleOrNull()
        val paramsValid = stopVal != null && stopVal > 0.0 &&
            stepVal != null && stepVal > 0.0 && stepVal < stopVal &&
            startVal != null && startVal >= 0.0 && startVal < stopVal
        val estimatedPoints = if (paramsValid) ((stopVal - startVal) / stepVal).toInt() else 0

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {

                // ── Header ────────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("SIMULATION CONFIGURATOR", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = "${variables.size} available signals · select up to $MAX_SELECTED_VARIABLES",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    // Estimated points badge
                    if (paramsValid) {
                        Box(
                            modifier = Modifier
                                .background(AccentCyan.copy(0.1f))
                                .border(
                                    1.dp,
                                    AccentCyan.copy(0.3f),
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = "~$estimatedPoints STEPS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AccentCyan, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp,
                                ),
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, AccentCyan.copy(0.5f), Color.Transparent)
                            )
                        )
                )

                // ── Main two-column layout ────────────────────────────────────
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {

                    // LEFT — parameters panel
                    EngineeringCard(modifier = Modifier.width(280.dp).fillMaxHeight()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            SectionLabel("PARAMETERS")

                            EngNumberField(
                                label = "START TIME (s)",
                                value = startTime,
                                onValueChange = { startTime = it },
                                isValid = startVal != null && startVal >= 0.0,
                            )
                            EngNumberField(
                                label = "STOP TIME (s)",
                                value = stopTime,
                                onValueChange = { stopTime = it },
                                isValid = stopVal != null && stopVal > 0.0,
                            )
                            EngNumberField(
                                label = "STEP SIZE (s)",
                                value = stepSize,
                                onValueChange = { stepSize = it },
                                isValid = stepVal != null && stepVal > 0.0,
                            )

                            Spacer(Modifier.weight(1f))

                            // Summary widget
                            ParamSummary(
                                start = startTime,
                                stop = stopTime,
                                step = stepSize,
                                points = if (paramsValid) estimatedPoints else null,
                                valid = paramsValid,
                            )
                        }
                    }

                    // RIGHT — variable selector
                    EngineeringCard(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                SectionLabel("OUTPUT VARIABLES")
                                SelectionBadge(
                                    selected = selectedVariables.size,
                                    max = MAX_SELECTED_VARIABLES,
                                )
                            }

                            // Search field
                            EngSearchField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                            )

                            // Variable list
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(BgElevated),
                                verticalArrangement = Arrangement.spacedBy(1.dp),
                            ) {
                                items(filteredVariables) { variable ->
                                    val isSelected = variable in selectedVariables
                                    val canSelect = isSelected || selectedVariables.size < MAX_SELECTED_VARIABLES
                                    VariableRow(
                                        name = variable,
                                        isSelected = isSelected,
                                        enabled = canSelect,
                                        onToggle = {
                                            selectedVariables = if (isSelected) {
                                                selectedVariables - variable
                                            } else {
                                                selectedVariables + variable
                                            }
                                        },
                                    )
                                }
                            }

                            // Quick-select row
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                TextButton(
                                    onClick = {
                                        selectedVariables = filteredVariables
                                            .take(MAX_SELECTED_VARIABLES)
                                            .toSet()
                                    },
                                    contentPadding = PaddingValues(4.dp),
                                ) {
                                    Text(
                                        "SELECT FIRST $MAX_SELECTED_VARIABLES",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = AccentCyan.copy(0.7f), fontSize = 9.sp,
                                        ),
                                    )
                                }
                                TextButton(
                                    onClick = { selectedVariables = emptySet() },
                                    contentPadding = PaddingValues(4.dp),
                                ) {
                                    Text(
                                        "CLEAR ALL",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = AccentRed.copy(0.7f), fontSize = 9.sp,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Error ─────────────────────────────────────────────────────
                errorMessage?.let {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AccentRed.copy(0.1f))
                            .border(
                                1.dp,
                                AccentRed.copy(0.4f),
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("!", color = AccentRed)
                        Text(it, style = MaterialTheme.typography.bodySmall.copy(color = AccentRed))
                    }
                }

                // ── Action row ────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    EngButton(
                        text = "BACK",
                        onClick = { navigator.pop() },
                        modifier = Modifier.weight(1f),
                        isPrimary = false,
                        enabled = !isRunning,
                    )
                    EngButton(
                        text = if (isRunning) "SIMULATING…" else "RUN SIMULATION",
                        onClick = {
                            if (!paramsValid) {
                                errorMessage = "Invalid parameters — check stop time, step size and start time."
                                return@EngButton
                            }
                            if (selectedVariables.isEmpty()) {
                                errorMessage = "Select at least one output variable."
                                return@EngButton
                            }
                            errorMessage = null
                            isRunning = true
                            scope.launch {
                                try {
                                    val config = SimulationConfig(
                                        startTime = startVal,
                                        stopTime = stopVal,
                                        stepSize = stepVal,
                                        outputVariables = selectedVariables.toList(),
                                    )
                                    val result = runSimulation(config)
                                    navigator.push(ChartScreen(result))
                                } catch (e: Exception) {
                                    errorMessage = "Simulation error: ${e.message}"
                                } finally {
                                    isRunning = false
                                }
                            }
                        },
                        modifier = Modifier.weight(2f),
                        isPrimary = true,
                        enabled = !isRunning,
                        showLoader = isRunning,
                    )
                }
            }
        }
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun EngNumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isValid: Boolean,
) {
    val color = if (isValid) AccentCyan.copy(0.6f) else AccentRed.copy(0.7f)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, letterSpacing = 1.sp),
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = AccentCyan,
                unfocusedBorderColor = color,
                focusedTextColor     = TextPrimary,
                unfocusedTextColor   = TextPrimary,
                cursorColor          = AccentCyan,
                unfocusedContainerColor = BgElevated,
                focusedContainerColor   = BgElevated,
            ),
        )
    }
}

@Composable
private fun EngSearchField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                "FILTER VARIABLES…",
                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, letterSpacing = 1.sp),
            )
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = AccentCyan,
            unfocusedBorderColor    = BorderSubtle,
            focusedTextColor        = TextPrimary,
            unfocusedTextColor      = TextPrimary,
            cursorColor             = AccentCyan,
            unfocusedContainerColor = BgElevated,
            focusedContainerColor   = BgElevated,
        ),
    )
}

@Composable
private fun SelectionBadge(selected: Int, max: Int) {
    val color = when {
        selected == 0   -> TextSecondary
        selected >= max -> AccentAmber
        else            -> AccentGreen
    }
    Box(
        modifier = Modifier
            .background(color.copy(0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = "$selected / $max",
            style = MaterialTheme.typography.labelSmall.copy(color = color, fontWeight = FontWeight.Bold),
        )
    }
}

@Composable
private fun VariableRow(
    name: String,
    isSelected: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    val bg = if (isSelected) AccentCyan.copy(0.10f) else BgElevated
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable(enabled = enabled, onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Custom checkbox-like indicator
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(if (isSelected) AccentCyan else BgSurface)
                .border(
                    1.dp,
                    if (isSelected) AccentCyan else BorderSubtle,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Text("✓", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF0A0E14), fontSize = 9.sp))
            }
        }
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall.copy(
                color = if (enabled) TextPrimary else TextSecondary,
            ),
        )
    }
}

@Composable
private fun ParamSummary(
    start: String,
    stop: String,
    step: String,
    points: Int?,
    valid: Boolean,
) {
    val borderColor = if (valid) AccentCyan.copy(0.3f) else BorderSubtle
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor)
            .background(BgElevated)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "SUMMARY",
            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, letterSpacing = 1.2.sp),
        )
        SummaryLine("Interval", "[$start, $stop] s")
        SummaryLine("Step",     "$step s")
        SummaryLine("Points",   if (points != null) "~$points" else "—")
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
        Text(value, style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
    }
}
