# 个人页面、关于页面与导航栏重新设计

## 目标
1. 我的页面使用主题色（修复硬编码颜色）
2. 关于改为全屏页面，含介绍、作者邮箱、赞赏二维码
3. 底部导航栏改为 iOS 风格玻璃质感，带主题色淡染

## 修改内容

### 1. ProfileScreen.kt
- 修复 `Color(0xFF2C241A)` 两处 → `MaterialTheme.colorScheme.onSurface`
- 移除设置菜单中的"赞赏作者"独立条目（移入关于页面）
- 用 `showAboutScreen` 状态控制全屏 AboutScreen 覆盖层

### 2. AboutScreen（ProfileScreen.kt 内新增全屏 composable）
- 全屏覆盖层，顶部返回按钮 + 标题
- 应用介绍文字（从现有关于弹窗搬移）
- 功能列表
- 作者信息：3175878672@qq.com
- 赞赏二维码区域（从原赞赏弹窗搬移）

### 3. BottomNavBar.kt
- 提高背景透明度（alpha 从 0.88 → ~0.55）
- 叠加主题色淡染（primary.copy(alpha = 0.08f)）
- 保留现有圆角与布局结构

### 4. 移除
- 删除 `showDonateDialog`、`showAboutDialog` 改 `showAboutScreen`
- 移除原"赞赏作者"菜单条目和其 dialog 代码
- 移除原关于 dialog 代码
