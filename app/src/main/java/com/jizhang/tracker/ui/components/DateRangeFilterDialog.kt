package com.jizhang.tracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import com.jizhang.tracker.R
import com.jizhang.tracker.ui.DarkSubtleText
import com.jizhang.tracker.ui.DividerColor
import com.jizhang.tracker.ui.DarkTextColor
import com.jizhang.tracker.ui.SubtleText

@Composable
fun DateRangeFilterDialog(
    initialStart: Long?,
    initialEnd: Long?,
    onConfirm: (start: Long?, end: Long?) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var startDate by remember { mutableStateOf(initialStart) }
    var endDate by remember { mutableStateOf(initialEnd) }

    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }

    fun showDatePicker(current: Long, onSelected: (Long) -> Unit) {
        val cal = Calendar.getInstance().apply { timeInMillis = current }
        android.app.DatePickerDialog(
            context,
            { _, year, month, day ->
                cal.set(year, month, day, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                onSelected(cal.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(stringResource(R.string.date_filter_title), fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.date_filter_start_label), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = DarkSubtleText)
                Surface(
                    onClick = { showDatePicker(startDate ?: System.currentTimeMillis()) { startDate = it } },
                    shape = RoundedCornerShape(10.dp),
                    color = DividerColor
                ) {
                    Text(
                        text = if (startDate != null) dateFormat.format(Date(startDate!!)) else stringResource(R.string.date_filter_click_to_select),
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        fontSize = 14.sp,
                        color = if (startDate != null) DarkTextColor else SubtleText
                    )
                }

                Text(stringResource(R.string.date_filter_end_label), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = DarkSubtleText)
                Surface(
                    onClick = { showDatePicker(endDate ?: System.currentTimeMillis()) { endDate = it } },
                    shape = RoundedCornerShape(10.dp),
                    color = DividerColor
                ) {
                    Text(
                        text = if (endDate != null) dateFormat.format(Date(endDate!!)) else stringResource(R.string.date_filter_click_to_select),
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        fontSize = 14.sp,
                        color = if (endDate != null) DarkTextColor else SubtleText
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(startDate, endDate) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.date_filter_confirm), modifier = Modifier.padding(horizontal = 8.dp))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onReset) {
                    Text(stringResource(R.string.date_filter_reset), color = DarkSubtleText)
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.date_filter_cancel), color = DarkSubtleText)
                }
            }
        }
    )
}
