package com.jizhang.tracker.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jizhang.tracker.R
import com.jizhang.tracker.data.AIBillService
import com.jizhang.tracker.data.AIParseResult
import com.jizhang.tracker.data.TransactionType
import com.jizhang.tracker.data.AIBillException
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
import com.jizhang.tracker.viewmodel.LedgerViewModel

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
    onAddTransaction: (Double, TransactionType, String) -> Unit,
    viewModel: LedgerViewModel,
    aiService: AIBillService,
    aiNickname: String = "AI 记账助手",
    aiAvatarIndex: Int = 21,
    aiCustomAvatarUri: String = "",
    onAiCustomizeClick: () -> Unit = {}
) {
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val aiWelcomeMessage = stringResource(R.string.ai_welcome_message)
    val aiTypePrompt = stringResource(R.string.ai_type_prompt)
    val aiParseFailed = stringResource(R.string.ai_parse_failed)
    val aiNetworkError = stringResource(R.string.ai_network_error)
    val aiAuthError = stringResource(R.string.ai_auth_error)
    val aiGenericError = stringResource(R.string.ai_generic_error)
    val aiIncome = stringResource(R.string.ai_income)
    val aiExpense = stringResource(R.string.ai_expense)
    val aiConfirmedFormat = stringResource(R.string.ai_confirmed_format)
    val aiSnackbarConfirmed = stringResource(R.string.ai_snackbar_confirmed)
    val aiCancelled = stringResource(R.string.ai_cancelled)
    val aiInputCd = stringResource(R.string.ai_input_cd)

    val aiAvatarEmoji = avatarEmojis.getOrElse(aiAvatarIndex) { "🤖" }
    val aiCustomBitmap = remember(aiCustomAvatarUri) {
        if (aiCustomAvatarUri.isNotBlank()) {
            try {
                val uri = Uri.parse(aiCustomAvatarUri)
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            } catch (_: Exception) { null }
        } else null
    }

    // 欢迎消息
    LaunchedEffect(Unit) {
        messages = listOf(
            ChatMessage(
                text = aiWelcomeMessage,
                isUser = false
            )
        )
    }

    // 系统返回键
    BackHandler(onBack = onBack)

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
            isLoading = true
            val summary = viewModel.getTodaySummary()
            val result = aiService.parseChat(text, summary)
            isLoading = false
            result.fold(
                onSuccess = { chatResult ->
                    if (chatResult.parseResult != null && chatResult.parseResult.type != null) {
                        messages = messages + ChatMessage(text = chatResult.reply, isUser = false, parseResult = chatResult.parseResult)
                    } else if (chatResult.parseResult != null && chatResult.parseResult.type == null) {
                        val reply = chatResult.reply.ifEmpty { aiTypePrompt }
                        messages = messages + ChatMessage(text = reply, isUser = false, parseResult = chatResult.parseResult, pendingTypeSelection = true)
                    } else {
                        val reply = chatResult.reply.ifEmpty { aiParseFailed }
                        messages = messages + ChatMessage(text = reply, isUser = false)
                    }
                },
                onFailure = { err ->
                    val errorMsg = (err as? AIBillException)?.let {
                        if (it.isNetworkError) aiNetworkError
                        else if (it.isAuthError) aiAuthError
                        else it.message ?: aiGenericError
                    } ?: aiGenericError
                    messages = messages + ChatMessage(text = errorMsg, isUser = false)
                }
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onAiCustomizeClick() }
                    ) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = if (aiCustomBitmap != null) Color.Transparent else MaterialTheme.colorScheme.primary
                        ) {
                            if (aiCustomBitmap != null) {
                                Image(
                                    bitmap = aiCustomBitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(aiAvatarEmoji, fontSize = 18.sp)
                                }
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(aiNickname, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.ai_back_cd))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
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
                        aiAvatarEmoji = aiAvatarEmoji,
                        aiCustomBitmap = aiCustomBitmap,
                        onSelectType = if (msg.pendingTypeSelection) { selectedType ->
                            val updated = msg.parseResult?.copy(type = selectedType)
                            if (updated != null) {
                                val typeStr = if (selectedType == TransactionType.INCOME)
                                    aiIncome
                                else
                                    aiExpense
                                messages = messages.map {
                                    if (it == msg) it.copy(
                                        text = aiConfirmedFormat.format(typeStr, updated.amount.toString(), updated.category, updated.description),
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
                                scope.launch {
                                    snackbarHostState.showSnackbar(aiSnackbarConfirmed)
                                    val summary = viewModel.getTodaySummary()
                                    val reply = aiService.chatWithContext(msg.parseResult, summary)
                                    reply.onSuccess { text ->
                                        messages = messages + ChatMessage(text = text, isUser = false)
                                    }
                                }
                            }
                        } else null,
                        onCancel = if (!msg.isUser && msg.parseResult != null && !msg.isConfirmed) {
                            {
                                messages = messages.map {
                                    if (it == msg) it.copy(text = aiCancelled, parseResult = null) else it
                                }
                            }
                        } else null
                    )
                }
                item { Spacer(Modifier.height(4.dp)) }

                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Surface(
                                modifier = Modifier.size(40.dp).align(Alignment.Top),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(aiAvatarEmoji, fontSize = 20.sp)
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(3) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
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
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f).semantics { contentDescription = aiInputCd },
                        placeholder = { Text(stringResource(R.string.ai_input_placeholder), fontSize = 15.sp) },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (inputText.isNotBlank()) sendMessage()
                        }),
                        maxLines = 3,
                        textStyle = LocalTextStyle.current.copy(fontSize = 15.sp)
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = { sendMessage() },
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary else DisabledGray
                        ),
                        enabled = inputText.isNotBlank()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.ai_send_cd), tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    aiAvatarEmoji: String = "🤖",
    aiCustomBitmap: android.graphics.Bitmap? = null,
    onConfirm: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    onSelectType: ((TransactionType) -> Unit)? = null
) {
    val isUser = message.isUser

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Surface(
                modifier = Modifier.size(40.dp).align(Alignment.Top),
                shape = CircleShape,
                color = if (aiCustomBitmap != null) Color.Transparent else MaterialTheme.colorScheme.primary
            ) {
                if (aiCustomBitmap != null) {
                    Image(
                        bitmap = aiCustomBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text(aiAvatarEmoji, fontSize = 20.sp)
                    }
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
                color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        text = message.text,
                        fontSize = 16.sp,
                        color = if (isUser) DarkTextColor else DarkTextColor,
                        lineHeight = 22.sp
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
                                modifier = Modifier.height(34.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                            ) {
                                Text(stringResource(R.string.ai_income), fontSize = 14.sp, color = Color.White)
                            }
                            Button(
                                onClick = { onSelectType(TransactionType.EXPENSE) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                            ) {
                                Text(stringResource(R.string.ai_expense), fontSize = 14.sp, color = Color.White)
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
                                modifier = Modifier.height(32.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = WechatGreen),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text(stringResource(R.string.ai_confirm_add), fontSize = 13.sp, color = Color.White)
                            }
                            TextButton(
                                onClick = onCancel,
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text(stringResource(R.string.ai_cancel), fontSize = 13.sp, color = SubtleText)
                            }
                        }
                    }

                    // 已确认标记
                    if (message.isConfirmed) {
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(R.string.ai_added), fontSize = 12.sp, color = WechatGreen, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        if (isUser) {
            Spacer(Modifier.width(8.dp))
            Surface(
                modifier = Modifier.size(40.dp).align(Alignment.Top),
                shape = CircleShape,
                color = Color(0xFF4A90D9)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.ai_me), fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
