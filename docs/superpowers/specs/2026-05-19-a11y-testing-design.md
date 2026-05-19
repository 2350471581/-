# 无障碍与单元测试改进设计

> **Goal:** 为 BillTracker 添加系统的无障碍语义支持和 ViewModel + Repository 层单元测试

**Architecture:** 测试层基于 JUnit 5 + MockK + in-memory Room + coroutines-test，在 `app/src/test/` 下按包结构组织。无障碍改造在每个 Composable 组件上直接添加语义属性，不改变 UI 布局或业务逻辑。

**Tech Stack:** JUnit 4.13.2, MockK 1.13.9, kotlinx-coroutines-test 1.7.3, Compose Semantics API

---

## 测试覆盖范围

### ViewModel 层

- `LedgerViewModelTest`:
  - 添加交易 — 正常添加、重复检测去重、收入/支出分类
  - 删除交易 — 删除后列表更新
  - 今日摘要 — `getTodaySummary` 返回正确的收入和支出总和
  - 日期筛选 — `filterStartDate`/`filterEndDate` 变化后数据过滤正确
  - 模式切换 — `setManualMode`/`isManualMode` 状态正确
- `PlanViewModelTest`:
  - 预算目标 — 今日/总目标写入和读取一致
  - 自定义计划 — 添加、编辑、删除完整 CRUD
- `AnalysisViewModelTest`:
  - `generateMonthlySummary` — 调用成功返回摘要文本，失败返回空字符串

### Data 层

- `PlanStorageTest` — 所有属性（planBalance、todayPlanTarget、nickname 等）读写一致性验证

### 不覆盖的

- UI 自动化测试（Compose UI Test）— 第二优先级，本次不做
- 端到端测试 — 需真机环境，不做
- AI 服务（DeepSeek/AIBillService）— 依赖外部 API，不 mock

## 无障碍（语义标签 + 焦点顺序）

### 改造原则

- 不改变任何视觉 UI 布局和样式
- 每个可交互元素必须有 `contentDescription`
- 信息性卡片合并为单一语义节点（避免逐字朗读）
- 自定义主题下的对比度不做额外调整（主题色由用户选择，超出本次范围）

### 按屏幕改动

#### 1. TransactionCard / SourceLabel
- 整张卡片用 `semantics { contentDescription = "支出 / 35.00元 / 餐饮 / 微信" }` 聚合
- 金额、分类、来源各自设为 `invisibleToUser` 防止重复朗读
- `SourceLabel` 的图标设为 `contentDescription = null`（装饰性）

#### 2. 底部导航栏 (BottomNavBar)
- 当前 tab 加 `semantics { stateDescription = "已选中" }`
- 非选中 tab 保持默认

#### 3. 余额卡片 (WeChatStyleBalanceCard)
- 整体用 `semantics { contentDescription = "今日净收入 +128.00元，收入 200.00元，支出 72.00元" }` 合并
- 手动/自动切换按钮补充 `contentDescription = "切换到手动模式/自动模式"`

#### 4. 搜索、AI 聊天、计划页面
- 搜索框 `contentDescription = "搜索账单描述、分类或金额"`
- 清除按钮 `contentDescription = "清除搜索内容"`
- AI 聊天输入框、发送按钮补充标签
- 计划页面各数字输入框补充 `contentDescription`

#### 5. 分析页面 (AnalysisScreen)
- 月份切换按钮 `contentDescription = "上一个月" / "下一个月"`
- 图表区域简要描述状态：`"本月支出趋势图，显示近12个月数据"`

#### 6. 弹窗
- 添加账单/计划弹窗中每个输入字段补充标签
- 确认/取消按钮已有 `contentDescription`，统一风格
- 交易详情卡的备注编辑框补充标签

#### 7. 其它
- PrivacyFooter — 纯信息性，合并为单行 `contentDescription = "你的隐私数据仅保存在本地，不会上传"`
- TransactionItem 的左滑操作按钮（详情、删除）已有图标，补充 text + 描述一致性

## 测试基础设施

### 目录结构

```
app/src/test/java/com/example/billtracker/
├── data/
│   └── PlanStorageTest.kt
├── viewmodel/
│   ├── LedgerViewModelTest.kt
│   ├── PlanViewModelTest.kt
│   └── AnalysisViewModelTest.kt
└── TestUtils.kt (共享工具函数)
```

### 测试辅助

- `TestUtils.kt` 提供：
  - `createTestTransaction()` — 快速构造 `TransactionEntity` 实例
  - `TestDispatchers` — 使用 `StandardTestDispatcher` 的共享协程上下文

### ViewModel 测试模式

每个 ViewModel 测试使用：
- `MockK` mock 依赖的 Repository / DAO / PlanStorage
- `TestCoroutineScope` + `StandardTestDispatcher` 控制协程时序
- `UnconfinedTestDispatcher` 用于 fire-and-forget 协程
- `InstantTaskExecutorRule` 确保 LiveData 同步执行

## 实施顺序

1. 完成测试基础设施（TestUtils、测试基类）
2. 实现 PlanStorageTest（最底层，无依赖）
3. 实现 LedgerViewModelTest（核心功能）
4. 实现 PlanViewModelTest（简单 CRUD）
5. 实现 AnalysisViewModelTest（含协程调用）
6. 无障碍：TransactionCard / SourceLabel
7. 无障碍：BottomNavBar + 余额卡片
8. 无障碍：弹窗（添加账单/计划/交易详情）
9. 无障碍：搜索、AI聊天、分析、计划页面
10. 无障碍：其余屏幕（隐私声明、左滑操作等）

## 验收标准

- 全部测试通过：`./gradlew testDebugUnitTest`
- 每次提交前测试通过
- 无障碍：TalkBack 开启后能完整走通"记账 → 查看今日明细 → 打开详情"流程
- 无障碍：TalkBack 能读出余额卡片上的所有金额信息
- 无障碍：弹窗打开时焦点自动定位到第一个输入字段
