package com.example.ui.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.SecondarySurface
import com.example.ui.theme.SoftViolet
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import org.json.JSONObject

@Composable
fun QuizCard(
    cardJson: String,
    modifier: Modifier = Modifier
) {
    var selectedOptionId by remember { mutableStateOf<String?>(null) }

    val quizData: Pair<String, List<QuizOption>> = remember(cardJson) {
        try {
            val json = JSONObject(cardJson)
            val question = json.optString("question", "Quiz Question")
            val optionsArr = json.optJSONArray("options")
            val options = mutableListOf<QuizOption>()
            if (optionsArr != null) {
                for (i in 0 until optionsArr.length()) {
                    val opt = optionsArr.getJSONObject(i)
                    options.add(
                        QuizOption(
                            id = opt.optString("id", ('A' + i).toString()),
                            text = opt.optString("text", ""),
                            isCorrect = opt.optBoolean("isCorrect", false),
                            explanation = opt.optString("explanation", "")
                        )
                    )
                }
            }
            Pair(question, options as List<QuizOption>)
        } catch (e: Exception) {
            Pair("Quiz Question", emptyList<QuizOption>())
        }
    }

    val question = quizData.first
    val options = quizData.second

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, SoftViolet.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("quiz_interactive_card")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(SoftViolet.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "Quiz",
                    tint = SoftViolet,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Interactive Knowledge Check",
                color = SoftViolet,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = question,
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 22.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        options.forEach { option ->
            val isSelected = selectedOptionId == option.id
            val hasAnswered = selectedOptionId != null

            val borderColor = when {
                !hasAnswered -> BorderDark
                option.isCorrect -> EmeraldGreen
                isSelected -> CrimsonRed
                else -> BorderDark.copy(alpha = 0.5f)
            }

            val bgColor = when {
                !hasAnswered -> SurfaceVariantDark
                option.isCorrect -> EmeraldGreen.copy(alpha = 0.12f)
                isSelected -> CrimsonRed.copy(alpha = 0.12f)
                else -> SurfaceVariantDark.copy(alpha = 0.5f)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgColor)
                    .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                    .clickable {
                        if (!hasAnswered) {
                            selectedOptionId = option.id
                        }
                    }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected || (hasAnswered && option.isCorrect)) borderColor else BorderDark
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasAnswered && option.isCorrect) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Correct",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    } else if (hasAnswered && isSelected && !option.isCorrect) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Incorrect",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Text(
                            text = option.id,
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = option.text,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Explanation Box
        AnimatedVisibility(visible = selectedOptionId != null) {
            val chosen = options.find { it.id == selectedOptionId }
            val correctOpt = options.find { it.isCorrect }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SecondarySurface)
                    .padding(12.dp)
            ) {
                Text(
                    text = if (chosen?.isCorrect == true) "Correct!" else "Incorrect. The correct answer is ${correctOpt?.id}.",
                    color = if (chosen?.isCorrect == true) EmeraldGreen else CrimsonRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = correctOpt?.explanation.orEmpty(),
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

private data class QuizOption(
    val id: String,
    val text: String,
    val isCorrect: Boolean,
    val explanation: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiagramCard(
    cardJson: String,
    modifier: Modifier = Modifier
) {
    val diagramData: Pair<String, List<DiagramLabel>> = remember(cardJson) {
        try {
            val json = JSONObject(cardJson)
            val t = json.optString("title", "Interactive Diagram")
            val arr = json.optJSONArray("labels")
            val list = mutableListOf<DiagramLabel>()
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    list.add(DiagramLabel(item.optString("name"), item.optString("desc")))
                }
            }
            Pair(t, list as List<DiagramLabel>)
        } catch (e: Exception) {
            Pair("Diagram", emptyList<DiagramLabel>())
        }
    }

    val title = diagramData.first
    val labels = diagramData.second

    var selectedLabel by remember { mutableStateOf(labels.firstOrNull()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, ElectricBlue.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("diagram_interactive_card")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(ElectricBlue.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Diagram",
                    tint = ElectricBlue,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = ElectricBlue,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Tap any engineering component to explore its architectural function:",
            color = TextSecondary,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Interactive Hotspot Chips
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            labels.forEach { label ->
                val isSelected = selectedLabel?.name == label.name
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected) ElectricBlue.copy(alpha = 0.25f) else SurfaceVariantDark
                        )
                        .border(
                            1.dp,
                            if (isSelected) ElectricBlue else BorderDark,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedLabel = label }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label.name,
                        color = if (isSelected) ElectricBlue else TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Selected Hotspot Details Box
        selectedLabel?.let { label ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SecondarySurface)
                    .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = label.name,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label.desc,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

private data class DiagramLabel(val name: String, val desc: String)

@Composable
fun ErrorActionCard(
    errorMessage: String,
    onConfigureApi: () -> Unit,
    onTestConnection: () -> Unit,
    onRefreshModels: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CrimsonRed.copy(alpha = 0.08f))
            .border(1.dp, CrimsonRed.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(14.dp)
            .testTag("error_action_card")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "Error",
                tint = CrimsonRed,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Model Availability / Connection Alert",
                color = CrimsonRed,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = errorMessage,
            color = TextPrimary,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onRefreshModels != null) {
                ElevatedButton(
                    onClick = onRefreshModels,
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = ElectricBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_error_refresh_models")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Auto-Discover Models", fontSize = 12.sp)
                }
            }

            ElevatedButton(
                onClick = onConfigureApi,
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = CrimsonRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_error_configure_api")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Configure API", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = onTestConnection,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_error_test_connection")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Test Connection", fontSize = 12.sp)
            }
        }
    }
}
