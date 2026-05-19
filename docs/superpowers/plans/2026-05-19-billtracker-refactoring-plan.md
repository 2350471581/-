# BillTracker 架构重构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 按优先级重构 BillTracker 代码：统一主题色 → 抽取公共组件 → 拆分 ViewModel → 引入 Compose Navigation

**Architecture:** 4 步渐进式重构，每步可独立提交和回退。第一步纯替换不改逻辑，第二步抽取不破坏接口，第三步拆分不影响 UI，第四步重写导航。

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Hilt, Room, Compose Navigation

**当前状态:** 存在大量未提交的 WIP 代码（含新增的 ImportScreen/AIChatScreen/AboutScreen/SearchScreen），计划基于当前工作目录的最新状态增量修改。

---

### Task 0: 添加 Compose Navigation 依赖

**Files:**
- Modify: `app/build.gradle.kts`（在 dependencies 块末尾添加）

- [ ] **Step 1: 添加 navigation-compose 依赖**

```kotlin
// 在 app/build.gradle.kts 的 dependencies 块末尾添加
implementation("androidx.navigation:navigation-compose:2.7.7")
```

- [ ] **Step 2: 提交**

```bash
git add app/build.gradle.kts
git commit -m "chore: add navigation-compose dependency"
```

---

### Task 1: 统一主题色常量 + 添加 CardBg 函数

**Files:**
- Modify: `app/src/main/java/com/example/billtracker/ui/Theme.kt`
- Modify: `app/src/main/java/com/example/billtracker/ui/ThemeManager.kt`
- Modify: `app/src/main/java/com/example/billtracker/ui/MainScreen.kt`
- Modify: `app/src/main/java/com/example/billtracker/ui/ProfileScreen.kt`
- Modify: `app/src/main/java/com/example/billtracker/ui/AnalysisScreen.kt`
- Modify: `app/src/main/java/com/example/billtracker/ui/PlanScreen.kt`
- Modify: `app/src/main/java/com/example/billtracker/ui/SearchScreen.kt`
- Modify: `app/src/main/java/com/example/billtracker/ui/AIChatScreen.kt`
- Modify: `app/src/main/java/com/example/billtracker/ui/ImportScreen.kt`
- Modify: `app/src/main/java/com/example/billtracker/ui/AboutScreen.kt`

- [ ] **Step 1: 在 Theme.kt 中添加语义化颜色常量**

在 Theme.kt 的 `IncomeGreen` / `ExpenseRed` 等常量之后添加：

```kotlin
// ── 语义化颜色常量 ──
val SubtleText = Color(0xFF9AA0A6)        // 次要文字/图标
val DividerColor = Color(0xFFF1F3F4)      // 分隔线背景
val MutedIconColor = Color(0xFFBDBDBD)    // 静默图标
val DarkSubtleText = Color(0xFF5F6368)    // 深色次要文字
val FrostedWhite = Color.White.copy(alpha = 0.88f)   // 毛玻璃白
val FrostedDark = Color(0xFF2A2A2A).copy(alpha = 0.88f) // 毛玻璃暗
```

- [ ] **Step 2: 在 ThemeManager.kt 中添加统一的 CardBg composable**

在 `CustomThemeConfig` 后或文件末尾添加：

```kotlin
@Composable
fun CardBg(): Color {
    return if (isSystemInDarkTheme()) FrostedDark else FrostedWhite
}
```

- [ ] **Step 3: 全局替换硬编码颜色 — MainScreen.kt**

替换以下模式（查找替换，每项只改 MainScreen.kt）：

| 旧值 | 新值 |
|------|------|
| `Color(0xFF9AA0A6)` | `SubtleText` |
| `Color(0xFFF1F3F4)` | `DividerColor` |
| `Color(0xFF5F6368)` | `DarkSubtleText` |
| `Color(0xFFBDBDBD)` | `MutedIconColor` |
| `if (isSystemInDarkTheme()) Color(0xFF2A2A2A).copy(alpha = 0.88f) else Color.White.copy(alpha = 0.88f)` | `CardBg()` |

```kotlin
// MainScreen.kt — 修改 CardBg() 函数
// 之前
fun CardBg() = if (isSystemInDarkTheme()) Color(0xFF2A2A2A).copy(alpha = 0.88f)
    else Color.White.copy(alpha = 0.88f)

// 之后
@Composable
fun CardBg() = com.example.billtracker.ui.CardBg()
```

- [ ] **Step 4: 全局替换 — ProfileScreen.kt**

同样的替换模式：`Color(0xFF9AA0A6)` → `SubtleText`，`Color(0xFFBDBDBD)` → `MutedIconColor`，删除本地的 `frostedCardColor` 计算，改为引用公共 `CardBg()`。

- [ ] **Step 5: 全局替换 — AnalysisScreen.kt**

