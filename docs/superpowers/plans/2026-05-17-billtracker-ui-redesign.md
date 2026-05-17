# BillTracker UI Redesign & Code Optimization

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Beautify the UI with Organic design anchor (higher saturation), add gradient backgrounds, redesign bottom navigation, and optimize Kotlin code.

**Architecture:** Single-module Jetpack Compose app with MVVM architecture. Changes are primarily in the `ui/` package, with design tokens centralized in ThemeManager.kt and Theme.kt. Background gradient uses Compose `Brush.verticalGradient` with theme-aware colors. Bottom nav uses custom `ImageVector` resources and Material 3 components.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Android Gradle Plugin

---

### Task 1: Refine Theme System with Higher-Saturation Organic Colors

**Files:**
- Modify: `app/src/main/java/com/example/billtracker/ui/ThemeManager.kt` (full file)
- Modify: `app/src/main/java/com/example/billtracker/ui/Theme.kt` (full file)

- [ ] **Step 1: Update ThemeManager.kt with refined palette**

Update `ThemePalette` data class and all theme entries. Key changes:
- Higher saturation colors across all 5 themes
- Matcha Green (抹茶绿) becomes index 0 (default)
- Refined income/expense colors
- Add `backgroundGradientStart` and `backgroundGradientEnd` colors to `ThemePalette`

```kotlin
data class ThemePalette(
    val name: String,
    val primary: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val income: Color,
    val expense: Color,
    val gradientStart: Color,
    val gradientEnd: Color
)

val Themes = listOf(
    ThemePalette(
        name = "抹茶绿",
        primary = Color(0xFF7AA85A),
        background = Color(0xFFF5F8F2),
        surface = Color(0xFFFAFCF9),
        surfaceVariant = Color(0xFFEAF0E4),
        income = Color(0xFF5B9A5A),
        expense = Color(0xFFD4806A),
        gradientStart = Color(0xFFF5FBF2),
        gradientEnd = Color(0xFFA0C888)
    ),
    ThemePalette(
        name = "暖橙",
        primary = Color(0xFFD4784A),
        background = Color(0xFFFDF6F0),
        surface = Color(0xFFFFFCF9),
        surfaceVariant = Color(0xFFF5EDE4),
        income = Color(0xFF5B8C5A),
        expense = Color(0xFFD4604A),
        gradientStart = Color(0xFFFDF6F0),
        gradientEnd = Color(0xFFE8C8A8)
    ),
    ThemePalette(
        name = "静谧蓝",
        primary = Color(0xFF6A8FA8),
        background = Color(0xFFF4F7F9),
        surface = Color(0xFFF9FBFD),
        surfaceVariant = Color(0xFFE8EEF2),
        income = Color(0xFF5B8C5A),
        expense = Color(0xFFD4605A),
        gradientStart = Color(0xFFF2F8FC),
        gradientEnd = Color(0xFFB8D4E8)
    ),
    ThemePalette(
        name = "樱花粉",
        primary = Color(0xFFC48A9A),
        background = Color(0xFFFDF8F9),
        surface = Color(0xFFFFFCFC),
        surfaceVariant = Color(0xFFF5EEF0),
        income = Color(0xFF6A9E6A),
        expense = Color(0xFFD4606A),
        gradientStart = Color(0xFFFDF8F9),
        gradientEnd = Color(0xFFE8C8D0)
    ),
    ThemePalette(
        name = "经典蓝",
        primary = Color(0xFF3A7BD5),
        background = Color(0xFFF8F9FB),
        surface = Color.White,
        surfaceVariant = Color(0xFFF0F2F4),
        income = Color(0xFF34A853),
        expense = Color(0xFFD4604A),
        gradientStart = Color(0xFFF2F6FC),
        gradientEnd = Color(0xFFA8C8E8)
    ),
)
```

Update `colorSchemeFrom` and `darkColorSchemeFrom` to use refined colors. Remove `avatarEmojis` (move to a dedicated file).

- [ ] **Step 2: Update Theme.kt**

