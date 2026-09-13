package com.example.ui.screens.history

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ChatSessionEntity
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.theme.GeminiBackground
import com.example.ui.theme.GeminiBlue
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var filterPinnedOnly by remember { mutableStateOf(false) }

    var renamingSession by remember { mutableStateOf<ChatSessionEntity?>(null) }
    var newTitleText by remember { mutableStateOf("") }
    var sessionToDelete by remember { mutableStateOf<String?>(null) }

    val filteredSessions = remember(sessions, searchQuery, filterPinnedOnly) {
        sessions.filter {
            (searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true)) &&
                    (!filterPinnedOnly || it.isPinned)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GeminiBackground)
            .testTag("history_screen")
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_history_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = GeminiTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Column {
                    Text(
                        text = "Recent Chats",
                        color = GeminiTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${sessions.size} saved conversations",
                        color = GeminiTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            ElevatedButton(
                onClick = {
                    viewModel.createNewChat()
                    viewModel.selectTab(AppTab.CHAT)
                },
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = GeminiBlue,
                    contentColor = Color(0xFF041E49)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Chat", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search conversations...", color = GeminiTextMuted, fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = GeminiTextSecondary, modifier = Modifier.size(18.dp))
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = GeminiSurface,
                unfocusedContainerColor = GeminiSurface,
                focusedBorderColor = GeminiBlue,
                unfocusedBorderColor = GeminiOutline,
                focusedTextColor = GeminiTextPrimary,
                unfocusedTextColor = GeminiTextPrimary
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (!filterPinnedOnly) GeminiBlue.copy(alpha = 0.2f) else GeminiSurface)
                    .border(1.dp, if (!filterPinnedOnly) GeminiBlue else GeminiOutline, RoundedCornerShape(16.dp))
                    .clickable { filterPinnedOnly = false }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("All Chats", color = if (!filterPinnedOnly) GeminiBlue else GeminiTextSecondary, fontSize = 12.sp)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (filterPinnedOnly) GeminiPurple.copy(alpha = 0.25f) else GeminiSurface)
                    .border(1.dp, if (filterPinnedOnly) GeminiPurple else GeminiOutline, RoundedCornerShape(16.dp))
                    .clickable { filterPinnedOnly = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Pinned", color = if (filterPinnedOnly) GeminiPurple else GeminiTextSecondary, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Session List
        if (filteredSessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotBlank()) "No conversations match your search." else "No conversations yet. Start a new chat!",
                    color = GeminiTextSecondary,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredSessions, key = { it.id }) { session ->
                    val isCurrent = session.id == currentSessionId

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isCurrent) GeminiSurfaceElevated else GeminiSurface)
                            .border(
                                1.dp,
                                if (isCurrent) GeminiBlue.copy(alpha = 0.5f) else GeminiOutline,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                viewModel.selectSession(session.id)
                                viewModel.selectTab(AppTab.CHAT)
                            }
                            .padding(14.dp)
                            .testTag("session_item_${session.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (session.isPinned) GeminiPurple.copy(alpha = 0.2f) else GeminiSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (session.isPinned) Icons.Default.PushPin else Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = if (session.isPinned) GeminiPurple else GeminiTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = session.title,
                                color = GeminiTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(session.updatedAt)),
                                    color = GeminiTextMuted,
                                    fontSize = 11.sp
                                )
                                if (session.modelUsed.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("•", color = GeminiTextMuted, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = session.modelUsed,
                                        color = GeminiBlue,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Actions
                        IconButton(
                            onClick = { viewModel.togglePinSession(session.id, !session.isPinned) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pin session",
                                tint = if (session.isPinned) GeminiPurple else GeminiTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                renamingSession = session
                                newTitleText = session.title
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Rename",
                                tint = GeminiTextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        IconButton(
                            onClick = { sessionToDelete = session.id },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = GeminiRed,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }

    // Rename Dialog
    renamingSession?.let { session ->
        AlertDialog(
            onDismissRequest = { renamingSession = null },
            title = { Text("Rename Conversation", color = GeminiTextPrimary, fontSize = 16.sp) },
            text = {
                OutlinedTextField(
                    value = newTitleText,
                    onValueChange = { newTitleText = it },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = GeminiTextPrimary,
                        unfocusedTextColor = GeminiTextPrimary,
                        focusedBorderColor = GeminiBlue
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newTitleText.isNotBlank()) {
                            viewModel.renameSession(session.id, newTitleText.trim())
                        }
                        renamingSession = null
                    }
                ) {
                    Text("Save", color = GeminiBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingSession = null }) {
                    Text("Cancel", color = GeminiTextSecondary)
                }
            },
            containerColor = GeminiSurface
        )
    }

    // Delete confirmation dialog
    sessionToDelete?.let { sId ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Delete Conversation?", color = GeminiTextPrimary, fontSize = 16.sp) },
            text = { Text("This will permanently remove this chat and its messages.", color = GeminiTextSecondary, fontSize = 13.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSession(sId)
                        sessionToDelete = null
                    }
                ) {
                    Text("Delete", color = GeminiRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("Cancel", color = GeminiTextSecondary)
                }
            },
            containerColor = GeminiSurface
        )
    }
}