同样的替换模式，删除本地 `frostedCardColor` val，改用 `CardBg()`。

- [ ] **Step 6: 全局替换 — PlanScreen.kt**

同样的替换模式，删除各处的 `frostedCardColor`，统一用 `CardBg()`。

- [ ] **Step 7: 全局替换 — SearchScreen.kt、AIChatScreen.kt、ImportScreen.kt、AboutScreen.kt**

同样的替换模式。

- [ ] **Step 8: 提交**

```bash
git add app/src/main/java/com/example/billtracker/ui/
git commit -m "refactor(theme): unify hardcoded colors into semantic constants, add global CardBg()"
```

---

### Task 2: 抽取公共 UI 组件

**Files:**
- Create: `app/src/main/java/com/example/billtracker/ui/components/TransactionCard.kt`
- Create: `app/src/main/java/com/example/billtracker/ui/components/SourceLabel.kt`
- Create: `app/src/main/java/com/example/billtracker/ui/components/CountdownButton.kt`
- Modify: `app/src/main/java/com/example/billtracker/ui/MainScreen.kt`
- Modify: `app/src/main/java/com/example/billtracker/ui/SearchScreen.kt`
- Modify: `app/src/main/java/com/example/billtracker/ui/AnalysisScreen.kt`
- Modify: `app/src/main/java/com/example/billtracker/ui/PlanScreen.kt`

- [ ] **Step 1: 创建 SourceLabel.kt**

```kotlin
package com.example.billtracker.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billtracker.data.TransactionSource
import com.example.billtracker.ui.AlipayBlue
import com.example.billtracker.ui.SubtleText
import com.example.billtracker.ui.WechatGreen

fun sourceDisplayName(source: TransactionSource): String = when (source) {
    TransactionSource.WECHAT -> "微信"
    TransactionSource.ALIPAY -> "支付宝"
    TransactionSource.BANK -> "银行"
    TransactionSource.MANUAL -> "其他"
}

fun sourceColor(source: TransactionSource): Color = when (source) {
    TransactionSource.WECHAT -> WechatGreen
    TransactionSource.ALIPAY -> AlipayBlue
    TransactionSource.BANK -> Color(0xFFE65100)
    TransactionSource.MANUAL -> SubtleText
}

@Composable
fun SourceLabel(
    source: TransactionSource,
    modifier: Modifier = Modifier
) {
    val color = sourceColor(source)
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.12f),
        modifier = modifier
    ) {
        Text(
            text = sourceDisplayName(source),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}
```

- [ ] **Step 2: 创建 TransactionCard.kt**

```kotlin
package com.example.billtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billtracker.data.TransactionEntity
import com.example.billtracker.data.TransactionSource
import com.example.billtracker.data.TransactionType
import com.example.billtracker.data.CategoryManager
import com.example.billtracker.ui.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * 统一的交易条目卡片，支持紧凑/详情模式。
 * @param compactMode 为 true 时不显示分类、备注标记
 */
@Composable
fun TransactionCard(
    transaction: TransactionEntity,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    compactMode: Boolean = false
) {
    val dateStr = remember(transaction.dateMillis) {
        val sdf = SimpleDateFormat("yy/MM/dd HH:mm", Locale.getDefault())
        sdf.format(Date(transaction.dateMillis))
    }
    val displayDesc = transaction.description
        .replace(Regex("""【[^】]*】"""), "")
        .trim()
        .take(if (compactMode) 20 else 30)

    val amountColor = when (transaction.type) {
        TransactionType.INCOME -> IncomeGreen
        TransactionType.EXPENSE -> ExpenseRed
    }
    val typePrefix = when (transaction.type) {
        TransactionType.INCOME -> "+"
        TransactionType.EXPENSE -> "-"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compactMode) 56.dp else 64.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧色条
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(if (compactMode) 56.dp else 64.dp)
                    .background(amountColor, RoundedCornerShape(topEnd = 0.dp, bottomEnd = 0.dp))
            )
            Spacer(Modifier.width(14.dp))

            // 来源标签
            SourceLabel(source = transaction.source)

            // 非紧凑模式显示分类
            if (!compactMode && transaction.category != "其他") {
                Spacer(Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "${CategoryManager.getCategoryIcon(transaction.category)} ${transaction.category}",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // 描述 + 时间
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayDesc.ifEmpty { defaultDescription(transaction.source) },
                    fontSize = if (compactMode) 13.sp else 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!compactMode) {
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = dateStr, fontSize = 11.sp, color = SubtleText)
                        if (transaction.description.contains("\n--备注--\n")) {
                            Spacer(Modifier.width(4.dp))
                            Text("注", fontSize = 9.sp, color = MutedIconColor, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // 金额
            Text(
                text = "$typePrefix¥%.2f".format(transaction.amount),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 14.dp),
                color = amountColor
            )
        }
    }
}

private fun defaultDescription(source: TransactionSource): String = when (source) {
    TransactionSource.MANUAL -> "手动记账"
    TransactionSource.BANK -> "银行账单"
    else -> "账单"
}
```

