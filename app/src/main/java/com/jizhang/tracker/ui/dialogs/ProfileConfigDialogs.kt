package com.jizhang.tracker.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jizhang.tracker.R
import com.jizhang.tracker.ui.ExpenseRed

@Composable
fun CategoriesDialog(
    customCategories: List<com.jizhang.tracker.data.CustomCategory>,
    onAdd: (com.jizhang.tracker.data.CustomCategory) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var catName by remember { mutableStateOf("") }
    var catIcon by remember { mutableStateOf("📌") }
    var catKeywords by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text(stringResource(R.string.profile_manage_categories_title), fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
        text = {
            Column {
                if (customCategories.isEmpty()) {
                    Text(
                        stringResource(R.string.profile_category_empty),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Text(
                        "自定义分类",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    customCategories.forEach { cat ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text("${cat.icon} ", fontSize = 16.sp)
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = cat.name,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = { onRemove(cat.name) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.profile_category_delete_cd, cat.name),
                                    tint = ExpenseRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = catName,
                    onValueChange = { catName = it },
                    placeholder = { Text(stringResource(R.string.profile_category_name_hint), fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = catIcon,
                        onValueChange = { catIcon = it.take(2) },
                        placeholder = { Text(stringResource(R.string.profile_category_icon_hint), fontSize = 14.sp) },
                        singleLine = true,
                        modifier = Modifier.width(80.dp),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = catKeywords,
                        onValueChange = { catKeywords = it },
                        placeholder = { Text(stringResource(R.string.profile_category_keywords_hint), fontSize = 14.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (catName.isNotBlank()) {
                            val kwList = catKeywords.split("，", ",").map { it.trim() }.filter { it.isNotBlank() }
                            onAdd(com.jizhang.tracker.data.CustomCategory(name = catName.trim(), icon = catIcon.ifBlank { "📌" }, keywords = kwList))
                            catName = ""; catIcon = "📌"; catKeywords = ""
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    enabled = catName.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.profile_category_add)) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.profile_close), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
fun AiPromptDialog(
    currentPrompt: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var promptInput by remember { mutableStateOf(currentPrompt) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text(stringResource(R.string.profile_custom_ai_prompt_title), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    stringResource(R.string.profile_custom_ai_prompt_desc),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text(stringResource(R.string.profile_custom_ai_prompt_hint), fontSize = 13.sp) },
                    maxLines = 15
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(promptInput); onDismiss() },
                shape = RoundedCornerShape(10.dp)
            ) { Text(stringResource(R.string.profile_custom_ai_prompt_save)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    promptInput = ""
                    onSave("")
                    onDismiss()
                }) { Text(stringResource(R.string.profile_custom_ai_prompt_clear), color = ExpenseRed) }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.profile_close), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    )
}

@Composable
fun TriggerKeywordsDialog(
    triggerKeywords: Set<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newKeyword by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text(stringResource(R.string.profile_trigger_keywords_title), fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
        text = {
            Column {
                if (triggerKeywords.isEmpty()) {
                    Text(
                        stringResource(R.string.profile_trigger_keywords_empty),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Text(
                        "默认关键词始终生效",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    val customKeywords = triggerKeywords - setOf("微信", "支付宝")
                    if (customKeywords.isNotEmpty()) {
                        Column {
                            customKeywords.forEach { kw ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = kw,
                                            fontSize = 14.sp,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { onRemove(kw) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = stringResource(R.string.profile_trigger_keywords_delete_cd, kw),
                                            tint = ExpenseRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            stringResource(R.string.profile_trigger_keywords_empty),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newKeyword,
                        onValueChange = { newKeyword = it.trim() },
                        placeholder = { Text(stringResource(R.string.profile_trigger_keywords_add_hint), fontSize = 14.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newKeyword.isNotBlank()) {
                                onAdd(newKeyword)
                                newKeyword = ""
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        enabled = newKeyword.isNotBlank()
                    ) { Text(stringResource(R.string.profile_trigger_keywords_add)) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.profile_close), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
