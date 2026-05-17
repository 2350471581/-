package com.example.billtracker.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billtracker.data.BillImporter
import com.example.billtracker.data.ParsedBill
import com.example.billtracker.data.TransactionType
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

    // Image picker for OCR
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        isLoading = true
        loadingText = "正在识别图片中的文字..."
        scope.launch {
            try {
                val image = InputImage.fromFilePath(context, uri)
                val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
                val ocrResult = withContext(Dispatchers.Default) {
                    val latch = java.util.concurrent.CountDownLatch(1)
                    var mlResult: com.google.mlkit.vision.text.Text? = null
                    var mlError: Exception? = null
                    recognizer.process(image)
                        .addOnSuccessListener { mlResult = it; latch.countDown() }
                        .addOnFailureListener { mlError = it; latch.countDown() }
                    latch.await()
                    if (mlError != null) throw mlError!!
                    mlResult!!
                }
                recognizer.close()
                val rawText = ocrResult.text
                if (rawText.isBlank()) {
                    snackbarHostState.showSnackbar("未能识别出图片中的文字")
                    isLoading = false
                    return@launch
                }
                val parsed = withContext(Dispatchers.Default) { BillImporter.parseOcrText(rawText) }
                if (parsed.isEmpty()) {
                    snackbarHostState.showSnackbar("未能从图片中识别出账单金额")
                } else {
                    bills = parsed
                    snackbarHostState.showSnackbar("成功识别 ${parsed.size} 条账单")
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("识别失败：${e.message}")
            } finally {
                isLoading = false
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
                        Text("识别支付截图", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

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
            if (bills.isEmpty() && !isLoading && !showSuccess) {
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
            // 分类 + 描述
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bill.category,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
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
                    FilterChip(
                        selected = isExpense,
                        onClick = { isExpense = true },
                        label = { Text("支出") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFEA6B5C).copy(alpha = 0.15f),
                            selectedLabelColor = Color(0xFFEA6B5C)
                        )
                    )
                    FilterChip(
                        selected = !isExpense,
                        onClick = { isExpense = false },
                        label = { Text("收入") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF4CAF7A).copy(alpha = 0.15f),
                            selectedLabelColor = Color(0xFF4CAF7A)
                        )
                    )
                }
                Spacer(Modifier.height(12.dp))
                // 分类
                Text("分类", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 12.sp) }
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