- [ ] **Step 3: 创建 CountdownButton.kt**

```kotlin
package com.example.billtracker.ui.components

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import kotlinx.coroutines.delay

@Composable
fun CountdownButton(
    countdown: Int,
    enabled: Boolean = countdown <= 0,
    onClick: () -> Unit,
    text: String = "知道了"
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Text(
            text = if (enabled) text else "($countdown) $text",
            color = Color.White,
            fontWeight = if (!enabled) FontWeight.Normal else FontWeight.SemiBold
        )
    }
}
```

- [ ] **Step 4: 替换 MainScreen.kt 中的交易条目**

将 `TransactionItem` composable 中的卡片内容替换为 `TransactionCard`，保留左滑交互外层。

```kotlin
// 在 TransactionItem 的 Card 部分，替换内部 Row 为：
TransactionCard(
    transaction = transaction,
    onClick = onItemClick,
    modifier = Modifier.clickable { onItemClick() }
)
```

- [ ] **Step 5: 替换 SearchScreen.kt 中的 SearchResultCard**

将 `SearchResultCard` 整个替换为：

```kotlin
@Composable
private fun SearchResultCard(transaction: TransactionEntity) {
    TransactionCard(transaction = transaction, compactMode = false)
}
```

- [ ] **Step 6: 替换 AnalysisScreen.kt 中的 AnalysisTransactionRow**

将 `AnalysisTransactionRow` 整个替换为对 `TransactionCard(transaction = tx, compactMode = true)` 的调用（紧凑模式）。

- [ ] **Step 7: 合并 PlanDetailDialog 去重**

在 PlanScreen.kt 中，删除 `SavePlanDetailDialog` composable，修改所有调用 `SavePlanDetailDialog` 的地方为 `PlanDetailDialog(title = "省钱计划", ...)`。`SavePlanDetailDialog` 和 `PlanDetailDialog` 代码几乎一样，唯一区别是标题和进度文案，用 `PlanDetailDialog` 的参数即可覆盖。

- [ ] **Step 8: 提交**

```bash
git add app/src/main/java/com/example/billtracker/ui/components/TransactionCard.kt app/src/main/java/com/example/billtracker/ui/components/SourceLabel.kt app/src/main/java/com/example/billtracker/ui/components/CountdownButton.kt app/src/main/java/com/example/billtracker/ui/MainScreen.kt app/src/main/java/com/example/billtracker/ui/SearchScreen.kt app/src/main/java/com/example/billtracker/ui/AnalysisScreen.kt app/src/main/java/com/example/billtracker/ui/PlanScreen.kt
git commit -m "refactor(ui): extract common components (TransactionCard, SourceLabel, CountdownButton), deduplicate PlanDetailDialog"
```

---

### Task 3: 拆分 ViewModel

**Files:**
- Create: `viewmodel/LedgerViewModel.kt`
- Create: `viewmodel/PlanViewModel.kt`
- Create: `viewmodel/AnalysisViewModel.kt`
- Create: `viewmodel/ProfileViewModel.kt`
- Delete: `viewmodel/MainViewModel.kt`
- Modify: `ui/MainScreen.kt`
- Modify: `ui/PlanScreen.kt`
- Modify: `ui/AnalysisScreen.kt`
- Modify: `ui/ProfileScreen.kt`
- Modify: `ui/AIChatScreen.kt`
- Modify: `MainActivity.kt`

- [ ] **Step 1: 创建 LedgerViewModel.kt**

从 `MainViewModel` 中提取交易相关逻辑：

