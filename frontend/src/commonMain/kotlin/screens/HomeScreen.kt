package screens

import AccentAmber
import AccentCyan
import AccentGreen
import AccentRed
import BgElevated
import BgSurface
import BorderSubtle
import TextPrimary
import TextSecondary
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import client.fetchInfo
import client.uploadFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

class HomeScreen : Screen {

    @Composable
    override fun Content() {
        val scope = rememberCoroutineScope()
        val navigator = LocalNavigator.currentOrThrow

        var info by remember { mutableStateOf<JsonObject?>(null) }
        var fileName by remember { mutableStateOf<String?>(null) }
        var isLoading by remember { mutableStateOf(false) }
        var errorMsg by remember { mutableStateOf<String?>(null) }
        var uploadOk by remember { mutableStateOf(false) }

        val launcher = rememberFilePickerLauncher(
            mode = FileKitMode.Single,
            type = FileKitType.File(),
        ) { file ->
            file?.let {
                fileName = file.name
                uploadOk = false
                errorMsg = null
                scope.launch {
                    try {
                        uploadFile(it)
                        uploadOk = true
                    } catch (e: Exception) {
                        errorMsg = "Upload failed: ${e.message}"
                    }
                }
            }
        }

        // ── Root scaffold ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // ── Top bar ───────────────────────────────────────────────────
                TopBar()

                // ── Status strip ──────────────────────────────────────────────
                StatusStrip(fileName = fileName, uploadOk = uploadOk, errorMsg = errorMsg)

                // ── Main content area (two columns) ───────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    // LEFT — FMU upload panel
                    UploadPanel(
                        modifier = Modifier.weight(1f),
                        fileName = fileName,
                        uploadOk = uploadOk,
                        onUploadClick = { launcher.launch() },
                    )

                    // RIGHT — Inspect / proceed panel
                    InspectPanel(
                        modifier = Modifier.weight(1f),
                        isLoading = isLoading,
                        canInspect = uploadOk || fileName != null,
                        onInspectClick = {
                            scope.launch {
                                isLoading = true
                                errorMsg = null
                                try {
                                    info = fetchInfo()
                                    info?.let { navigator.push(InfoScreen(it)) }
                                } catch (e: Exception) {
                                    errorMsg = "Failed to fetch FMU info: ${e.message}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                    )
                }

                // ── Bottom hint bar ───────────────────────────────────────────
                HintBar()
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TopBar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = "FMI WORKBENCH",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Functional Mock-up Interface · Simulation Tool",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        // Version badge
        Box(
            modifier = Modifier
                .background(BgElevated)
                .border(1.dp, BorderSubtle)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text = "v1.0 · FMI 2.0",
                style = MaterialTheme.typography.labelSmall.copy(color = AccentCyan),
            )
        }
    }

    // Horizontal divider with glow
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, AccentCyan.copy(alpha = 0.6f), Color.Transparent),
                ),
            ),
    )
}

@Composable
private fun StatusStrip(fileName: String?, uploadOk: Boolean, errorMsg: String?) {
    val (label, color) = when {
        errorMsg != null -> "ERROR: $errorMsg" to AccentRed
        uploadOk -> "FMU LOADED · ${fileName ?: "unknown"}" to AccentGreen
        fileName != null -> "UPLOADING · $fileName" to AccentAmber
        else -> "READY · No FMU loaded" to TextSecondary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgSurface)
            .border(1.dp, color.copy(alpha = 0.4f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Pulsing indicator dot
        val alpha by animateFloatAsState(if (uploadOk) 1f else 0.6f, animationSpec = tween(800))
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color.copy(alpha = alpha)),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = color,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
            ),
        )
    }
}

@Composable
private fun UploadPanel(modifier: Modifier, fileName: String?, uploadOk: Boolean, onUploadClick: () -> Unit) {
    val borderColor by animateColorAsState(
        if (uploadOk) AccentGreen else BorderSubtle,
        animationSpec = tween(400),
    )
    EngineeringCard(modifier = modifier, borderColor = borderColor) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionLabel("FMU SOURCE")

            // Drop-zone area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(BgElevated)
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(AccentCyan.copy(0.3f), BorderSubtle),
                        ),
                        shape = RectangleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = if (uploadOk) "LOADED" else "UPLOAD",
                        fontSize = 32.sp,
                        color = if (uploadOk) AccentGreen else AccentCyan.copy(0.6f),
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        text = fileName ?: "No file selected",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (uploadOk) AccentGreen else TextPrimary,
                            textAlign = TextAlign.Center,
                        ),
                    )
                    Text(
                        text = if (uploadOk) "FMU READY" else ".fmu · binary format",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (uploadOk) AccentGreen.copy(0.7f) else TextSecondary,
                        ),
                    )
                }
            }

            EngButton(
                text = "SELECT FMU FILE",
                onClick = onUploadClick,
                modifier = Modifier.fillMaxWidth(),
                isPrimary = true,
            )
        }
    }
}

@Composable
private fun InspectPanel(modifier: Modifier, isLoading: Boolean, canInspect: Boolean, onInspectClick: () -> Unit) {
    EngineeringCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionLabel("INSPECTION & SIMULATION")

            // Info rows
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                InfoEntry("Step 1", "Select and upload an .fmu file")
                InfoEntry("Step 2", "Inspect model metadata and variables")
                InfoEntry("Step 3", "Configure and run simulation")
                InfoEntry("Step 4", "Visualize results chart")
            }

            EngButton(
                text = if (isLoading) "LOADING…" else "INSPECT FMU",
                onClick = onInspectClick,
                modifier = Modifier.fillMaxWidth(),
                isPrimary = false,
                enabled = canInspect && !isLoading,
                showLoader = isLoading,
            )
        }
    }
}

@Composable
private fun HintBar() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(BorderSubtle),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("FMI Workbench · KMP/Compose", style = MaterialTheme.typography.labelSmall)
        Text("localhost:8080", style = MaterialTheme.typography.labelSmall)
    }
}

// ── Shared primitives ─────────────────────────────────────────────────────────

@Composable
fun EngineeringCard(
    modifier: Modifier = Modifier,
    borderColor: Color = BorderSubtle,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .background(BgSurface)
            .border(1.dp, borderColor),
    ) {
        content()
    }
}

@Composable
fun SectionLabel(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(modifier = Modifier.width(3.dp).height(14.dp).background(AccentCyan))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                color = AccentCyan,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

@Composable
fun EngButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = true,
    enabled: Boolean = true,
    showLoader: Boolean = false,
) {
    val bg = if (isPrimary) AccentCyan else BgElevated
    val fg = if (isPrimary) Color(0xFF0A0E14) else AccentCyan
    val border = if (isPrimary) AccentCyan else AccentCyan.copy(0.4f)

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = bg,
            contentColor = fg,
            disabledContainerColor = BgElevated,
            disabledContentColor = TextSecondary,
        ),
        border = BorderStroke(1.dp, border),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        shape = RectangleShape,
    ) {
        if (showLoader) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                color = AccentCyan,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (enabled) fg else TextSecondary,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
            ),
        )
    }
}

@Composable
private fun InfoEntry(step: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgElevated)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = step,
            style = MaterialTheme.typography.labelSmall.copy(color = AccentCyan),
        )
        Text(text = desc, style = MaterialTheme.typography.bodySmall)
    }
}
