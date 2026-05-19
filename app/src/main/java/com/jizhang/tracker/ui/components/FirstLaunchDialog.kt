package com.jizhang.tracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import com.jizhang.tracker.R
import com.jizhang.tracker.ui.DarkTextColor
import com.jizhang.tracker.ui.SubtleText

@Composable
fun FirstLaunchDialog(
    onDismiss: () -> Unit
) {
    var countdown by remember { mutableIntStateOf(3) }
    val enabled = countdown <= 0

    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
    }

    Dialog(
        onDismissRequest = { if (enabled) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .fillMaxHeight(0.55f),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    Text(
                        text = stringResource(R.string.first_launch_welcome),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkTextColor
                    )
                    Spacer(Modifier.height(12.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        val features = listOf(
                            stringResource(R.string.first_launch_feature_manual_title) to stringResource(R.string.first_launch_feature_manual_desc),
                            stringResource(R.string.first_launch_feature_ai_title) to stringResource(R.string.first_launch_feature_ai_desc),
                            stringResource(R.string.first_launch_feature_chart_title) to stringResource(R.string.first_launch_feature_chart_desc),
                            stringResource(R.string.first_launch_feature_plan_title) to stringResource(R.string.first_launch_feature_plan_desc),
                            stringResource(R.string.first_launch_feature_theme_title) to stringResource(R.string.first_launch_feature_theme_desc),
                            stringResource(R.string.first_launch_feature_export_title) to stringResource(R.string.first_launch_feature_export_desc)
                        )
                        features.forEach { (title, desc) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("• ", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold)
                                Column {
                                    Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF3C4043))
                                    Text(desc, fontSize = 12.sp, color = SubtleText)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.first_launch_privacy_note),
                            fontSize = 12.sp,
                            color = SubtleText,
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onDismiss,
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = if (enabled) stringResource(R.string.first_launch_start) else stringResource(R.string.first_launch_start_countdown, countdown),
                            color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