```kotlin
package com.example.billtracker.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.Manifest
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.billtracker.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LedgerViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val planStorage: PlanStorage,
    application: Application
) : AndroidViewModel(application) {

    private val todayStart: Long
        get() {
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }
    private val todayEnd: Long get() = todayStart + 86400000L

    val todayTransactions: StateFlow<List<TransactionEntity>> =
        repository.getTodayTransactions(todayStart, todayEnd)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayIncome: StateFlow<Double> =
        repository.getTodayIncome(todayStart, todayEnd)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayExpense: StateFlow<Double> =
        repository.getTodayExpense(todayStart, todayEnd)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val allTransactions: StateFlow<List<TransactionEntity>> =
        repository.getAllTransactions()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isRefreshing = MutableStateFlow(false)

    val installDateMillis = planStorage.installDateMillis

    val filterStartDate = MutableStateFlow<Long?>(null)
    val filterEndDate = MutableStateFlow<Long?>(null)

    val recentTransactions: StateFlow<List<TransactionEntity>> =
        combine(allTransactions, filterStartDate, filterEndDate) { list, start, end ->
            list.filter { it.dateMillis >= installDateMillis }.let { filtered ->
                val s = start ?: (System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)
                val e = end ?: Long.MAX_VALUE
                filtered.filter { it.dateMillis in s..e }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 通知监听权限
    val hasNotificationPermission = MutableStateFlow(
        NotificationListener.isPermissionGranted(application)
    )

    fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(intent)
    }

    fun checkNotificationPermission() {
        hasNotificationPermission.value = NotificationListener.isPermissionGranted(getApplication())
    }

    // 手动/自动模式
    val isManualMode = MutableStateFlow(planStorage.isManualMode)

    fun setManualMode(enabled: Boolean) {
        isManualMode.value = enabled
        planStorage.isManualMode = enabled
    }

    fun addTransaction(amount: Double, type: TransactionType, description: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = repository.addManualTransaction(amount, type, description)
            onResult(success)
        }
    }

    fun importBills(bills: List<ParsedBill>, onDuplicateCount: (Int) -> Unit = {}) {
        viewModelScope.launch {
            var duplicates = 0
            bills.forEach {
                if (!repository.importTransaction(it)) duplicates++
            }
            onDuplicateCount(duplicates)
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch { repository.deleteTransaction(id) }
    }

    fun updateTransactionNote(id: Long, note: String) {
        viewModelScope.launch { repository.updateTransactionDescription(id, note) }
    }

    fun refreshFromSms() {
        viewModelScope.launch {
            isRefreshing.value = true
            repository.importFromSms(getApplication())
            isRefreshing.value = false
        }
    }
}
```

- [ ] **Step 2: 创建 PlanViewModel.kt**

```kotlin
package com.example.billtracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.billtracker.data.CustomPlan
import com.example.billtracker.data.PlanDataType
import com.example.billtracker.data.PlanStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlanViewModel @Inject constructor(
    private val planStorage: PlanStorage
) : ViewModel() {

    val planBalance = MutableStateFlow(planStorage.balance)
    val todayPlanTarget = MutableStateFlow(planStorage.todayPlanTarget)
    val totalPlanTarget = MutableStateFlow(planStorage.totalPlanTarget)
    val savePlanTarget = MutableStateFlow(planStorage.savePlanTarget)
    val todayPlanNote = MutableStateFlow(planStorage.todayPlanNote)
    val totalPlanNote = MutableStateFlow(planStorage.totalPlanNote)
    val savePlanNote = MutableStateFlow(planStorage.savePlanNote)
    val customPlans = MutableStateFlow(planStorage.getAllCustomPlans())

    fun updatePlanBalance(balance: Double) {
        planBalance.value = balance
        planStorage.balance = balance
    }

    fun updateTodayPlanTarget(target: Double) {
        todayPlanTarget.value = target
        planStorage.todayPlanTarget = target
    }

    fun updateTotalPlanTarget(target: Double) {
        totalPlanTarget.value = target
        planStorage.totalPlanTarget = target
    }

    fun updateSavePlanTarget(target: Double) {
        savePlanTarget.value = target
        planStorage.savePlanTarget = target
    }

    fun updateTodayPlanNote(note: String) {
        todayPlanNote.value = note
        planStorage.todayPlanNote = note
    }

    fun updateTotalPlanNote(note: String) {
        totalPlanNote.value = note
        planStorage.totalPlanNote = note
    }

    fun updateSavePlanNote(note: String) {
        savePlanNote.value = note
        planStorage.savePlanNote = note
    }

    fun addCustomPlan(name: String, target: Double, note: String, type: PlanDataType = PlanDataType.TODAY_NET) {
        val plan = CustomPlan(name, target, note, type)
        planStorage.addCustomPlan(plan)
        customPlans.value = planStorage.getAllCustomPlans()
    }

    fun updateCustomPlan(index: Int, target: Double, note: String) {
        planStorage.updateCustomPlan(index, target, note)
        customPlans.value = planStorage.getAllCustomPlans()
    }

    fun deleteCustomPlan(index: Int) {
        planStorage.deleteCustomPlan(index)
        customPlans.value = planStorage.getAllCustomPlans()
    }
}
```

- [ ] **Step 3: 创建 AnalysisViewModel.kt**

