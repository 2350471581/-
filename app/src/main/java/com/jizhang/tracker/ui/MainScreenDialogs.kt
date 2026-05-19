package com.jizhang.tracker.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jizhang.tracker.data.PlanDataType
import com.jizhang.tracker.data.TransactionEntity
import com.jizhang.tracker.data.TransactionType
import com.jizhang.tracker.ui.components.AddTransactionDialog
import com.jizhang.tracker.ui.components.DateRangeFilterDialog
import com.jizhang.tracker.ui.components.FirstLaunchDialog
import com.jizhang.tracker.ui.components.TransactionDetailCard
import com.jizhang.tracker.ui.dialogs.AIChatTutorialDialog
import com.jizhang.tracker.ui.dialogs.AiChatEnabledDialog
import com.jizhang.tracker.ui.dialogs.BackupRestoreDialog
import com.jizhang.tracker.ui.dialogs.UpdateDialog
import kotlinx.coroutines.launch

@Composable
fun MainScreenDialogs(
    showAddDialog: Boolean,
    onDismissAddDialog: () -> Unit,
    onConfirmAdd: (Double, TransactionType, String) -> Unit,
    showAddPlanDialog: Boolean,
    onDismissAddPlanDialog: () -> Unit,
    onConfirmPlan: (String, Double, String, PlanDataType) -> Unit,
    showDateFilterDialog: Boolean,
    initialFilterStart: Long?,
    initialFilterEnd: Long?,
    onDateFilterConfirm: (Long?, Long?) -> Unit,
    onDateFilterReset: () -> Unit,
    onDismissDateFilter: () -> Unit,
    detailTransaction: TransactionEntity?,
    onDismissDetail: () -> Unit,
    onNoteSave: (Long, String) -> Unit,
    showManualPlanAlert: Boolean,
    onDismissManualPlanAlert: () -> Unit,
    showAIChatTutorial: Boolean,
    onDismissAIChatTutorial: () -> Unit,
    showAiChatProfileTutorial: Boolean,
    onDismissAiChatProfileTutorial: () -> Unit,
    showIntroDialog: Boolean,
    onDismissIntro: () -> Unit,
    showUpdateDialog: Boolean,
    onDismissUpdate: () -> Unit,
    showBackupDialog: Boolean,
    onDismissBackup: () -> Unit,
    onExportBackup: () -> Unit,
    onImportRestore: () -> Unit,
) {
    if (showAddPlanDialog) {
        AddPlanDialog(
            onDismiss = onDismissAddPlanDialog,
            onConfirm = onConfirmPlan
        )
    }

    if (showAddDialog) {
        AddTransactionDialog(
            onDismiss = onDismissAddDialog,
            onConfirm = onConfirmAdd
        )
    }

    if (showBackupDialog) {
        BackupRestoreDialog(
            onDismiss = onDismissBackup,
            onExportBackup = onExportBackup,
            onImportRestore = onImportRestore
        )
    }

    detailTransaction?.let { tx ->
        TransactionDetailCard(
            transaction = tx,
            onNoteSave = onNoteSave,
            onDismiss = onDismissDetail
        )
    }

    if (showManualPlanAlert) {
        AlertDialog(
            onDismissRequest = onDismissManualPlanAlert,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("提示", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            },
            text = {
                Text("请打开自动模式", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                Button(
                    onClick = onDismissManualPlanAlert,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("知道了")
                }
            }
        )
    }

    if (showAIChatTutorial) {
        AIChatTutorialDialog(
            onDismiss = onDismissAIChatTutorial
        )
    }

    if (showAiChatProfileTutorial) {
        AiChatEnabledDialog(
            onDismiss = onDismissAiChatProfileTutorial
        )
    }

    if (showIntroDialog) {
        FirstLaunchDialog(
            onDismiss = onDismissIntro
        )
    }

    if (showUpdateDialog) {
        UpdateDialog(onDismiss = onDismissUpdate)
    }

    if (showDateFilterDialog) {
        DateRangeFilterDialog(
            initialStart = initialFilterStart,
            initialEnd = initialFilterEnd,
            onConfirm = onDateFilterConfirm,
            onReset = onDateFilterReset,
            onDismiss = onDismissDateFilter
        )
    }
}