Update `BillTrackerTheme` composable to accept new palette fields and set default theme index to 0 (Matcha Green). Keep the existing typography (思源黑体) but refine any weights.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/billtracker/ui/ThemeManager.kt app/src/main/java/com/example/billtracker/ui/Theme.kt
git commit -m "refactor(theme): refine colors with high-saturation Organic palette, set matcha green as default"
```

---

### Task 2: Add Background Gradient System

**Files:**
- Modify: `app/src/main/java/com/example/billtracker/ui/ThemeManager.kt`
- Modify: `app/src/main/java/com/example/billtracker/ui/MainScreen.kt`
- Create: `app/src/main/java/com/example/billtracker/ui/components/BackgroundGradient.kt`

- [ ] **Step 1: Create BackgroundGradient composable**

```kotlin
// components/BackgroundGradient.kt
package com.example.billtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun BackgroundGradient(
    gradientStart: Color,
    gradientEnd: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(gradientStart, gradientEnd)
                )
            )
    ) {
        content()
    }
}
```

- [ ] **Step 2: Integrate gradient into MainScreen**

Wrap the Scaffold content column with `BackgroundGradient`. Pass gradient colors from the theme palette via MaterialTheme access.

- [ ] **Step 3: Make cards semi-transparent (frosted glass effect)**

Update card colors in main screens to use `Color.White.copy(alpha = 0.88f)` with elevated shadows. Apply `background` modifier with slight transparency.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/billtracker/ui/components/BackgroundGradient.kt app/src/main/java/com/example/billtracker/ui/MainScreen.kt
git commit -m "feat(ui): add theme-aware background gradient with frosted glass cards"
```

---

### Task 3: Redesign Bottom Navigation

**Files:**
- Modify: `app/src/main/java/com/example/billtracker/ui/MainScreen.kt`
- Create: `app/src/main/java/com/example/billtracker/ui/components/BottomNavBar.kt`

- [ ] **Step 1: Create custom SVG icon resources for bottom nav**

Create three vector drawables:
- `ic_nav_receipt.xml` — receipt/clipboard icon for "记账"
- `ic_nav_star.xml` — star icon for "计划"  
- `ic_nav_person.xml` — person icon for "我的"

Use Material Design SVG paths converted to Android vector drawable format.

- [ ] **Step 2: Create BottomNavBar composable**

```kotlin
// components/BottomNavBar.kt
@Composable
fun BillTrackerBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        NavTab("记账", Icons.Default.Receipt),
        NavTab("计划", Icons.Default.Star),
        NavTab("我的", Icons.Default.Person)
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = Color.White.copy(alpha = 0.92f),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = selectedTab == index
                BottomNavItem(
                    icon = tab.icon,
                    label = tab.label,
                    isSelected = isSelected,
                    onClick = { onTabSelected(index) }
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val iconTint = if (isSelected) Color.White else Color(0xFFB0B0B0)
    val bgColor = if (isSelected) primary else Color.Transparent

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        if (isSelected) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = primary,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}
```

- [ ] **Step 3: Replace old bottom bar in MainScreen**

Replace the existing `Surface`-based bottom bar with `BillTrackerBottomBar`. Remove the old `BottomNavItem` private function. Remove the unused `Icons.Default.Receipt`, `Star`, `Person` imports if they become unused.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/billtracker/ui/components/BottomNavBar.kt app/src/main/java/com/example/billtracker/ui/MainScreen.kt app/src/main/res/drawable/
git commit -m "feat(nav): redesign bottom navigation with theme-aware icons and labels"
```

---

### Task 4: Refine Balance Card Design

**Files:**
- Modify: `app/src/main/java/com/example/billtracker/ui/MainScreen.kt`

- [ ] **Step 1: Update WeChatStyleBalanceCard**

Redesign the balance card with:
- Frosted glass background (`Color.White.copy(alpha = 0.88f)`)
- Larger, bolder net amount display
- Clearer income/expense split with arrow indicators (↑/↓)
- Theme-aware accent colors
- Manual/Auto toggle styled as segmented control with rounded corners

```kotlin
@Composable
fun WeChatStyleBalanceCard(...) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.88f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // title row with segment control
            // net amount
            // divider
            // income / expense row
        }
    }
}
```

- [ ] **Step 2: Update TransactionItem**

Update the transaction item card:
- Frosted glass background
- Thinner color indicator strip (4dp)
- Better spacing and typography
- Left-swipe action buttons with theme colors

- [ ] **Step 3: Update TransactionDetailCard**

Apply consistent styling:
- Frosted glass card
- Rounded corners 24dp
- Theme-colored accents on buttons

- [ ] **Step 4: Update AddTransactionDialog**

Redesign with:
- Larger corner radius (24dp)
- Better type selection with theme colors
- Consistent button styling

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/billtracker/ui/MainScreen.kt
git commit -m "feat(ui): refine balance card, transaction items, and dialogs with frosted glass style"
```

---

### Task 5: Polish ProfileScreen

**Files:**
- Modify: `app/src/main/java/com/example/billtracker/ui/ProfileScreen.kt`

- [ ] **Step 1: Update card styling**