```kotlin
package com.example.billtracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.billtracker.data.DeepSeekService
import com.example.billtracker.data.TransactionDao
import com.example.billtracker.data.TransactionEntity
import com.example.billtracker.data.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val dao: TransactionDao,
    private val deepSeekService: DeepSeekService,
    application: Application
) : AndroidViewModel(application) {

    data class AnalysisPeriod(val label: String, val startMillis: Long)

    fun getAnalysisPeriods(): List<AnalysisPeriod> {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        return listOf(
            AnalysisPeriod("今天", run {
                cal.timeInMillis = now
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }),
            AnalysisPeriod("昨天", run {
                cal.timeInMillis = now - 86400000L
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }),
            AnalysisPeriod("一周", run { now - 7L * 86400000L }),
            AnalysisPeriod("一个月", run { now - 30L * 86400000L }),
            AnalysisPeriod("三个月", run { now - 90L * 86400000L }),
            AnalysisPeriod("半年", run { now - 180L * 86400000L }),
        )
    }

    suspend fun getAnalysisTransactions(startMillis: Long): List<TransactionEntity> {
        return withContext(Dispatchers.IO) {
            dao.getTransactionsBetweenSync(startMillis, System.currentTimeMillis())
        }
    }

    suspend fun generateMonthlySummary(
        year: Int, month: Int, totalIncome: Double, totalExpense: Double,
        netBalance: Double, expenseCategories: Map<String, Double>,
        transactionCount: Int, nickname: String = ""
    ): String = deepSeekService.generateMonthlySummary(
        year, month, totalIncome, totalExpense, netBalance,
        expenseCategories, transactionCount, nickname
    )

    suspend fun getTodaySummary(): String = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        val end = start + 86400000L

        val tx = dao.getTransactionsBetweenSync(start, end)
        val income = tx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = tx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val count = tx.size

        buildString {
            appendLine("今日账单概况（截至${"HH:mm".let { java.text.SimpleDateFormat(it, java.util.Locale.getDefault()).format(java.util.Date()) }}）：")
            appendLine("收入：¥${"%.2f".format(income)}")
            appendLine("支出：¥${"%.2f".format(expense)}")
            appendLine("交易笔数：$count")
            if (tx.isNotEmpty()) {
                append("最近几笔：")
                tx.take(5).forEachIndexed { i, t ->
                    val type = if (t.type == TransactionType.INCOME) "收入" else "支出"
                    append("${if (i > 0) "；" else ""}${t.description} ${type}¥${"%.2f".format(t.amount)}")
                }
            }
        }
    }
}
```

- [ ] **Step 4: 创建 ProfileViewModel.kt**

