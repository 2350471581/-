package com.jizhang.tracker.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlinx.coroutines.launch
import com.jizhang.tracker.data.AppUpdater
import com.jizhang.tracker.data.DownloadResult
import com.jizhang.tracker.data.UpdateResult
import com.jizhang.tracker.ui.ExpenseRed
import com.jizhang.tracker.ui.IncomeGreen
import com.jizhang.tracker.ui.OrangeTint

@Composable
fun UpdateDialog(
    onDismiss: () -> Unit
) {
    var updateState by remember { mutableStateOf("") }
    var updateInfo by remember { mutableStateOf<com.jizhang.tracker.data.UpdateInfo?>(null) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    var updateError by remember { mutableStateOf("") }
    var downloadedFile by remember { mutableStateOf<java.io.File?>(null) }
    var showManualUpdateDialog by remember { mutableStateOf(false) }
    var showManualDownloadDialog by remember { mutableStateOf(false) }

    val updateScope = rememberCoroutineScope()
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = {
            if (updateState != "downloading") onDismiss()
        },
        shape = RoundedCornerShape(20.dp),
        title = { Text(stringResource(R.string.profile_update_title), fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (updateState) {
                    "" -> {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.profile_current_version, AppUpdater.getCurrentVersionName(context)),
                            fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                updateState = "checking"
                                updateScope.launch {
                                    val result = AppUpdater.checkUpdate(context)
                                    when (result) {
                                        is UpdateResult.Available -> {
                                            updateInfo = result.info
                                            updateState = "available"
                                        }
                                        is UpdateResult.UpToDate -> {
                                            updateState = "up_to_date"
                                        }
                                        is UpdateResult.Error -> {
                                            updateError = result.message
                                            updateState = "error"
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) { Text(stringResource(R.string.profile_check_update_btn)) }
                    }
                    "checking" -> {
                        Spacer(Modifier.height(12.dp))
                        CircularProgressIndicator(modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.profile_checking), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    "up_to_date" -> {
                        Spacer(Modifier.height(12.dp))
                        Text("✅", fontSize = 36.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.profile_up_to_date), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                    "available" -> {
                        val info = updateInfo ?: return@Column
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(R.string.profile_new_version_found, info.versionName), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        if (info.releaseNotes.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = info.releaseNotes,
                                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Column(modifier = Modifier.fillMaxWidth()) {
                            info.sources.forEach { src ->
                                val status = if (src.latencyMs >= 0) "${src.latencyMs}ms" else stringResource(R.string.profile_source_unavailable)
                                val statusColor = when {
                                    src.latencyMs < 0 -> ExpenseRed
                                    src.latencyMs < 500 -> IncomeGreen
                                    src.latencyMs < 2000 -> OrangeTint
                                    else -> ExpenseRed
                                }
                                Row(
                                    modifier = Modifier.padding(vertical = 1.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = src.label,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        text = status,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = statusColor
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    updateState = "downloading"
                                    updateScope.launch {
                                        val result = AppUpdater.downloadApk(context, info) { pct ->
                                            downloadProgress = pct
                                        }
                                        when (result) {
                                            is DownloadResult.Success -> {
                                                downloadedFile = result.file
                                                updateState = "downloaded"
                                            }
                                            is DownloadResult.Error -> {
                                                updateError = result.message
                                                updateState = "error"
                                            }
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) { Text(stringResource(R.string.profile_download_update)) }
                            Button(
                                onClick = { showManualUpdateDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) { Text(stringResource(R.string.profile_manual_update)) }
                        }
                        if (info.lanzouUrl.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { showManualDownloadDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(stringResource(R.string.profile_manual_download)) }
                        }
                    }
                    "downloading" -> {
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress / 100f },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.profile_downloading_progress, downloadProgress), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    "downloaded" -> {
                        Spacer(Modifier.height(12.dp))
                        Text("✅", fontSize = 36.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.profile_downloaded), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                downloadedFile?.let { file ->
                                    AppUpdater.installApk(context, file)
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) { Text(stringResource(R.string.profile_install)) }
                    }
                    "error" -> {
                        Spacer(Modifier.height(12.dp))
                        Text("❌", fontSize = 36.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.profile_update_error), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(4.dp))
                        Text(updateError, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { updateState = "" },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) { Text(stringResource(R.string.profile_retry)) }
                            Button(
                                onClick = { showManualUpdateDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) { Text(stringResource(R.string.profile_manual_update)) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (updateState != "downloading") {
                    onDismiss()
                }
            }) {
                Text(if (updateState == "downloading") stringResource(R.string.profile_downloading) else stringResource(R.string.profile_close), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )

    if (showManualDownloadDialog) {
        val info = updateInfo
        ManualDownloadDialog(
            url = info?.lanzouUrl ?: "",
            password = info?.lanzouPassword ?: "",
            onDismiss = { showManualDownloadDialog = false }
        )
    }

    if (showManualUpdateDialog) {
        ManualUpdateDialog(
            versionName = updateInfo?.versionName ?: "",
            lanzouUrl = updateInfo?.lanzouUrl ?: com.jizhang.tracker.data.AppConfig.APP_LANZOU_URL,
            lanzouPassword = updateInfo?.lanzouPassword ?: com.jizhang.tracker.data.AppConfig.APP_LANZOU_PASSWORD,
            onDismiss = { showManualUpdateDialog = false }
        )
    }
}
