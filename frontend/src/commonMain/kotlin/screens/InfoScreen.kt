package screens

import AccentCyan
import AccentGreen
import AccentRed
import BgElevated
import TextPrimary
import TextSecondary
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.serialization.json.*

class InfoScreen(val info: JsonObject) : Screen {

    @Composable
    override fun Content() {
        val navigator   = LocalNavigator.currentOrThrow
        val canSimulate = info["canSimulate"]?.jsonPrimitive?.boolean ?: false
        val variables: Map<String, String> = (info["variables"] as? JsonObject)
            ?.mapValues { it.value.jsonPrimitive.content } ?: emptyMap()

        // Separate metadata fields from the variables/canSimulate keys
        val metaEntries = info.entries
            .filter { it.key != "variables" && it.key != "canSimulate" }

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
                        Text("MODEL METADATA", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = "FMU introspection report",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    // Simulate capability badge
                    SimCapabilityBadge(canSimulate)
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

                // ── Two-column body ───────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // LEFT — model metadata table
                    EngineeringCard(modifier = Modifier.weight(1.2f).fillMaxHeight()) {
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            SectionLabel("PROPERTIES")
                            Spacer(Modifier.height(12.dp))
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                items(metaEntries.toList()) { (key, value) ->
                                    when (value) {
                                        is JsonPrimitive -> MetaRow(key, value.content.ifBlank { "—" })
                                        is JsonArray     -> MetaRow(
                                            key,
                                            value.joinToString(", ") { it.jsonPrimitive.content }
                                        )
                                        else -> {}
                                    }
                                }
                            }
                        }
                    }

                    // RIGHT — variable list
                    EngineeringCard(modifier = Modifier.weight(0.8f).fillMaxHeight()) {
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            SectionLabel("VARIABLES")
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${variables.size} output signals",
                                style = MaterialTheme.typography.labelSmall,
                            )
                            Spacer(Modifier.height(8.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                items(variables.keys.toList()) { name ->
                                    VariableChip(name, variables[name].orEmpty())
                                }
                            }
                        }
                    }
                }

                // ── Action buttons ────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    EngButton(
                        text = "BACK",
                        onClick = { navigator.pop() },
                        modifier = Modifier.weight(1f),
                        isPrimary = false,
                    )
                    if (canSimulate) {
                        EngButton(
                            text = "CONFIGURE SIMULATION",
                            onClick = { navigator.push(SimulationScreen(variables)) },
                            modifier = Modifier.weight(2f),
                            isPrimary = true,
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(2f)
                                .background(BgElevated)
                                .border(1.dp, AccentRed.copy(0.4f))
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "! MODEL EXCHANGE ONLY — SIMULATION UNAVAILABLE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AccentRed,
                                    letterSpacing = 1.sp,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun MetaRow(label: String, content: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgElevated)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = Modifier.weight(0.9f),
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary),
                modifier = Modifier.weight(1.1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.height(1.dp))
    }

    @Composable
    private fun VariableChip(name: String, mesure: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgElevated)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(modifier = Modifier.size(5.dp).background(AccentCyan.copy(0.5f)))
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "[$mesure]",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )        }
        Spacer(modifier = Modifier.height(1.dp))
    }
}

@Composable
private fun SimCapabilityBadge(canSimulate: Boolean) {
    val color  = if (canSimulate) AccentGreen else AccentRed
    val label  = if (canSimulate) "CO-SIM CAPABLE" else "MODEL EXCHANGE"
    Box(
        modifier = Modifier
            .background(color.copy(0.12f))
            .border(1.dp, color.copy(0.5f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = color,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
            ),
        )
    }
}