```kotlin
package com.example.billtracker.viewmodel

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.billtracker.data.*
import com.example.billtracker.ui.CustomThemeConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val planStorage: PlanStorage,
    private val dao: TransactionDao,
    application: Application
) : AndroidViewModel(application) {

    // 主题
    val themeIndex = MutableStateFlow(planStorage.themeIndex)
    val followSystemTheme = MutableStateFlow(planStorage.followSystemTheme)
    val customThemeConfig = MutableStateFlow(
        CustomThemeConfig.fromJson(planStorage.customThemeConfigJson)
    )

    // 昵称/头像
    val nickname = MutableStateFlow(planStorage.nickname)
    val avatarEmoji = MutableStateFlow(planStorage.avatarEmoji)
    val customAvatarUri = MutableStateFlow(planStorage.customAvatarUri)

    // AI
    val aiChatEnabled = MutableStateFlow(planStorage.aiChatEnabled)
    val aiChatTutorialDone = MutableStateFlow(planStorage.aiChatTutorialDone)
    val isFirstLaunch = MutableStateFlow(planStorage.isFirstLaunch)

    fun setThemeIndex(index: Int) {
        themeIndex.value = index
        planStorage.themeIndex = index
    }

    fun setFollowSystemTheme(enabled: Boolean) {
        followSystemTheme.value = enabled
        planStorage.followSystemTheme = enabled
    }

    fun setCustomThemeConfig(config: CustomThemeConfig) {
        customThemeConfig.value = config
        planStorage.customThemeConfigJson = config.toJson()
    }

    fun setNickname(name: String) {
        nickname.value = name
        planStorage.nickname = name
    }

    fun setAvatarEmoji(index: Int) {
        avatarEmoji.value = index
        planStorage.avatarEmoji = index
    }

    fun setCustomAvatarUri(uri: String) {
        customAvatarUri.value = uri
        planStorage.customAvatarUri = uri
    }

    fun setAiChatEnabled(enabled: Boolean) {
        aiChatEnabled.value = enabled
        planStorage.aiChatEnabled = enabled
    }

    fun markAiChatTutorialDone() {
        aiChatTutorialDone.value = true
        planStorage.aiChatTutorialDone = true
    }

    fun dismissFirstLaunch() {
        planStorage.markFirstLaunchSeen()
        isFirstLaunch.value = false
    }

    fun clearAllData() {
        viewModelScope.launch { dao.clearAll() }
    }

    // 导出 CSV
    suspend fun exportCsv(startMillis: Long = 0, endMillis: Long = System.currentTimeMillis()): Uri? = withContext(Dispatchers.IO) {
        try {
            val allTx = dao.getTransactionsBetweenSync(startMillis, endMillis)
            val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val csv = buildString {
                appendLine("日期,金额,类型,来源,分类,备注")
                allTx.forEach { tx ->
                    val type = if (tx.type == TransactionType.INCOME) "收入" else "支出"
                    val source = when (tx.source) {
                        TransactionSource.WECHAT -> "微信"
                        TransactionSource.ALIPAY -> "支付宝"
                        TransactionSource.MANUAL -> "手动"
                        TransactionSource.BANK -> "银行"
                    }
                    appendLine("${dateFmt.format(tx.dateMillis)},${tx.amount},$type,$source,${tx.category},${tx.description.replace(",", "，")}")
                }
            }
            val file = File(getApplication<Application>().cacheDir, "billtracker_export.csv")
            file.writeText(csv)
            FileProvider.getUriForFile(getApplication(), "${getApplication<Application>().packageName}.fileprovider", file)
        } catch (e: Exception) { null }
    }

    // 导出图片
    suspend fun exportImage(startMillis: Long = 0, endMillis: Long = System.currentTimeMillis()): Uri? = withContext(Dispatchers.IO) {
        // 保持与 MainViewModel.exportImage 相同的实现
        try {
            val allTx = dao.getTransactionsBetweenSync(startMillis, endMillis)
            val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val width = 1080
            val headerHeight = 160
            val rowHeight = 44
            val padding = 60
            val titleHeight = 100
            val summaryHeight = 120
            val totalRows = allTx.size.coerceAtLeast(1)
            val height = padding + titleHeight + summaryHeight + headerHeight + totalRows * rowHeight + padding
            val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            // ... 完整绘制逻辑与 MainViewModel.exportImage 相同
            val bgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
            val titlePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#2C241A"); textSize = 48f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            val headerPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#9AA0A6"); textSize = 26f
            }
            val rowPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#3C4043"); textSize = 30f
            }
            val incomePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#4CAF7A"); textSize = 30f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            val expensePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#EA6B5C"); textSize = 30f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            val linePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#F1F3F4"); strokeWidth = 1f
            }
            val grayPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#9AA0A6"); textSize = 24f
            }
            var y = padding
            canvas.drawText("账单记录", 60f, y + 50f, titlePaint)
            y += titleHeight
            val totalIncome = allTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val totalExpense = allTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            canvas.drawText("收入: ¥${"%.2f".format(totalIncome)}", 60f, y.toFloat(), incomePaint)
            canvas.drawText("支出: ¥${"%.2f".format(totalExpense)}", 400f, y.toFloat(), expensePaint)
            y += summaryHeight
            canvas.drawRect(60f, y.toFloat(), (width - 60).toFloat(), (y + headerHeight).toFloat(), android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#FFF8F0")
            })
            canvas.drawText("日期", 80f, y + 36f, headerPaint)
            canvas.drawText("金额", 340f, y + 36f, headerPaint)
            canvas.drawText("分类", 540f, y + 36f, headerPaint)
            canvas.drawText("来源", 720f, y + 36f, headerPaint)
            canvas.drawText("备注", 860f, y + 36f, headerPaint)
            y += headerHeight
            val displayTx = allTx.take(100)
            displayTx.forEachIndexed { i, tx ->
                if (i % 2 == 1) {
                    canvas.drawRect(60f, y.toFloat(), (width - 60).toFloat(), (y + rowHeight).toFloat(), android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#F8F9FA")
                    })
                }
                val amountPaint = if (tx.type == TransactionType.INCOME) incomePaint else expensePaint
                val sign = if (tx.type == TransactionType.INCOME) "+" else "-"
                canvas.drawText(dateFmt.format(tx.dateMillis), 80f, y + 30f, grayPaint)
                canvas.drawText("${sign}¥${"%.2f".format(tx.amount)}", 340f, y + 30f, amountPaint)
                canvas.drawText(tx.category, 540f, y + 30f, rowPaint)
                val srcText = when (tx.source) {
                    TransactionSource.WECHAT -> "微信"; TransactionSource.ALIPAY -> "支付宝"
                    TransactionSource.MANUAL -> "手动"; TransactionSource.BANK -> "银行"
                }
                canvas.drawText(srcText, 720f, y + 30f, grayPaint)
                canvas.drawText(tx.description.take(8), 860f, y + 30f, rowPaint)
                y += rowHeight
                canvas.drawLine(60f, y.toFloat(), (width - 60).toFloat(), y.toFloat(), linePaint)
            }
            if (allTx.size > 50) canvas.drawText("... 共${allTx.size}条记录", 60f, y + 40f, grayPaint)
            val file = File(getApplication<Application>().cacheDir, "billtracker_export.png")
            file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            FileProvider.getUriForFile(getApplication(), "${getApplication<Application>().packageName}.fileprovider", file)
        } catch (e: Exception) { null }
    }

    // 备份恢复
    suspend fun exportBackup(): Uri? {
        return BackupManager.exportToJson(getApplication(), dao, planStorage)
    }

    suspend fun importBackup(uri: Uri): Int {
        return BackupManager.importFromJson(getApplication(), dao, planStorage, uri)
    }
}
```

