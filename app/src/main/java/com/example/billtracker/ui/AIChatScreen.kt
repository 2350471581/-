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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billtracker.data.AIBillService
import com.example.billtracker.data.AIParseResult
import com.example.billtracker.data.TransactionType
import kotlinx.coroutines.launch

private data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val parseResult: AIParseResult? = null,
    val isConfirmed: Boolean = false,
    val pendingTypeSelection: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatScreen(
    onBack: () -> Unit,
    onAddTransaction: (Double, TransactionType, String) -> Unit
) {
    val aiService = remember { AIBillService() }
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 欢迎消息
    LaunchedEffect(Unit) {
        messages = listOf(
            ChatMessage(
                text = "你好！我是AI记账助手。你可以用日常语言告诉我收支情况，比如：\n\n" +
                        "「今天吃饭花了35块」\n" +
                        "「工资到账8000元」\n" +
                        "「打车花了20块」",
                isUser = false
            )
        )
    }

    // 自动滚动到底部
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun sendMessage() {
        val text = inputText.trim()
        if (text.isBlank()) return
        inputText = ""

        val userMsg = ChatMessage(text = text, isUser = true)
        messages = messages + userMsg

        scope.launch {
            val result = aiService.parse(text)
            if (result != null && result.type != null) {
                val typeStr = if (result.type == TransactionType.INCOME) "收入" else "支出"
                val reply = "已识别：${typeStr} ${result.amount}元 - ${result.category}\n${result.description}"
                messages = messages + ChatMessage(text = reply, isUser = false, parseResult = result)
            } else if (result != null && result.type == null) {
                val reply = "已识别金额 ${result.amount}元 - ${result.category}\n请选择这笔是收入还是支出？"
                messages = messages + ChatMessage(text = reply, isUser = false, parseResult = result, pendingTypeSelection = true)
            } else {
                messages = messages + ChatMessage(
                    text = "抱歉，无法从您的描述中识别出账单信息。请包含金额和用途，例如「吃饭花了35块」",
                    isUser = false
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("AI 记账", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFEDEDED)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            // 消息列表
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(
                        message = msg,
                        onSelectType = if (msg.pendingTypeSelection) { selectedType ->
                            val updated = msg.parseResult?.copy(type = selectedType)
                            if (updated != null) {
                                val typeStr = if (selectedType == TransactionType.INCOME) "收入" else "支出"
                                messages = messages.map {
                                    if (it == msg) it.copy(
                                        text = "已确认：${typeStr} ${updated.amount}元 - ${updated.category}\n${updated.description}",
                                        parseResult = updated,
                                        pendingTypeSelection = false
                                    ) else it
                                }
                            }
                        } else null,
                        onConfirm = if (!msg.isUser && msg.parseResult != null && !msg.isConfirmed) {
                            {
                                onAddTransaction(msg.parseResult.amount, msg.parseResult.type!!, msg.parseResult.description)
                                messages = messages.map {
                                    if (it == msg) it.copy(isConfirmed = true) else it
                                }
                                scope.launch { snackbarHostState.showSnackbar("已添加账单") }
                            }
                        } else null,
                        onCancel = if (!msg.isUser && msg.parseResult != null && !msg.isConfirmed) {
                            {
                                messages = messages.map {
                                    if (it == msg) it.copy(text = "已取消", parseResult = null) else it
                                }
                            }
                        } else null
                    )
                }
                item { Spacer(Modifier.height(4.dp)) }
            }

            // 底部输入栏
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 2.dp,
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("输入收支描述...", fontSize = 14.sp) },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (inputText.isNotBlank()) sendMessage()
                        }),
                        maxLines = 3,
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = { sendMessage() },
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary else Color(0xFFC8C8C8)
                        ),
                        enabled = inputText.isNotBlank()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "发送", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    onConfirm: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    onSelectType: ((TransactionType) -> Unit)? = null
) {
    val isUser = message.isUser
    val isError = !isUser && message.parseResult == null && message.text.startsWith("抱歉")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // 头像
            Surface(
                modifier = Modifier.size(36.dp).align(Alignment.Top),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("AI", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
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
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        text = message.text,
                        fontSize = 14.sp,
                        color = if (isUser) Color(0xFF1F1F1F) else Color(0xFF1F1F1F),
                        lineHeight = 20.sp
                    )

                    // 收支方向选择（类型不明确时）
                    if (message.pendingTypeSelection && onSelectType != null) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onSelectType(TransactionType.INCOME) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF7A)),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                            ) {
                                Text("收入", fontSize = 13.sp, color = Color.White)
                            }
                            Button(
                                onClick = { onSelectType(TransactionType.EXPENSE) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA6B5C)),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                            ) {
                                Text("支出", fontSize = 13.sp, color = Color.White)
                            }
                        }
                    }

                    // 确认/取消按钮
                    if (!message.pendingTypeSelection && onConfirm != null && onCancel != null) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = onConfirm,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(30.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF07C160)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text("确认添加", fontSize = 12.sp, color = Color.White)
                            }
                            TextButton(
                                onClick = onCancel,
                                modifier = Modifier.height(30.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text("取消", fontSize = 12.sp, color = Color(0xFF9AA0A6))
                            }
                        }
                    }

                    // 已确认标记
                    if (message.isConfirmed) {
                        Spacer(Modifier.height(4.dp))
                        Text("✓ 已添加", fontSize = 11.sp, color = Color(0xFF07C160), fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        if (isUser) {
            Spacer(Modifier.width(8.dp))
            Surface(
                modifier = Modifier.size(36.dp).align(Alignment.Top),
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
