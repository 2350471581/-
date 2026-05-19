package com.jizhang.tracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jizhang.tracker.R
import com.jizhang.tracker.data.TransactionType
import com.jizhang.tracker.ui.ExpenseRed

@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, type: TransactionType, note: String) -> Unit
) {
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf(false) }

    val expenseCd = stringResource(R.string.add_tx_expense_cd)
    val incomeCd = stringResource(R.string.add_tx_income_cd)
    val amountInputCd = stringResource(R.string.add_tx_amount_input_cd)
    val noteInputCd = stringResource(R.string.add_tx_note_input_cd)
    val confirmCd = stringResource(R.string.add_tx_confirm_cd)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(stringResource(R.string.add_tx_title), fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = type == TransactionType.EXPENSE,
                        onClick = { type = TransactionType.EXPENSE },
                        label = { Text(stringResource(R.string.expense)) },
                        modifier = Modifier.weight(1f).semantics { contentDescription = expenseCd },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            selectedLabelColor = MaterialTheme.colorScheme.error
                        )
                    )
                    FilterChip(
                        selected = type == TransactionType.INCOME,
                        onClick = { type = TransactionType.INCOME },
                        label = { Text(stringResource(R.string.income)) },
                        modifier = Modifier.weight(1f).semantics { contentDescription = incomeCd },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; amountError = false },
                    label = { Text(stringResource(R.string.add_tx_amount)) },
                    placeholder = { Text("0.00") },
                    leadingIcon = { Text("¥", fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = amountError,
                    supportingText = if (amountError) {{ Text(stringResource(R.string.add_tx_amount_error), color = ExpenseRed) }} else null,
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = amountInputCd },
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.add_tx_note)) },
                    placeholder = { Text(stringResource(R.string.add_tx_note_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = noteInputCd },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        amountError = true
                    } else {
                        onConfirm(amount, type, note)
                    }
                },
                modifier = Modifier.semantics { contentDescription = confirmCd },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.add_tx_confirm), modifier = Modifier.padding(horizontal = 8.dp))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.add_tx_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
