package com.example.ui.screens.apihub

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.ApiConfigEntity
import com.example.data.local.entity.DiscoveredModelEntity
import com.example.data.network.providers.ProviderRegistry
import com.example.data.network.providers.ProviderTemplate
import com.example.ui.MainViewModel
import com.example.ui.components.StatusBadge
import com.example.ui.theme.GeminiBackground
import com.example.ui.theme.GeminiBlue
import com.example.ui.theme.GeminiGreen
import com.example.ui.theme.GeminiOutline
import com.example.ui.theme.GeminiPink
import com.example.ui.theme.GeminiPurple
import com.example.ui.theme.GeminiRed
import com.example.ui.theme.GeminiSparkleGradient
import com.example.ui.theme.GeminiSurface
import com.example.ui.theme.GeminiSurfaceElevated
import com.example.ui.theme.GeminiSurfaceVariant
import com.example.ui.theme.GeminiTextMuted
import com.example.ui.theme.GeminiTextPrimary
import com.example.ui.theme.GeminiTextSecondary
import java.util.UUID

@Composable
fun ApiHubScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allConfigs by viewModel.allConfigs.collectAsState()
    val testingId by viewModel.testingConfigId.collectAsState()
    val allDiscoveredModels by viewModel.allDiscoveredModels.collectAsState()
    val isDiscoveringModels by viewModel.isDiscoveringModels.collectAsState()

    var editingConfig by remember { mutableStateOf<ApiConfigEntity?>(null) }
    var isAddingNew by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GeminiBackground)
            .testTag("api_hub_screen")
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(GeminiSparkleGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Hub,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Central API Hub",
                        color = GeminiTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Dynamic discovery & multi-provider LLMs",
                        color = GeminiTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            // Add Provider Button
            ElevatedButton(
                onClick = { isAddingNew = true },
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = GeminiBlue,
                    contentColor = Color(0xFF041E49)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("btn_add_api_provider")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add API", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // List of Configured Providers
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                // Free Tier / Quick Links Ribbon
                QuickPresetsSection(
                    onSelectPreset = { template ->
                        editingConfig = ApiConfigEntity(
                            id = UUID.randomUUID().toString(),
                            name = template.name,
                            category = template.category,
                            providerType = template.providerType,
                            baseUrl = template.defaultBaseUrl,
                            modelName = "",
                            supportedCapabilities = "chat,vision,streaming"
                        )
                    }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Configured Providers (${allConfigs.size})",
                        color = GeminiTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    if (isDiscoveringModels) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = GeminiBlue)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Discovering models...", color = GeminiBlue, fontSize = 11.sp)
                        }
                    }
                }
            }

            items(allConfigs, key = { it.id }) { config ->
                val providerModels = allDiscoveredModels.filter { it.providerType == config.providerType }
                ApiConfigCard(
                    config = config,
                    discoveredModels = providerModels,
                    isTesting = testingId == config.id,
                    onTest = { viewModel.testConfig(config) },
                    onDiscoverModels = { viewModel.refreshModelsForConfig(config) },
                    onSelectModel = { modelId -> viewModel.selectModelForActiveConfig(modelId) },
                    onSetDefault = { viewModel.setSelectedConfig(config) },
                    onToggleEnabled = { enabled -> viewModel.toggleConfigEnabled(config.id, enabled) },
                    onEdit = { editingConfig = config },
                    onDelete = { viewModel.deleteConfig(config.id) }
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // Add or Edit Dialog
    if (isAddingNew || editingConfig != null) {
        AddEditApiDialog(
            initialConfig = editingConfig,
            onDismiss = {
                isAddingNew = false
                editingConfig = null
            },
            onSave = { updated ->
                viewModel.saveConfig(updated, performTest = true)
                isAddingNew = false
                editingConfig = null
                Toast.makeText(context, "Saved ${updated.name} (Auto-discovering models)", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun QuickPresetsSection(
    onSelectPreset: (ProviderTemplate) -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GeminiSurface)
            .border(1.dp, GeminiOutline, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Text(
            text = "Official Developer Dashboards & Free Keys",
            color = GeminiTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Models are discovered live from these official endpoints:",
            color = GeminiTextSecondary,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        ProviderRegistry.templates.take(4).forEach { t ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(GeminiSurfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(t.name, color = GeminiTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (t.hasFreeTier) "Free Tier Available" else "Direct Provider API",
                        color = if (t.hasFreeTier) GeminiGreen else GeminiTextMuted,
                        fontSize = 10.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(t.apiKeyUrl))
                            context.startActivity(intent)
                        }
                    ) {
                        Text(
                            text = if (t.hasFreeTier) "Get Free Key" else "Get Key",
                            color = if (t.hasFreeTier) GeminiGreen else GeminiBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            tint = if (t.hasFreeTier) GeminiGreen else GeminiBlue,
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    OutlinedButton(
                        onClick = { onSelectPreset(t) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("+ Use", fontSize = 11.sp, color = GeminiTextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun ApiConfigCard(
    config: ApiConfigEntity,
    discoveredModels: List<DiscoveredModelEntity>,
    isTesting: Boolean,
    onTest: () -> Unit,
    onDiscoverModels: () -> Unit,
    onSelectModel: (String) -> Unit,
    onSetDefault: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GeminiSurface)
            .border(
                1.dp,
                if (config.isDefault) GeminiBlue.copy(alpha = 0.5f) else GeminiOutline,
                RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
            .testTag("api_config_card_${config.name}")
    ) {
        // Top Row: Name, Category, Default Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = config.name,
                    color = GeminiTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                if (config.isDefault) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(GeminiBlue.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Active Default", color = GeminiBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Enable / Disable switch
            Switch(
                checked = config.isEnabled,
                onCheckedChange = onToggleEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = GeminiBlue,
                    uncheckedThumbColor = GeminiTextMuted,
                    uncheckedTrackColor = GeminiSurfaceVariant
                )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Status & Model info
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatusBadge(status = config.status, latencyMs = config.lastLatencyMs)

            Text(
                text = "Model: ${config.modelName.ifBlank { "Auto-discovering..." }}",
                color = GeminiTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // API Key Masked & Base URL
        Text(
            text = "Key: ${config.maskedApiKey}",
            color = GeminiTextMuted,
            fontSize = 11.sp
        )
        Text(
            text = "Endpoint: ${config.baseUrl.ifBlank { "Default provider host" }}",
            color = GeminiTextMuted,
            fontSize = 11.sp
        )

        // Error message if any
        if (!config.lastErrorMessage.isNullOrBlank() && config.status != "CONNECTED") {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Note: ${config.lastErrorMessage}",
                color = GeminiRed,
                fontSize = 11.sp
            )
        }

        // Live Discovered Models carousel for this provider
        if (discoveredModels.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Discovered Models (${discoveredModels.size}):",
                color = GeminiTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(discoveredModels.take(8)) { model ->
                    val isSelected = config.modelName == model.modelId
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) GeminiBlue.copy(alpha = 0.25f) else GeminiSurfaceVariant)
                            .border(
                                1.dp,
                                if (isSelected) GeminiBlue else GeminiOutline,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectModel(model.modelId) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (model.isVerified) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = GeminiGreen, modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                            }
                            Text(
                                text = model.modelId,
                                color = if (isSelected) GeminiBlue else GeminiTextPrimary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = GeminiOutline, modifier = Modifier.padding(vertical = 10.dp))

        // Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Test Connection Button
                OutlinedButton(
                    onClick = onTest,
                    enabled = !isTesting && config.apiKey.isNotBlank(),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = GeminiBlue)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test", fontSize = 11.sp)
                }

                // Discover Models Button
                OutlinedButton(
                    onClick = onDiscoverModels,
                    enabled = config.apiKey.isNotBlank(),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(12.dp), tint = GeminiBlue)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Discover", fontSize = 11.sp)
                }

                // Set as Default
                if (!config.isDefault && config.isEnabled) {
                    OutlinedButton(
                        onClick = onSetDefault,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Default", fontSize = 11.sp)
                    }
                }
            }

            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = GeminiTextSecondary, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = GeminiRed, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun AddEditApiDialog(
    initialConfig: ApiConfigEntity?,
    onDismiss: () -> Unit,
    onSave: (ApiConfigEntity) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialConfig?.name ?: "Google Gemini") }
    var category by remember { mutableStateOf(initialConfig?.category ?: "Chat / LLM") }
    var providerType by remember { mutableStateOf(initialConfig?.providerType ?: "GEMINI") }
    var apiKey by remember { mutableStateOf(initialConfig?.apiKey ?: "") }
    var baseUrl by remember { mutableStateOf(initialConfig?.baseUrl ?: "https://generativelanguage.googleapis.com") }
    var modelName by remember { mutableStateOf(initialConfig?.modelName ?: "") }
    var isApiKeyVisible by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(GeminiSurface)
                .border(1.dp, GeminiOutline, RoundedCornerShape(18.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (initialConfig != null) "Edit API Provider" else "Add API Provider",
                    color = GeminiTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                // Provider Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Provider Name") },
                    colors = dialogFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                // API Key with Show/Hide toggle
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    colors = dialogFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                            Icon(
                                imageVector = if (isApiKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle key visibility",
                                tint = GeminiTextSecondary
                            )
                        }
                    }
                )

                // Quick Link to get free key
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            val url = when (providerType) {
                                "GEMINI" -> "https://aistudio.google.com/app/apikey"
                                else -> "https://console.groq.com/keys"
                            }
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    ) {
                        Text("Get Free API Key", color = GeminiGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = GeminiGreen, modifier = Modifier.size(12.dp))
                    }
                }

                // Base URL
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Base URL Endpoint") },
                    colors = dialogFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                // Model Name (optional, auto-discovers if blank)
                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = { Text("Model Name (leave blank to auto-discover)") },
                    placeholder = { Text("Auto-discovered dynamically", color = GeminiTextMuted) },
                    colors = dialogFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = GeminiTextSecondary)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    ElevatedButton(
                        onClick = {
                            val updated = (initialConfig ?: ApiConfigEntity(
                                id = UUID.randomUUID().toString(),
                                name = name,
                                category = category,
                                providerType = providerType,
                                baseUrl = baseUrl
                            )).copy(
                                name = name,
                                category = category,
                                providerType = providerType,
                                apiKey = apiKey.trim(),
                                baseUrl = baseUrl.trim(),
                                modelName = modelName.trim()
                            )
                            onSave(updated)
                        },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = GeminiBlue,
                            contentColor = Color(0xFF041E49)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save & Auto-Discover", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GeminiBlue,
    unfocusedBorderColor = GeminiOutline,
    focusedTextColor = GeminiTextPrimary,
    unfocusedTextColor = GeminiTextPrimary,
    focusedLabelColor = GeminiBlue,
    unfocusedLabelColor = GeminiTextSecondary,
    cursorColor = GeminiBlue
)
