package com.example.billtracker.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.billtracker.data.BillImporter
import com.example.billtracker.data.ParsedBill
import com.example.billtracker.data.TransactionSource
import com.example.billtracker.data.TransactionType
import com.example.billtracker.ui.components.TagChip
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    onBack: () -> Unit,
    onImport: (List<ParsedBill>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var bills by remember { mutableStateOf<List<ParsedBill>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var loadingText by remember { mutableStateOf("") }
    var editIndex by remember { mutableIntStateOf(-1) }
    var showSuccess by remember { mutableStateOf(false) }

    // Batch image import state
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var checkedIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var previewUriIndex by remember { mutableIntStateOf(-1) }
    var showGallery by remember { mutableStateOf(false) }

    // CSV file picker
    val csvPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        isLoading = true
        loadingText = "正在解析 CSV 文件..."
        scope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val content = reader.readText()
                reader.close()
                val parsed = withContext(Dispatchers.Default) { BillImporter.parseCsv(content) }
                if (parsed.isEmpty()) {
                    snackbarHostState.showSnackbar("未能识别出有效账单数据，请检查CSV格式")
                } else {
                    bills = parsed
                    snackbarHostState.showSnackbar("成功识别 ${parsed.size} 条账单")
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("解析失败：${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    // Image picker for OCR (multi-select)
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        selectedImageUris = uris
        checkedIndices = uris.indices.toSet()
        showGallery = true
        bills = emptyList()
        showSuccess = false
    }

    // OCR processing function
    suspend fun runOcrOnImage(uri: Uri): String? {
        return withContext(Dispatchers.Default) {
            val image = InputImage.fromFilePath(context, uri)
            val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
            try {
                val latch = java.util.concurrent.CountDownLatch(1)
                var mlResult: com.google.mlkit.vision.text.Text? = null
                var mlError: Exception? = null
                recognizer.process(image)
                    .addOnSuccessListener { mlResult = it; latch.countDown() }
                    .addOnFailureListener { mlError = it; latch.countDown() }
                latch.await()
                if (mlError != null) throw mlError!!
                mlResult!!.text
            } finally {
                recognizer.close()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("导入账单", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (bills.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Button(
                        onClick = {
                            onImport(bills)
                            showSuccess = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("确认导入（${bills.size} 条）", fontSize = 16.sp)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // 导入按钮
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                // CSV 导入
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = !isLoading) { csvPicker.launch(arrayOf("text/*", "*/*")) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("从 CSV 导入", fontSize = 14.sp, fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text("选择 CSV 文件", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                }

                // 图片导入
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = !isLoading) { imagePicker.launch("image/*") },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("从图片导入", fontSize = 14.sp, fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text("批量识别支付截图", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Batch Image Gallery ──
            if (showGallery && selectedImageUris.isNotEmpty() && !showSuccess) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "已选择 ${selectedImageUris.size} 张图片",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = {
                        selectedImageUris = emptyList()
                        checkedIndices = emptySet()
                        showGallery = false
                    }) {
                        Text("清空", fontSize = 13.sp, color = Color(0xFFEA6B5C))
                    }
                }
                Spacer(Modifier.height(8.dp))

                // Thumbnail grid (3 columns)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedImageUris.size) { index ->
                        val uri = selectedImageUris[index]
                        ImageThumbnailCard(
                            uri = uri,
                            checked = index in checkedIndices,
                            onToggleCheck = {
                                checkedIndices = if (index in checkedIndices) {
                                    checkedIndices - index
                                } else {
                                    checkedIndices + index
                                }
                            },
                            onTap = { previewUriIndex = index }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Batch OCR trigger button
                Button(
                    onClick = {
                        isLoading = true
                        loadingText = "正在识别..."
                        scope.launch {
                            try {
                                val allBills = mutableListOf<ParsedBill>()
                                val urisToProcess = selectedImageUris
                                    .filterIndexed { idx, _ -> idx in checkedIndices }

                                urisToProcess.forEachIndexed { i, uri ->
                                    loadingText = "正在识别图片 (${i + 1}/${urisToProcess.size})..."
                                    val rawText = runOcrOnImage(uri)
                                    if (!rawText.isNullOrBlank()) {
                                        val parsed = withContext(Dispatchers.Default) {
                                            BillImporter.parseOcrText(rawText)
                                        }
                                        allBills.addAll(parsed)
                                    }
                                }

                                if (allBills.isEmpty()) {
                                    snackbarHostState.showSnackbar("未能从所选图片中识别出账单")
                                } else {
                                    bills = allBills
                                    showGallery = false
                                    snackbarHostState.showSnackbar("成功识别 ${allBills.size} 条账单")
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("识别失败：${e.message}")
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = checkedIndices.isNotEmpty() && !isLoading,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (checkedIndices.size == selectedImageUris.size) "识别全部图片"
                               else "识别选中图片（${checkedIndices.size}张）",
                        fontSize = 16.sp
                    )
                }

                Spacer(Modifier.height(16.dp))
            }

            // 加载中
            if (isLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(loadingText, fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // 成功提示
            if (showSuccess) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("✅", fontSize = 40.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("已成功导入 ${bills.size} 条账单！",
                            fontSize = 16.sp, fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32))
                        Spacer(Modifier.height(4.dp))
                        Text("点击返回查看全部记录", fontSize = 13.sp,
                            color = Color(0xFF558B2F))
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onBack,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text("返回", color = Color.White)
                        }
                    }
                }
            }

            // 预览列表
            if (bills.isNotEmpty() && !showSuccess) {
                Text(
                    text = "预览（共 ${bills.size} 条）",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(bills) { index, bill ->
                        ParsedBillCard(
                            bill = bill,
                            index = index,
                            onClick = { editIndex = index }
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }

            // 初始提示
            if (bills.isEmpty() && !isLoading && !showSuccess && !showGallery) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.FileUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "选择 CSV 文件或支付截图开始导入",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }

    // 编辑弹窗
    if (editIndex in bills.indices) {
        EditBillDialog(
            bill = bills[editIndex],
            onDismiss = { editIndex = -1 },
            onSave = { updated ->
                bills = bills.toMutableList().also { it[editIndex] = updated }
                editIndex = -1
            }
        )
    }

    // Full-screen image preview
    if (previewUriIndex in selectedImageUris.indices) {
        FullScreenImagePreview(
            uri = selectedImageUris[previewUriIndex],
            isChecked = previewUriIndex in checkedIndices,
            onSelect = {
                checkedIndices = checkedIndices + previewUriIndex
                previewUriIndex = -1
            },
            onCancel = {
                previewUriIndex = -1
            },
            onDismiss = {
                previewUriIndex = -1
            }
        )
    }
}

@Composable
private fun ParsedBillCard(
    bill: ParsedBill,
    index: Int,
    onClick: () -> Unit
) {
    val dateFmt = remember { SimpleDateFormat("MM/dd", Locale.getDefault()) }
    val isExpense = bill.type == TransactionType.EXPENSE
    val amountColor = if (isExpense) Color(0xFFEA6B5C) else Color(0xFF4CAF7A)
    val sign = if (isExpense) "-" else "+"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 序号
            Text(
                text = "${index + 1}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.width(24.dp)
            )
            // 日期
            Column(modifier = Modifier.width(56.dp)) {
                Text(
                    text = dateFmt.format(Date(bill.dateMillis)),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // 分类 + 描述 + 支付方式
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = bill.category,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(6.dp))
                    val sourceLabel = when (bill.source) {
                        TransactionSource.WECHAT -> "微信"
                        TransactionSource.ALIPAY -> "支付宝"
                        TransactionSource.BANK -> "银行"
                        TransactionSource.MANUAL -> ""
                    }
                    if (sourceLabel.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = sourceLabel,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                if (bill.description.isNotBlank()) {
                    Text(
                        text = bill.description,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 1
                    )
                }
            }
            // 金额
            Text(
                text = "$sign¥${String.format("%.2f", bill.amount)}",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = amountColor
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Default.Edit,
                contentDescription = "编辑",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun EditBillDialog(
    bill: ParsedBill,
    onDismiss: () -> Unit,
    onSave: (ParsedBill) -> Unit
) {
    var amount by remember { mutableStateOf(bill.amount.toString()) }
    var isExpense by remember { mutableStateOf(bill.type == TransactionType.EXPENSE) }
    var category by remember { mutableStateOf(bill.category) }
    var description by remember { mutableStateOf(bill.description) }

    val categories = listOf("餐饮", "交通", "购物", "生活缴费", "娱乐", "医疗", "转账", "工资", "其他")

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("编辑账单", fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // 金额
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("金额") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                // 收支类型
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TagChip(
                        selected = isExpense,
                        onClick = { isExpense = true },
                        label = "支出"
                    )
                    TagChip(
                        selected = !isExpense,
                        onClick = { isExpense = false },
                        label = "收入"
                    )
                }
                Spacer(Modifier.height(12.dp))
                // 分类
                Text("分类", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { cat ->
                        TagChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = cat
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                // 备注
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("备注") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: return@Button
                    onSave(bill.copy(
                        amount = amt,
                        type = if (isExpense) TransactionType.EXPENSE else TransactionType.INCOME,
                        category = category,
                        description = description
                    ))
                },
                shape = RoundedCornerShape(10.dp)
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

// ── Circular checkbox for thumbnail selection ──
@Composable
private fun CircularCheckbox(
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (checked) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f),
        border = if (!checked) BorderStroke(2.dp, Color.White) else null,
        modifier = modifier.size(24.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (checked) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "已选择",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ── Thumbnail card with image and checkbox ──
@Composable
private fun ImageThumbnailCard(
    uri: Uri,
    checked: Boolean,
    onToggleCheck: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap = remember(uri) {
        try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            options.inSampleSize = maxOf(
                options.outWidth / 300,
                options.outHeight / 300,
                1
            )
            options.inJustDecodeBounds = false
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        } catch (_: Exception) { null }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onTap),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "预览",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.BrokenImage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            CircularCheckbox(
                checked = checked,
                onClick = onToggleCheck,
                modifier = Modifier.padding(6.dp)
            )
        }
    }
}

// ── Full-screen image preview ──
@Composable
private fun FullScreenImagePreview(
    uri: Uri,
    isChecked: Boolean,
    onSelect: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val bitmap = remember(uri) {
        try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            options.inSampleSize = maxOf(
                options.outWidth / 1200,
                options.outHeight / 1200,
                1
            )
            options.inJustDecodeBounds = false
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        } catch (_: Exception) { null }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(onClick = onDismiss)
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "全屏预览",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Bottom bar
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                ) {
                    Text("取消", color = Color.White)
                }
                Button(
                    onClick = onSelect,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isChecked) "已选择" else "选择")
                }
            }

            // Top-right close indicator
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