- [ ] **Step 5: 更新 MainScreen.kt 改用新 ViewModel**

将 `MainScreen` 的函数签名从 `(viewModel: MainViewModel = viewModel())` 改为接收 `LedgerViewModel`。将所有访问从 `viewModel.todayTransactions` / `viewModel.addTransaction` 等改为引用 `ledgerViewModel.xxx`。

```kotlin
// 函数签名
fun MainScreen(
    ledgerViewModel: LedgerViewModel = viewModel(),
    planViewModel: PlanViewModel = viewModel(),
    analysisViewModel: AnalysisViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel()
) {
```

传递 các sub-screen 所需的 ViewModel：
- `AIChatScreen` 仍需要 `LedgerViewModel`（用于 `addTransaction` 和 `todaySummary`）
- `PlanScreen` 接收 `PlanViewModel` 而非单个参数
- `AnalysisScreen` 接收 `AnalysisViewModel`
- `ProfileScreen` 接收 `ProfileViewModel`

- [ ] **Step 6: 更新 PlanScreen.kt 改用 PlanViewModel**

将 PlanScreen 从接收大量单个参数，改为接收 `PlanViewModel`：

```kotlin
@Composable
fun PlanScreen(planViewModel: PlanViewModel, ...) {
    val planBalance by planViewModel.planBalance.collectAsStateWithLifecycle()
    // 直接读取 planViewModel 的各个 StateFlow
}
```

- [ ] **Step 7: 更新 AnalysisScreen.kt 改用 AnalysisViewModel**

将 `AnalysisScreen(viewModel: MainViewModel)` 改为 `AnalysisScreen(analysisViewModel: AnalysisViewModel)`，更新内部调用。

- [ ] **Step 8: 更新 ProfileScreen.kt 改用 ProfileViewModel**

将 ProfileScreen 的参数从展开的单个回调改为接收 `ProfileViewModel`。

```kotlin
@Composable
fun ProfileScreen(profileViewModel: ProfileViewModel, ...) {
    val themeIndex by profileViewModel.themeIndex.collectAsStateWithLifecycle()
    val nickname by profileViewModel.nickname.collectAsStateWithLifecycle()
    // ...
}
```

- [ ] **Step 9: 更新 AIChatScreen.kt**

`AIChatScreen` 依赖 `viewModel.getTodaySummary()` 和 `viewModel.aiBillService`。`getTodaySummary` 移到 `AnalysisViewModel`，`aiBillService` 是 `AIBillService` 可以直接注入。

```kotlin
// 改为：
fun AIChatScreen(
    onBack: () -> Unit,
    onAddTransaction: (Double, TransactionType, String) -> Unit,
    analysisViewModel: AnalysisViewModel = viewModel(),
    aiService: AIBillService
)
```

并将 `viewModel.getTodaySummary()` 改为 `analysisViewModel.getTodaySummary()`。

- [ ] **Step 10: 删除 MainViewModel.kt**

```bash
rm app/src/main/java/com/example/billtracker/viewmodel/MainViewModel.kt
```

- [ ] **Step 11: 提交**

```bash
git add app/src/main/java/com/example/billtracker/viewmodel/ app/src/main/java/com/example/billtracker/ui/
git commit -m "refactor(viewmodel): split MainViewModel into LedgerViewModel, PlanViewModel, AnalysisViewModel, ProfileViewModel"
```

---

### Task 4: 引入 Compose Navigation

**Files:**
- Create: `ui/MainRoute.kt`
- Create: `ui/LedgerScreen.kt`
- Modify: `MainActivity.kt`
- Modify: `ui/MainScreen.kt`（部分内容移至 LedgerScreen.kt）

- [ ] **Step 1: 创建 MainRoute.kt（导航宿主）**

