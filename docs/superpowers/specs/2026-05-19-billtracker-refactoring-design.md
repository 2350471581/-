# BillTracker 架构重构设计

> 基于 v0.8 代码库的按优先级改进：导航重写、ViewModel 拆分、主题色统一、UI 组件去重。

## 1. 导航架构

### 现状问题

单 `MainScreen` 用 `selectedBottomTab` int + 十几个 `showXxx` boolean flag 控制所有子页面，函数 1300 行，无返回栈支持。

### 方案

完整 Compose Navigation `NavHost` + 路由系统：

```
NavHost(startDestination = "main")
├── main                              // Scaffold + BottomBar 宿主
│   ├── tab_ledger  (记账 / tab 0)
│   ├── tab_plan    (计划 / tab 1)
│   ├── tab_analysis(分析 / tab 2)
│   └── tab_profile (我的 / tab 3)
├── ai_chat                           // AI 对话页
├── search                            // 搜索页
├── import                            // 导入页
└── about                             // 关于页
```

### 关键实现

- `MainScreen.kt` 拆为 `MainRoute.kt`（Scaffold + BottomBar 布局）和 `LedgerScreen.kt` / `PlanScreen.kt` / `AnalysisScreen.kt` / `ProfileScreen.kt` 四个独立 screen
- `MainRoute` 内部对底部 4 个 tab 仍用 `HorizontalPager` 保持滑动切换体验，不把 tab 页面做成独立 NavDestination
- 子页面（AI 聊天、搜索、导入、关于）通过 `navController.navigate()` 推入返回栈，自动获得系统返回键支持
- 双击返回退出逻辑只在 `MainRoute` 根路由且不在子页面时生效
- 底部导航栏在子页面路由时自动隐藏（通过 `currentBackStackEntry` 监听）

### 文件变更

| 操作 | 文件 |
|------|------|
| 删除 | `MainScreen.kt`（内容拆分到多个文件）|
| 新增 | `MainRoute.kt` — 导航宿主 + BottomBar 管理 |
| 新增 | `LedgerScreen.kt` — 记账 Tab 内容（从 MainScreen 抽出）|
| 修改 | `PlanScreen.kt` — 保持独立，调整参数签名 |
| 修改 | `AnalysisScreen.kt` — 从 `MainViewModel` 参数改为自有 ViewModel |
| 修改 | `ProfileScreen.kt` — 从 `MainViewModel` 参数改为自有 ViewModel |
| 修改 | `MainActivity.kt` — 引入 NavHost，去掉 `MainScreen()` 直接调用 |

## 2. ViewModel 拆分

### 现状问题

`MainViewModel` 管理所有业务逻辑（~530 行），违反单一职责原则。

### 方案

从 `MainViewModel` 拆出 5 个 ViewModel：

```
MainViewModel (删除)
├── LedgerViewModel      → 交易列表、今日收支、增删改查、重复检测、SMS 刷新
├── PlanViewModel        → 余额、今日计划、总计划、省钱计划、自定义计划
├── AnalysisViewModel    → 时间范围筛选、统计数据、图表数据、AI 月总结
├── ProfileViewModel     → 主题、昵称、头像、导出 CSV/图片、备份恢复、更新检查
└── AIChatViewModel      → AI 对话消息、解析结果、确认记账 (保持现状)
```

### 依赖关系

所有 ViewModel 通过 Hilt `@HiltViewModel` + `@Inject constructor` 注入：

```
                    ┌─────────────────┐
                    │  TransactionDao  │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              ▼              ▼              ▼
     LedgerViewModel   AnalysisViewModel  ProfileViewModel
          │                                  │
          │  ┌──────────────┐               │
          │  │  PlanStorage  │◄──────────────┤
          │  └──────────────┘               │
          ▼                                  ▼
   TransactionRepository               BackupManager
```

### 跨 ViewModel 数据同步

- 新增/删除交易后，`LedgerViewModel` 写 Room → `PlanViewModel` 和 `AnalysisViewModel` 通过 Room `Flow` 自动收到更新
- 主题切换在 `ProfileViewModel` 中保存到 `SharedPreferences`，`MainActivity` 通过 `DisposableEffect` 监听变更同步

### 文件变更

| 操作 | 文件 |
|------|------|
| 删除 | `viewmodel/MainViewModel.kt` |
| 新增 | `viewmodel/LedgerViewModel.kt` |
| 新增 | `viewmodel/PlanViewModel.kt` |
| 新增 | `viewmodel/AnalysisViewModel.kt` |
| 新增 | `viewmodel/ProfileViewModel.kt` |
| 修改 | 各 Screen composable 改用对应的新 ViewModel |

## 3. 统一主题色引用

### 现状问题

硬编码颜色值散落在所有 UI 文件中：`Color(0xFF9AA0A6)`、`Color(0xFFF1F3F4)`、`Color(0xFF5F6368)`、`Color(0xFFBDBDBD)` 等。

### 方案

在 `Theme.kt` 中定义语义化颜色常量：

```kotlin
// Theme.kt / ThemeManager.kt
val DividerColor = Color(0xFFF1F3F4)           // 分隔线颜色
val SubtleTextColor = Color(0xFF9AA0A6)        // 次要文字
val MutedIconColor = Color(0xFFBDBDBD)         // 静默图标
val DarkSubtleText = Color(0xFF5F6368)         // 深色次要文字
```

现有的 `IncomeGreen`、`ExpenseRed`、`WechatGreen`、`AlipayBlue` 保留不动。

### 替换范围

全局查找替换，涉及文件：`MainScreen.kt`、`ProfileScreen.kt`、`AnalysisScreen.kt`、`PlanScreen.kt`、`SearchScreen.kt`、`AIChatScreen.kt`。

## 4. 去重公共 UI 组件

### 现状问题

同一 UI 模式在多个文件中重复实现。

### 方案

| 组件 | 来源文件 | 目标文件 |
|------|---------|---------|
| `TransactionCard` | 合并 `TransactionItem`、`SearchResultCard`、`AnalysisTransactionRow` | `components/TransactionCard.kt` |
| `SourceLabel` | 4 处 `when` 判断来源标签 | `components/SourceLabel.kt` |
| `CardBg()` | 每个 Screen 的 `if (isDark) Color(0xFF2A2A2A) else Color.White` | `Theme.kt` 统一函数 |
| `CountdownButton` | AI 教程弹窗重复 2 次 | `components/CountdownButton.kt` |
| `PlanDetailDialog` 合并 | 去重 `SavePlanDetailDialog` | 保留 `PlanDetailDialog` 加参数 |

## 5. 实施顺序

1. **第一步**：统一主题色 + 抽取 `CardBg()` — 最安全，纯查找替换，不改逻辑
2. **第二步**：抽取公共 UI 组件（TransactionCard、SourceLabel、CountdownButton、合并 PlanDetailDialog）
3. **第三步**：拆分 ViewModel（LedgerViewModel、PlanViewModel、AnalysisViewModel、ProfileViewModel）
4. **第四步**：引入 Compose Navigation（MainRoute + 子页面路由）

每步完成后提交一次，确保可回退。

## 6. 不在此次范围的改进

- 测试代码（需先拆完才能写）
- 图表增强（独立功能）
- 生物识别锁（独立功能）
- 云同步（独立功能）