Apply `Color.White.copy(alpha = 0.88f)` and consistent 16dp rounded corners to all cards.

- [ ] **Step 2: Update menu items**

Refine `ProfileMenuItem` with:
- Better icon spacing and sizing
- Consistent RoundedCornerShape(12.dp) for interactive areas
- Theme-aware arrow indicators

- [ ] **Step 3: Update all dialogs**

Apply consistent styling to all dialogs in ProfileScreen:
- 24dp corner radius for Dialog cards
- Theme-colored buttons
- Consistent typography

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/billtracker/ui/ProfileScreen.kt
git commit -m "feat(profile): apply consistent Organic styling to profile screen and dialogs"
```

---

### Task 6: Polish PlanScreen & AnalysisScreen

**Files:**
- Modify: `app/src/main/java/com/example/billtracker/ui/PlanScreen.kt`
- Modify: `app/src/main/java/com/example/billtracker/ui/AnalysisScreen.kt`

- [ ] **Step 1: Update BalanceCard in PlanScreen**

Apply frosted glass card style, consistent with the main screen balance card.

- [ ] **Step 2: Update PlanCard and SavePlanCard**

Apply consistent styling:
- `Color.White.copy(alpha = 0.88f)` backgrounds
- 20dp rounded corners
- Theme-colored progress bars
- Segmented color for progress (green when complete, orange when >70%, red when over budget)

- [ ] **Step 3: Update PlanDetailDialog and SavePlanDetailDialog**

Consistent 24dp corner radius, frosted glass, theme-colored buttons.

- [ ] **Step 4: Update AnalysisScreen cards**

Apply frosted glass card style to stat cards, bar chart card, and pie chart card.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/billtracker/ui/PlanScreen.kt app/src/main/java/com/example/billtracker/ui/AnalysisScreen.kt
git commit -m "feat(plans): apply consistent Organic styling to plan and analysis screens"
```

---

### Task 7: Code Optimization

**Files:**
- Modify: All UI files

- [ ] **Step 1: Extract hardcoded color constants**

Move all hardcoded `Color(0xFF...)` values in UI files to use `MaterialTheme.colorScheme` or centralized constants. Key colors to replace:
- `Color(0xFFF8F9FA)` → use theme background
- `Color(0xFFF1F3F4)` → use theme surfaceVariant or outline
- `Color(0xFF9AA0A6)` → use `onSurfaceVariant`
- `Color(0xFF5F6368)` → use `onSurfaceVariant`
- `Color(0xFFE8824A)` → use `MaterialTheme.colorScheme.primary`
- `Color(0xFFBDBDBD)` → use `outline`

- [ ] **Step 2: Remove unused imports**

Remove unused import statements across all UI files (e.g., unused `Icons`, unused Compose imports).

- [ ] **Step 3: Extract repeated patterns**

Extract common dialog patterns (e.g., the countdown dialog pattern with "知道了" button) into reusable composables.

- [ ] **Step 4: Standardize file structure**

Ensure all UI files have consistent import ordering, consistent code formatting, and no dead code.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/billtracker/ui/
git commit -m "refactor: extract color constants, remove dead code, standardize formatting"
```

---

### Task 8: Set Up Auto-Brainstorming Hook

**Files:**
- Create/Modify: `.claude/settings.local.json` in X:\BillTracker

- [ ] **Step 1: Configure settings.json with brainstorming hook**

```json
{
  "hooks": {
    "preToolCall": {
      "match": {
        "tool": "Edit|Write"
      },
      "command": "claude /brainstorming"
    }
  }
}
```

Wait — actually this approach won't work cleanly because hook commands can't conditionally invoke skills. The proper approach is to add a `pre-tool-call` hook that checks if we're about to write code and prompts brainstorming.

Actually, the user wants "每一次改动都自动启用这个" - they want brainstorming to run before every change. The cleanest way is to add a `pre-tool-call` or `pre-edit` hook that runs brainstorming. But since hooks run shell commands and can't directly invoke skills, a better approach would be a pre-hook that asks a question.

Let me use a simpler approach: add a `.claude/rules.md` or `CLAUDE.md` instruction that says to always invoke brainstorming before making changes.

- [ ] **Step 2: Update CLAUDE.md in project**

Append to the project's CLAUDE.md:
```
## Auto-Brainstorming
Before making any code changes, always invoke the `brainstorming` skill first.
```

- [ ] **Step 3: Commit**

```bash
git add .claude/settings.local.json CLAUDE.md
git commit -m "chore: add auto-brainstorming hook configuration"
```
