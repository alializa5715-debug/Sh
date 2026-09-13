package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun StatusBadge(
    status: String,
    latencyMs: Long = 0L,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, icon, label) = when (status.uppercase()) {
        "CONNECTED", "VERIFIED", "READY" -> {
            val latencyText = if (latencyMs > 0) " (${latencyMs}ms)" else ""
            Quad(
                EmeraldGreen.copy(alpha = 0.15f),
                EmeraldGreen,
                Icons.Default.CheckCircle,
                "Verified & Ready$latencyText"
            )
        }
        "INVALID_KEY", "INVALID_API_KEY", "UNAUTHORIZED" -> Quad(
            CrimsonRed.copy(alpha = 0.15f),
            CrimsonRed,
            Icons.Default.Error,
            "Invalid Key"
        )
        "RATE_LIMITED", "RATE_LIMIT", "TOO_MANY_REQUESTS" -> Quad(
            AmberWarning.copy(alpha = 0.15f),
            AmberWarning,
            Icons.Default.Warning,
            "Rate Limited"
        )
        "MODEL_UNAVAILABLE", "UNAVAILABLE" -> Quad(
            Color(0xFFFF9800).copy(alpha = 0.18f),
            Color(0xFFFF9800), // Orange
            Icons.Default.Warning,
            "Model Unavailable"
        )
        "ERROR" -> Quad(
            CrimsonRed.copy(alpha = 0.15f),
            CrimsonRed,
            Icons.Default.Error,
            "Error"
        )
        else -> Quad(
            Color(0xFF2E2E2E),
            TextMuted,
            Icons.Default.Help,
            "Untested"
        )
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = textColor,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CrimsonRed.copy(alpha = 0.2f))
            .border(1.dp, CrimsonRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Offline",
            tint = CrimsonRed,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "You're offline. Check your internet connection.",
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PulsingIndicator(
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
    )
}
