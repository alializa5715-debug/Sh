package com.example.ui.screens.chat

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.DiscoveredModelEntity
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.components.OfflineBanner
import com.example.ui.theme.GeminiBackground
import com.example.ui.theme.GeminiBlue
import com.example.ui.theme.GeminiBlueBright
import com.example.ui.theme.GeminiGreen
import com.example.ui.theme.GeminiOutline
import com.example.ui.theme.GeminiOutlineFocused
import com.example.ui.theme.GeminiPillGradient
import com.example.ui.theme.GeminiPurple
import com.example.ui.theme.GeminiSparkleGradient
import com.example.ui.theme.GeminiSurface
import com.example.ui.theme.GeminiSurfaceElevated
import com.example.ui.theme.GeminiSurfaceVariant
import com.example.ui.theme.GeminiTextMuted
import com.example.ui.theme.GeminiTextPrimary
import com.example.ui.theme.GeminiTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
    onOpenHistory: () -> Unit = {}
) {
    val context = LocalContext.current
    val isOnline by viewModel.isOnline.collectAsState()
    val messages by viewModel.currentMessages.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val enabledConfigs by viewModel.enabledConfigs.collectAsState()
    val selectedConfig by viewModel.selectedModelConfig.collectAsState()
    val allDiscoveredModels by viewModel.allDiscoveredModels.collectAsState()
    val isDiscoveringModels by viewModel.isDiscoveringModels.collectAsState()
    val attachedB64 by viewModel.attachedImageBase64.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showModelBottomSheet by remember { mutableStateOf(false) }
    var showAttachSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.attachImageUri(it) }
    }

    val recordAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.speechService.startListening { spoken ->
                inputText = spoken
            }
        } else {
            Toast.makeText(context, "Microphone permission is required for voice input.", Toast.LENGTH_SHORT).show()
        }
    }

    // Scroll to bottom on new message or keyboard opened
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val density = androidx.compose.ui.platform.LocalDensity.current
    val imeBottom = androidx.compose.foundation.layout.WindowInsets.ime.getBottom(density)
    LaunchedEffect(imeBottom) {
        if (imeBottom > 0 && messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GeminiBackground)
            .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
    ) {
        // TOP APP BAR - Nova Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Top-left: small Settings icon only
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("btn_open_settings")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = GeminiTextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Center: "Nova AI by Rauf" + selected model name below
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showModelBottomSheet = true }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .testTag("model_selector_dropdown"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Nova AI by Rauf",
                    color = GeminiTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                val modelDisplay = selectedConfig?.let {
                    if (it.modelName.isNotBlank()) it.modelName else it.name
                } ?: "groq/compound-mini"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = modelDisplay,
                        color = GeminiTextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Switch Model",
                        tint = GeminiTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Right Actions: History + New Chat
            IconButton(
                onClick = onOpenHistory,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("btn_open_history")
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Chat History",
                    tint = GeminiTextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = { viewModel.createNewChat() },
                modifier = Modifier
                    .size(38.dp)
                    .testTag("btn_new_chat")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "New chat",
                    tint = GeminiTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Offline Banner if disconnected
        if (!isOnline) {
            OfflineBanner(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }

        // CHAT MESSAGES LIST / EMPTY STATE
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                GeminiEmptyStateView(
                    onSuggestionClick = { suggestion ->
                        viewModel.sendMessage(suggestion)
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    items(messages, key = { it.id }) { msg ->
                        when (msg.role) {
                            "user" -> GeminiUserMessageItem(msg)
                            "assistant" -> GeminiAssistantMessageItem(
                                message = msg,
                                onSpeak = { text -> viewModel.speechService.speak(text) },
                                onCopy = { text ->
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Nova", text))
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                onShare = { text ->
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, text)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share with"))
                                }
                            )
                            "error" -> ErrorActionCard(
                                errorMessage = msg.content,
                                onConfigureApi = { viewModel.selectTab(AppTab.SETTINGS) },
                                onTestConnection = {
                                    selectedConfig?.let { viewModel.testConfig(it) }
                                },
                                onRefreshModels = {
                                    selectedConfig?.let { viewModel.refreshModelsForConfig(it) }
                                }
                            )
                        }
                    }

                    if (isSending) {
                        item {
                            GeminiThinkingIndicator()
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }

        // ATTACHMENT PREVIEW CHIP
        AnimatedVisibility(visible = attachedB64 != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(GeminiSurfaceVariant)
                        .border(1.dp, GeminiOutline, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = GeminiBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Image attached for vision analysis", color = GeminiTextPrimary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove",
                            tint = GeminiTextSecondary,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { viewModel.clearAttachment() }
                        )
                    }
                }
            }
        }

        // GOOGLE GEMINI FLOATING INPUT PILL
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(GeminiSurface)
                .border(1.dp, GeminiOutline, RoundedCornerShape(32.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "+" Attach Button
            IconButton(
                onClick = { showAttachSheet = true },
                modifier = Modifier
                    .size(40.dp)
                    .testTag("btn_attach_media")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Attach media or files",
                    tint = GeminiTextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Input Text Field
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Ask Nova...", color = GeminiTextMuted, fontSize = 14.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = GeminiTextPrimary,
                    unfocusedTextColor = GeminiTextPrimary,
                    cursorColor = GeminiBlue
                ),
                maxLines = 4
            )

            // Mic / Voice Recognition Button
            IconButton(
                onClick = {
                    val hasPerm = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasPerm) {
                        viewModel.speechService.startListening { spoken ->
                            inputText = spoken
                        }
                    } else {
                        recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .testTag("btn_voice_input")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Input",
                    tint = GeminiTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Send Button OR Gemini Live Waveform Button
            if (inputText.isNotBlank() || attachedB64 != null) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(GeminiSparkleGradient)
                        .clickable {
                            val textToSend = inputText
                            inputText = ""
                            viewModel.sendMessage(textToSend)
                        }
                        .testTag("btn_send_message"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send Message",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(GeminiSurfaceElevated)
                        .clickable {
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) {
                                viewModel.setLiveVoiceActive(true)
                            } else {
                                recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                        .testTag("btn_live_voice_mode"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Nova Live Voice",
                        tint = GeminiBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    // GEMINI DYNAMIC MODEL SELECTOR BOTTOM SHEET
    if (showModelBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showModelBottomSheet = false },
            containerColor = GeminiSurface,
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = GeminiBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Model Discovery & Verification",
                            color = GeminiTextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isDiscoveringModels) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = GeminiBlue)
                    } else {
                        IconButton(
                            onClick = {
                                selectedConfig?.let { viewModel.refreshModelsForConfig(it) }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh models",
                                tint = GeminiBlue
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Live models discovered directly from your configured AI APIs",
                    color = GeminiTextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Configured Providers List
                Text(
                    text = "Active AI Configurations",
                    color = GeminiTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                enabledConfigs.forEach { cfg ->
                    val isSelected = cfg.id == selectedConfig?.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) GeminiSurfaceElevated else GeminiSurfaceVariant)
                            .border(
                                1.dp,
                                if (isSelected) GeminiBlue.copy(alpha = 0.5f) else GeminiOutline,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                viewModel.setSelectedConfig(cfg)
                                showModelBottomSheet = false
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = cfg.name,
                                    color = GeminiTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                if (cfg.isDefault) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(GeminiBlue.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("Active", color = GeminiBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Model: ${cfg.modelName.ifBlank { "Auto-discovering..." }}",
                                color = GeminiTextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = GeminiBlue, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Discovered Models for current provider
                val currentProviderType = selectedConfig?.providerType ?: "GEMINI"
                val relevantDiscovered = allDiscoveredModels.filter { it.providerType == currentProviderType }

                if (relevantDiscovered.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Discovered Models for $currentProviderType",
                        color = GeminiTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    relevantDiscovered.take(6).forEach { model ->
                        val isCurrentModel = selectedConfig?.modelName == model.modelId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isCurrentModel) GeminiSurfaceElevated else GeminiSurfaceVariant)
                                .border(
                                    1.dp,
                                    if (isCurrentModel) GeminiBlue.copy(alpha = 0.4f) else GeminiOutline,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    viewModel.selectModelForActiveConfig(model.modelId)
                                    showModelBottomSheet = false
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = model.displayName.ifBlank { model.modelId },
                                        color = GeminiTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (model.freeTier) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(GeminiGreen.copy(alpha = 0.2f))
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text("Free Tier", color = GeminiGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Text(
                                    text = model.modelId,
                                    color = GeminiTextMuted,
                                    fontSize = 10.sp
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (model.isVerified) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Verified",
                                        tint = GeminiGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Verified", color = GeminiGreen, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = GeminiOutline)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showModelBottomSheet = false
                            viewModel.selectTab(AppTab.SETTINGS)
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = GeminiBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Central API Hub Settings", color = GeminiBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // ATTACHMENT BOTTOM SHEET
    if (showAttachSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachSheet = false },
            containerColor = GeminiSurface,
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Add to conversation",
                    color = GeminiTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(GeminiSurfaceVariant)
                        .border(1.dp, GeminiOutline, RoundedCornerShape(14.dp))
                        .clickable {
                            showAttachSheet = false
                            imagePickerLauncher.launch("image/*")
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = GeminiBlue)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Upload Image / Vision", color = GeminiTextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("Analyze diagrams, photos, or documents with multimodal models", color = GeminiTextSecondary, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(GeminiSurfaceVariant)
                        .border(1.dp, GeminiOutline, RoundedCornerShape(14.dp))
                        .clickable {
                            showAttachSheet = false
                            viewModel.sendMessage("Turn my class notes into custom quizzes")
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = GeminiPurple)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Interactive Quiz Generator", color = GeminiTextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("Create multi-choice knowledge checks", color = GeminiTextSecondary, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(GeminiSurfaceVariant)
                        .border(1.dp, GeminiOutline, RoundedCornerShape(14.dp))
                        .clickable {
                            showAttachSheet = false
                            viewModel.sendMessage("Deep dive: Roman aqueducts architecture and engineering")
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = GeminiBlue)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Deep Dive & Architecture Diagrams", color = GeminiTextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("Explore annotated architectural components", color = GeminiTextSecondary, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun GeminiEmptyStateView(onSuggestionClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        // Signature Google Gemini Greeting with Gradient Typography
        Text(
            text = "Hello, Rauf",
            style = TextStyle(
                brush = GeminiSparkleGradient,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "How can Nova help you today?",
            color = GeminiTextSecondary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Curated Suggestion Cards
        val suggestions = listOf(
            Pair("Help me write", "Draft a clear, professional response"),
            Pair("Brainstorm ideas", "5 innovative AI application concepts"),
            Pair("Live Voice", "Start real-time voice-to-voice discussion"),
            Pair("Explain concepts", "Quantum computing in simple terms")
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            suggestions.forEach { (title, subtitle) ->
                Box(
                    modifier = Modifier
                        .width(160.dp)
                        .height(130.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(GeminiSurface)
                        .border(1.dp, GeminiOutline, RoundedCornerShape(16.dp))
                        .clickable { onSuggestionClick("$title: $subtitle") }
                        .padding(14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = title,
                            color = GeminiTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = subtitle,
                            color = GeminiTextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = GeminiBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GeminiUserMessageItem(message: ChatMessageEntity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("user_message_bubble"),
        horizontalAlignment = Alignment.End
    ) {
        // Attached Image Preview
        if (!message.mediaUri.isNullOrBlank() && message.mediaUri.contains("base64,")) {
            val base64Data = message.mediaUri.substringAfter("base64,")
            val decodedBytes = try {
                Base64.decode(base64Data, Base64.DEFAULT)
            } catch (e: Exception) {
                null
            }
            if (decodedBytes != null) {
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Attached Image",
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .size(160.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, GeminiOutline, RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp))
                .background(GeminiSurfaceVariant)
                .border(1.dp, GeminiOutline, RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = message.content,
                color = GeminiTextPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun GeminiAssistantMessageItem(
    message: ChatMessageEntity,
    onSpeak: (String) -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("assistant_message_bubble"),
        horizontalAlignment = Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.Top) {
            // Google Gemini Sparkle Avatar
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(GeminiPillGradient)
                    .border(1.dp, GeminiBlue.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Nova",
                    tint = GeminiBlue,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Assistant Text Body
                Text(
                    text = message.content,
                    color = GeminiTextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )

                // Interactive Cards if present
                if (message.cardType == "quiz" && !message.cardJson.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    QuizCard(cardJson = message.cardJson)
                } else if (message.cardType == "diagram" && !message.cardJson.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    DiagramCard(cardJson = message.cardJson)
                }

                // Google Gemini Action Row
                Row(
                    modifier = Modifier
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onSpeak(message.spokenText ?: message.content) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Read aloud",
                            tint = GeminiTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = { onCopy(message.content) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy message",
                            tint = GeminiTextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    IconButton(
                        onClick = { onShare(message.content) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share message",
                            tint = GeminiTextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    IconButton(
                        onClick = { /* Feedback thumbs up */ },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = "Good response",
                            tint = GeminiTextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    IconButton(
                        onClick = { /* Feedback thumbs down */ },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbDown,
                            contentDescription = "Bad response",
                            tint = GeminiTextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GeminiThinkingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(GeminiSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                color = GeminiBlue,
                strokeWidth = 2.dp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Nova is thinking...",
            color = GeminiTextSecondary,
            fontSize = 12.sp
        )
    }
}
