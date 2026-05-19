package com.jizhang.tracker.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jizhang.tracker.ui.DarkSubtleText
import kotlinx.coroutines.delay

@Composable
fun AIChatTutorialDialog(
    onDismiss: () -> Unit,
) {
    var countdown by remember { mutableIntStateOf(3) }
    val enabled = countdown <= 0
    LaunchedEffect(Unit) { while (countdown > 0) { delay(1000); countdown-- } }
    AlertDialog(
        onDismissRequest = { if (enabled) onDismiss() },
        shape = RoundedCornerShape(20.dp),
        title = { Text("AI 聊天记账", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
        text = {
            Column {
                listOf(
                    "💬 像聊天一样记账",
                    "输入「中午吃饭花了35块」即可快速记账",
                    "",
                    "🤖 AI 智能识别",
                    "自动提取金额、类别，支持 DeepSeek 大模型",
                    "",
                    "📋 确认流程",
                    "AI 识别后点击「确认添加」才真正记账",
                    "",
                    "📱 本地兜底",
                    "网络不可用时自动使用本地规则识别"
                ).forEach { line ->
                    val isHeader = line.startsWith("💬") || line.startsWith("🤖") || line.startsWith("📋") || line.startsWith("📱")
                    Text(
                        text = line,
                        fontSize = if (isHeader) 15.sp else 13.sp,
                        fontWeight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isHeader) MaterialTheme.colorScheme.onBackground else DarkSubtleText,
                        modifier = Modifier.padding(top = if (line.isBlank()) 4.dp else 2.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                enabled = enabled,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text(if (enabled) "知道了" else "($countdown) 知道了", color = Color.White) }
        }
    )
}

@Composable
fun AiChatEnabledDialog(
    onDismiss: () -> Unit,
) {
    var countdown by remember { mutableIntStateOf(3) }
    val enabled = countdown <= 0
    LaunchedEffect(Unit) { while (countdown > 0) { delay(1000); countdown-- } }
    AlertDialog(
        onDismissRequest = { if (enabled) onDismiss() },
        shape = RoundedCornerShape(20.dp),
        title = { Text("AI 助手已开启", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🤖", fontSize = 40.sp)
                Spacer(Modifier.height(12.dp))
                Text("AI 聊天记账已开启", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "点击记账助手的 + 号按钮即可使用 AI 自然语言记账",
                    fontSize = 14.sp, color = DarkSubtleText, lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                enabled = enabled,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(if (enabled) "知道了" else "($countdown) 知道了", color = Color.White)
            }
        }
    )
}
