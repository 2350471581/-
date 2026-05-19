package com.jizhang.tracker.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jizhang.tracker.R
import com.jizhang.tracker.ui.components.TagChip

@Composable
fun ExportDialog(
    onExportCsv: (Long, Long) -> Unit,
    onExportImage: (Long, Long) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var exportTimeRange by remember { mutableIntStateOf(0) }
    var exportStartDate by remember { mutableLongStateOf(0L) }
    var exportEndDate by remember { mutableLongStateOf(0L) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text(stringResource(R.string.profile_export_title), fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.profile_time_range), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        stringResource(R.string.profile_all_time),
                        stringResource(R.string.profile_last_30_days),
                        stringResource(R.string.profile_custom_range)
                    ).forEachIndexed { i, label ->
                        TagChip(
                            selected = exportTimeRange == i,
                            onClick = {
                                exportTimeRange = i
                                if (i == 0) { exportStartDate = 0L; exportEndDate = 0L }
                                else if (i == 1) {
                                    exportEndDate = 0L
                                    exportStartDate = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
                                }
                            },
                            label = label
                        )
                    }
                }

                if (exportTimeRange == 2) {
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.profile_start_date), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        onClick = {
                            val cal = java.util.Calendar.getInstance()
                            if (exportStartDate > 0) cal.timeInMillis = exportStartDate
                            android.app.DatePickerDialog(
                                context, { _, year, month, day ->
                                    cal.set(year, month, day, 0, 0, 0)
                                    cal.set(java.util.Calendar.MILLISECOND, 0)
                                    exportStartDate = cal.timeInMillis
                                },
                                cal.get(java.util.Calendar.YEAR),
                                cal.get(java.util.Calendar.MONTH),
                                cal.get(java.util.Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val fmt = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault())
                        Text(
                            text = if (exportStartDate > 0) fmt.format(java.util.Date(exportStartDate)) else stringResource(R.string.profile_click_to_select),
                            modifier = Modifier.padding(12.dp),
                            fontSize = 14.sp,
                            color = if (exportStartDate > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.profile_end_date), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        onClick = {
                            val cal = java.util.Calendar.getInstance()
                            if (exportEndDate > 0) cal.timeInMillis = exportEndDate
                            android.app.DatePickerDialog(
                                context, { _, year, month, day ->
                                    cal.set(year, month, day, 23, 59, 59)
                                    cal.set(java.util.Calendar.MILLISECOND, 999)
                                    exportEndDate = cal.timeInMillis
                                },
                                cal.get(java.util.Calendar.YEAR),
                                cal.get(java.util.Calendar.MONTH),
                                cal.get(java.util.Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val fmt = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault())
                        Text(
                            text = if (exportEndDate > 0) fmt.format(java.util.Date(exportEndDate)) else stringResource(R.string.profile_click_to_select),
                            modifier = Modifier.padding(12.dp),
                            fontSize = 14.sp,
                            color = if (exportEndDate > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Surface(
                    onClick = {
                        onDismiss()
                        val s = if (exportTimeRange == 0) 0L else exportStartDate
                        val e = if (exportTimeRange == 0) 0L else exportEndDate
                        onExportCsv(s, e)
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(stringResource(R.string.profile_export_csv), fontSize = 15.sp, fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface)
                            Text(stringResource(R.string.profile_export_csv_desc), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Surface(
                    onClick = {
                        onDismiss()
                        val s = if (exportTimeRange == 0) 0L else exportStartDate
                        val e = if (exportTimeRange == 0) 0L else exportEndDate
                        onExportImage(s, e)
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(stringResource(R.string.profile_export_image), fontSize = 15.sp, fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface)
                            Text(stringResource(R.string.profile_export_image_desc), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.profile_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    )
}
