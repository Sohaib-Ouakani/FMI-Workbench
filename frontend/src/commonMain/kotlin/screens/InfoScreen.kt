package screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.example.fmi_client.screens.SimulationScreen
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.boolean

class InfoScreen(val info: JsonObject) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val canSimulate = info["canSimulate"]?.jsonPrimitive?.boolean ?: false
        val variables = (info["variables"] as? JsonArray)
            ?.map { it.jsonPrimitive.content }
            ?: emptyList()


        Box(modifier = Modifier.fillMaxSize()) {

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LazyColumn(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .weight(1f)
                ) {
                    items(info.entries.toList()) { (key, value) ->
                        when (value) {
                            is JsonPrimitive -> InfoRow(key, value.content.ifBlank { "----" })
                            is JsonArray -> InfoRow(
                                key,
                                value.joinToString(",") { it.jsonPrimitive.content }
                            )
                            else -> {}
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (canSimulate) {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                navigator.push(SimulationScreen(variables))
                            },
                        ) {
                            Text("Simulate")
                        }
                    } else {
                        Text(
                            text = "⚠ Simulation not available: this FMU is Model Exchange only.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = { navigator.pop() }
                ) {
                    Text("Go back")
                    }
                }
            }
        }
    }
    @Composable
    fun InfoRow(label: String, content: String) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = content,
                modifier = Modifier.weight(2f)
            )
        }
    }
}
