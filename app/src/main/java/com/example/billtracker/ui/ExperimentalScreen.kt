package com.example.billtracker.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class ChatMsg(
    val text: String,
    val isUser: Boolean,
    val isToggle: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperimentalScreen(
    aiChatEnabled: Boolean,
    onAiChatToggle: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val listState = rememberLazyListState()
    val messages = remember(aiChatEnabled) {
        listOfNotNull(
            ChatMsg(
                text = "你好！我是你的 AI 记账助手。\n\n我可以帮你用自然语言快速记账，就像聊天一样简单。",
                isUser = false
            ),
            ChatMsg(
                text = "点击下方选项开启或关闭 AI 记账功能：",
                isUser = false
            ),
            ChatMsg(
                text = if (aiChatEnabled) "AI 聊天式记账" else "AI 聊天式记账",
                isUser = false,
                isToggle = true
            ),
            if (aiChatEnabled) ChatMsg(
                text = "✅ AI 聊天式记账已开启\n\n现在点击首页的 + 号按钮即可进入 AI 记账，输入「吃饭花了35块」这样的话就能快速记账。",
                isUser = false
            ) else null
        )
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 助手", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFEDEDED))
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5)),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                if (msg.isToggle) {
                    ToggleBubble(
                        enabled = aiChatEnabled,
                        onClick = { onAiChatToggle(!aiChatEnabled) }
                    )
                } else {
                    ChatBubble(
                        text = msg.text,
                        isUser = msg.isUser
                    )
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun ChatBubble(
    text: String,
    isUser: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Surface(
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Top),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("AI", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = if (isUser) 16.dp else 4.dp,
                topEnd = if (isUser) 4.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            color = if (isUser) Color(0xFF95EC69) else Color.White,
            shadowElevation = 1.dp
        ) {
            Text(
                text = text,
                fontSize = 14.sp,
                color = Color(0xFF1F1F1F),
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }

        if (isUser) {
            Spacer(Modifier.width(8.dp))
            Surface(
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Top),
                shape = CircleShape,
                color = Color(0xFF4A90D9)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("我", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ToggleBubble(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Spacer(Modifier.width(44.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clickable { onClick() },
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "AI 聊天式记账",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1F1F1F)
                    )
                    Text(
                        if (enabled) "已开启 - 点击 + 号使用 AI 记账" else "使用自然语言完成记账，点击开启",
                        fontSize = 12.sp,
                        color = Color(0xFF9AA0A6)
                    )
                }
                // √ mark when enabled, nothing when disabled
                if (enabled) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF07C160)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "✓",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
