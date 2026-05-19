package com.example.billtracker.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun CountdownButton(
    countdown: Int,
    enabled: Boolean = countdown <= 0,
    onClick: () -> Unit,
    text: String = "知道了"
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Text(
            text = if (enabled) text else "($countdown) $text",
            color = Color.White,
            fontWeight = if (!enabled) FontWeight.Normal else FontWeight.SemiBold
        )
    }
}
