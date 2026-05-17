# 账单导入功能

## 目标
支持从 CSV 文件和支付截图（淘宝/拼多多等）导入账单数据。

## 技术方案
- OCR: Google ML Kit `text-recognition-chinese`（设备端离线识别，不上传）
- CSV: 兼容 `日期,金额,类型,分类,备注` 格式
- 新增 `ImportScreen` + `BillImporter`，入口在"我的"页面

## 新增文件

### BillImporter.kt
- `parseCsv(content: String): List<ParsedBill>` — CSV 解析
- `parseOcrText(rawText: String): List<ParsedBill>` — OCR 文本解析，提取金额/商家
- `ParsedBill` data class: date, amount, type, category, description

### ImportScreen.kt
- 文件选择 → 预览列表（可编辑单条）
- 图片选择 → OCR 识别 → 预览列表
- "确认导入"批量写入数据库

## 修改文件

### build.gradle.kts
- 添加 `com.google.mlkit:text-recognition-chinese`

### ProfileScreen.kt
- 导出卡片中加"导入账单"选项

### MainScreen.kt
- `showImport` 状态 + ImportScreen 路由
