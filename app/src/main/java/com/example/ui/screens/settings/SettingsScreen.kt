package com.example.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.components.StatusBadge
import com.example.ui.screens.apihub.ApiHubScreen
import com.example.ui.screens.history.HistoryScreen
import com.example.ui.theme.GeminiBackground
import com.example.ui.theme.GeminiBlue
import com.example.ui.theme.GeminiGreen
import com.example.ui.theme.GeminiOutline
import com.example.ui.theme.GeminiPurple
import com.example.ui.theme.GeminiRed
import com.example.ui.theme.GeminiSparkleGradient
import com.example.ui.theme.GeminiSurface
import com.example.ui.theme.GeminiSurfaceElevated
import com.example.ui.theme.GeminiSurfaceVariant
import com.example.ui.theme.GeminiTextMuted
import com.example.ui.theme.GeminiTextPrimary
import com.example.ui.theme.GeminiTextSecondary

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var showApiHubSubscreen by remember { mutableStateOf(false) }
    var showHistorySubscreen by remember { mutableStateOf(false) }

    val activeConfig by viewModel.selectedModelConfig.collectAsState()
    val enabledConfigs by viewModel.enabledConfigs.collectAsState()
    val isSpeechCleanerOn by viewModel.isSpeechCleanerEnabled.collectAsState()
    val isMemoryOn by viewModel.isMemoryEnabled.collectAsState()
    val memories by viewModel.memories.collectAsState()
    val testingId by viewModel.testingConfigId.collectAsState()
    val testResult by viewModel.testResult.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val allDiscoveredModels by viewModel.allDiscoveredModels.collectAsState()
    val isDiscoveringModels by viewModel.isDiscoveringModels.collectAsState()

    var showAddMemoryDialog by remember { mutableStateOf(false) }
    var newMemoryFact by remember { mutableStateOf("") }
    var newMemoryCategory by remember { mutableStateOf("Preference") }

    if (showHistorySubscreen) {
        HistoryScreen(
            viewModel = viewModel,
            modifier = modifier.statusBarsPadding().navigationBarsPadding(),
            onBack = { showHistorySubscreen = false }
        )
        return
    }

    if (showApiHubSubscreen) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(GeminiBackground)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GeminiSurface)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showApiHubSubscreen = false }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Settings",
                        tint = GeminiTextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Central API Hub", color = GeminiTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            ApiHubScreen(viewModel = viewModel)
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GeminiBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // TOP APP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(38.dp).testTag("btn_settings_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Chat",
                    tint = GeminiTextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text("Settings", color = GeminiTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Nova AI by Rauf", color = GeminiTextMuted, fontSize = 11.sp)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. API & PROVIDER CONFIGURATION
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GeminiSurface)
                        .border(1.dp, GeminiBlue.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .clickable { showApiHubSubscreen = true }
                        .padding(16.dp)
                        .testTag("nav_to_api_hub"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(GeminiBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Hub, contentDescription = null, tint = GeminiBlue, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Central API Hub", color = GeminiTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                text = "Active: ${activeConfig?.name ?: "None"} (${activeConfig?.modelName?.ifBlank { "Auto-discovering" } ?: "No model"})",
                                color = GeminiTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = GeminiTextMuted, modifier = Modifier.size(16.dp))
                }
            }

            // 2. MODEL MANAGEMENT
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GeminiSurface)
                        .border(1.dp, GeminiOutline, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                        .testTag("model_management_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GeminiBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Model Management", color = GeminiTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        if (activeConfig != null) {
                            StatusBadge(status = activeConfig!!.status, latencyMs = activeConfig!!.lastLatencyMs)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    DiagnosticRow("Active Model", activeConfig?.modelName?.ifBlank { "Auto-discovering..." } ?: "None")
                    DiagnosticRow("Active Provider", activeConfig?.name ?: "None configured")
                    DiagnosticRow("Endpoint URL", activeConfig?.baseUrl ?: "None")
                    DiagnosticRow("Masked Key", activeConfig?.maskedApiKey ?: "Not set")

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { activeConfig?.let { viewModel.testConfig(it) } },
                            enabled = activeConfig != null && testingId == null,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (testingId != null) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = GeminiBlue)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Verifying...", fontSize = 11.sp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Verify Active Model", fontSize = 11.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = { activeConfig?.let { viewModel.refreshModelsForConfig(it) } },
                            enabled = !isDiscoveringModels && activeConfig != null,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isDiscoveringModels) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = GeminiBlue)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Scanning...", fontSize = 11.sp)
                            } else {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Refresh Models", fontSize = 11.sp)
                            }
                        }
                    }

                    testResult?.let { res ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (res.success) GeminiGreen.copy(alpha = 0.15f) else GeminiRed.copy(alpha = 0.15f))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = res.message,
                                color = if (res.success) GeminiGreen else GeminiRed,
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (enabledConfigs.size > 1) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = GeminiOutline)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Switch Active Provider / Model:", color = GeminiTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))

                        enabledConfigs.forEach { cfg ->
                            val isSelected = cfg.id == activeConfig?.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) GeminiBlue.copy(alpha = 0.12f) else GeminiSurfaceVariant)
                                    .border(1.dp, if (isSelected) GeminiBlue else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setSelectedConfig(cfg) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(cfg.name, color = GeminiTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(cfg.modelName.ifBlank { "Auto-discovering" }, color = GeminiTextSecondary, fontSize = 11.sp)
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = "Active", tint = GeminiBlue, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // 3. VOICE SETTINGS
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GeminiSurface)
                        .border(1.dp, GeminiOutline, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                        .testTag("voice_settings_card")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = GeminiBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Voice & Audio Settings", color = GeminiTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Live Voice Launch Button
                    ElevatedButton(
                        onClick = {
                            viewModel.setLiveVoiceActive(true)
                            onBack()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = GeminiBlue,
                            contentColor = Color(0xFF041E49)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Nova Live Voice Session", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = GeminiOutline)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Speech Cleaning Pipeline", color = GeminiTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Strips emojis, markdown headers, URLs, code blocks, and citations prior to Text-to-Speech synthesis.",
                                color = GeminiTextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                        Switch(
                            checked = isSpeechCleanerOn,
                            onCheckedChange = { viewModel.toggleSpeechCleaner(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = GeminiBlue,
                                uncheckedThumbColor = GeminiTextMuted,
                                uncheckedTrackColor = GeminiSurfaceVariant
                            )
                        )
                    }
                }
            }

            // 4. HISTORY / RECENT CHATS
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GeminiSurface)
                        .border(1.dp, GeminiOutline, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                        .testTag("history_settings_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, tint = GeminiPurple, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Recent Chats & History", color = GeminiTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("${sessions.size} saved", color = GeminiTextSecondary, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Open Full History Subscreen
                    OutlinedButton(
                        onClick = { showHistorySubscreen = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Open Full Chat History (${sessions.size})", fontSize = 12.sp)
                    }

                    if (sessions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Recent Conversations:", color = GeminiTextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        sessions.take(3).forEach { session ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GeminiSurfaceVariant)
                                    .clickable {
                                        viewModel.selectSession(session.id)
                                        onBack()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = session.title,
                                    color = GeminiTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open →", color = GeminiBlue, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // 5. ASSISTANT MEMORY
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GeminiSurface)
                        .border(1.dp, GeminiOutline, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = GeminiPurple, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Nova Assistant Memory", color = GeminiTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        Switch(
                            checked = isMemoryOn,
                            onCheckedChange = { viewModel.toggleMemory(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = GeminiPurple,
                                uncheckedThumbColor = GeminiTextMuted,
                                uncheckedTrackColor = GeminiSurfaceVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "When active, Nova remembers key facts and preferences across conversations to personalize responses.",
                        color = GeminiTextSecondary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    memories.forEach { mem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(GeminiSurfaceVariant)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "[${mem.category}] ${mem.fact}",
                                color = GeminiTextPrimary,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.deleteMemory(mem.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Memory", tint = GeminiRed, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = { showAddMemoryDialog = true },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Memory Fact", fontSize = 11.sp)
                        }

                        if (memories.isNotEmpty()) {
                            TextButton(onClick = { viewModel.clearAllMemories() }) {
                                Text("Clear All", color = GeminiRed, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // 6. ABOUT NOVA
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GeminiSurface)
                        .border(1.dp, GeminiOutline, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                        .testTag("about_card")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GeminiBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("About Nova", color = GeminiTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Nova AI by Rauf",
                        color = GeminiTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DiagnosticRow("Platform", "Nova")
                    DiagnosticRow("Developer", "Rauf")
                    DiagnosticRow("Creator", "Rauf")
                    DiagnosticRow("Core Purpose", "AI Text Chat & Real-Time Voice")
                    DiagnosticRow("Architecture", "Central API Hub Multi-Model Engine")
                    DiagnosticRow("Version", "1.0.0 Production Native")
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // Add Memory Dialog
    if (showAddMemoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddMemoryDialog = false },
            title = { Text("Add Memory Fact", color = GeminiTextPrimary, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newMemoryFact,
                        onValueChange = { newMemoryFact = it },
                        placeholder = { Text("e.g., Prefers Kotlin over Java", color = GeminiTextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = GeminiTextPrimary,
                            unfocusedTextColor = GeminiTextPrimary,
                            focusedBorderColor = GeminiBlue
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newMemoryFact.isNotBlank()) {
                            viewModel.addMemory(newMemoryFact.trim(), newMemoryCategory)
                            newMemoryFact = ""
                        }
                        showAddMemoryDialog = false
                    }
                ) {
                    Text("Save Fact", color = GeminiBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMemoryDialog = false }) {
                    Text("Cancel", color = GeminiTextSecondary)
                }
            },
            containerColor = GeminiSurface
        )
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = GeminiTextSecondary, fontSize = 12.sp)
        Text(value, color = GeminiTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