```kotlin
package com.example.billtracker.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.billtracker.ui.components.BillTrackerBottomBar
import com.example.billtracker.viewmodel.*

object Routes {
    const val MAIN = "main"
    const val TAB_LEDGER = "tab_ledger"
    const val TAB_PLAN = "tab_plan"
    const val TAB_ANALYSIS = "tab_analysis"
    const val TAB_PROFILE = "tab_profile"
    const val AI_CHAT = "ai_chat"
    const val SEARCH = "search"
    const val IMPORT = "import"
    const val ABOUT = "about"
}

@Composable
fun MainRoute(
    ledgerViewModel: LedgerViewModel,
    planViewModel: PlanViewModel,
    analysisViewModel: AnalysisViewModel,
    profileViewModel: ProfileViewModel
) {
    val navController = rememberNavController()

    // 判断是否应该在底部 tab 页面（隐藏 BottomBar 的子页面）
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val showBottomBar = currentRoute in listOf(
        Routes.TAB_LEDGER, Routes.TAB_PLAN, Routes.TAB_ANALYSIS, Routes.TAB_PROFILE
    ) || currentRoute == null

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                val selectedTab = when (currentRoute) {
                    Routes.TAB_LEDGER -> 0
                    Routes.TAB_PLAN -> 1
                    Routes.TAB_ANALYSIS -> 2
                    Routes.TAB_PROFILE -> 3
                    else -> 0
                }
                BillTrackerBottomBar(
                    selectedTab = selectedTab,
                    onTabSelected = { index ->
                        val route = when (index) {
                            0 -> Routes.TAB_LEDGER
                            1 -> Routes.TAB_PLAN
                            2 -> Routes.TAB_ANALYSIS
                            3 -> Routes.TAB_PROFILE
                            else -> Routes.TAB_LEDGER
                        }
                        navController.navigate(route) {
                            popUpTo(Routes.MAIN) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.MAIN,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.MAIN) {
                // 默认导航到记账 tab
                navController.navigate(Routes.TAB_LEDGER) {
                    popUpTo(Routes.MAIN) { inclusive = true }
                }
            }
            composable(Routes.TAB_LEDGER) {
                LedgerScreen(
                    ledgerViewModel = ledgerViewModel,
                    profileViewModel = profileViewModel,
                    onNavigateToAIChat = { navController.navigate(Routes.AI_CHAT) },
                    onNavigateToSearch = { navController.navigate(Routes.SEARCH) },
                    onNavigateToImport = { navController.navigate(Routes.IMPORT) }
                )
            }
            composable(Routes.TAB_PLAN) {
                PlanScreen(planViewModel = planViewModel)
            }
            composable(Routes.TAB_ANALYSIS) {
                AnalysisScreen(analysisViewModel = analysisViewModel)
            }
            composable(Routes.TAB_PROFILE) {
                ProfileScreen(
                    profileViewModel = profileViewModel,
                    onNavigateToImport = { navController.navigate(Routes.IMPORT) },
                    onNavigateToAbout = { navController.navigate(Routes.ABOUT) }
                )
            }
            composable(Routes.AI_CHAT) {
                AIChatScreen(
                    onBack = { navController.popBackStack() },
                    onAddTransaction = { amount, type, desc ->
                        ledgerViewModel.addTransaction(amount, type, desc)
                    },
                    analysisViewModel = analysisViewModel,
                    aiService = /* need to inject AIBillService */
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    onBack = { navController.popBackStack() },
                    allTransactions = { ledgerViewModel.allTransactions.value }
                )
            }
            composable(Routes.IMPORT) {
                ImportScreen(
                    onBack = { navController.popBackStack() },
                    onImport = { bills ->
                        ledgerViewModel.importBills(bills)
                    }
                )
            }
            composable(Routes.ABOUT) {
                AboutScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
```

- [ ] **Step 2: 创建 LedgerScreen.kt（从 MainScreen 提取记账 Tab 内容）**

从 `MainScreen.kt` 中提取 `selectedBottomTab == 0` 时显示的内容（记账 Tab）到单独文件。包含余额卡片、Tab 切换、交易列表、权限提示等。保留所有现有逻辑。

```kotlin
package com.example.billtracker.ui

// 导入与 MainScreen 相同的依赖
// 内容：从 MainScreen 复制 selectedBottomTab == 0 分支的 UI，删除其他 tab 分支
// 函数签名：
@Composable
fun LedgerScreen(
    ledgerViewModel: LedgerViewModel,
    profileViewModel: ProfileViewModel,
    onNavigateToAIChat: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToImport: () -> Unit
)
```

- [ ] **Step 3: 更新 MainActivity.kt**

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("plan_prefs", Context.MODE_PRIVATE)
        setContent {
            var themeIndex by remember { mutableStateOf(prefs.getInt("theme_index", 2)) }
            // ... 现有主题代码保持不变 ...

            BillTrackerTheme(themeIndex = effectiveThemeIndex, isDarkTheme = isDark, customConfig = customConfig) {
                MainRoute(
                    ledgerViewModel = viewModel(),
                    planViewModel = viewModel(),
                    analysisViewModel = viewModel(),
                    profileViewModel = viewModel()
                )
            }
        }
    }
}
```

- [ ] **Step 4: 提交**

```bash
git add app/src/main/java/com/example/billtracker/ui/MainRoute.kt app/src/main/java/com/example/billtracker/ui/LedgerScreen.kt app/src/main/java/com/example/billtracker/MainActivity.kt
git commit -m "feat(nav): introduce Compose Navigation with MainRoute host and sub-page routing"
```
