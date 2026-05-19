package com.jizhang.tracker.ui.dialogs

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jizhang.tracker.R
import com.jizhang.tracker.ui.CustomThemeConfig
import com.jizhang.tracker.ui.DarkSubtleText
import com.jizhang.tracker.ui.SubtleText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CustomThemeConfigDialog(
    config: CustomThemeConfig,
    onConfirm: (CustomThemeConfig) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf(config.imageUri) }
    var blurRadius by remember { mutableFloatStateOf(config.blurRadius) }
    var glassOpacity by remember { mutableFloatStateOf(config.glassOpacity) }
    var extractedPrimary by remember { mutableStateOf(Color(config.extractedPrimary)) }
    var isLoadingColor by remember { mutableStateOf(false) }
    var colorSwatches by remember { mutableStateOf(listOf<Color>()) }
    var swatchIndex by remember { mutableIntStateOf(0) }

    // Load bitmap and extract all palette swatches
    val previewBitmap = remember(imageUri) {
        if (imageUri.isNotBlank()) {
            try {
                val uri = Uri.parse(imageUri)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } catch (_: Exception) { null }
        } else null
    }

    // Extract palette when bitmap changes
    LaunchedEffect(previewBitmap) {
        if (previewBitmap != null) {
            isLoadingColor = true
            withContext(Dispatchers.IO) {
                val palette = androidx.palette.graphics.Palette.from(previewBitmap).generate()
                val swatches = mutableListOf<Color>()
                fun addIfNew(colorInt: Int) {
                    val c = Color(colorInt)
                    if (swatches.none { it.value == c.value }) swatches.add(c)
                }
                addIfNew(palette.getDominantColor(0xFFCDDBF7.toInt()))
                addIfNew(palette.getVibrantColor(0xFFCDDBF7.toInt()))
                addIfNew(palette.getLightVibrantColor(0xFFCDDBF7.toInt()))
                addIfNew(palette.getDarkVibrantColor(0xFFCDDBF7.toInt()))
                addIfNew(palette.getMutedColor(0xFFCDDBF7.toInt()))
                addIfNew(palette.getLightMutedColor(0xFFCDDBF7.toInt()))
                addIfNew(palette.getDarkMutedColor(0xFFCDDBF7.toInt()))
                colorSwatches = swatches
                swatchIndex = 0
                if (swatches.isNotEmpty()) extractedPrimary = swatches[0]
            }
            isLoadingColor = false
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { imageUri = it.toString() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.75f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    stringResource(R.string.custom_theme_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                // ── 背景图片预览 ──
                Text(stringResource(R.string.custom_theme_bg_image), fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    color = DarkSubtleText)
                Text(
                    text = stringResource(R.string.custom_theme_click_image_hint),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            if (colorSwatches.size > 1) {
                                swatchIndex = (swatchIndex + 1) % colorSwatches.size
                                extractedPrimary = colorSwatches[swatchIndex]
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (previewBitmap != null) {
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = stringResource(R.string.custom_theme_bg_preview_cd),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Glass overlay preview
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    extractedPrimary.copy(alpha = glassOpacity)
                                )
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(stringResource(R.string.custom_theme_select_image_hint),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        }
                    }
                }

                // 选择图片按钮
                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = extractedPrimary)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (imageUri.isNotBlank()) stringResource(R.string.custom_theme_change_image) else stringResource(R.string.custom_theme_select_image))
                }

                // ── 模糊半径 ──
                Text(stringResource(R.string.custom_theme_blur_radius, blurRadius.toInt()), fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    color = DarkSubtleText)
                Slider(
                    value = blurRadius,
                    onValueChange = { blurRadius = it },
                    valueRange = 0f..25f,
                    colors = SliderDefaults.colors(
                        thumbColor = extractedPrimary,
                        activeTrackColor = extractedPrimary
                    )
                )

                // ── 毛玻璃透明度 ──
                Text(stringResource(R.string.custom_theme_glass_opacity, (glassOpacity * 100).toInt()), fontSize = 13.sp,
                    fontWeight = FontWeight.Medium, color = DarkSubtleText)
                Slider(
                    value = glassOpacity,
                    onValueChange = { glassOpacity = it },
                    valueRange = 0f..0.8f,
                    colors = SliderDefaults.colors(
                        thumbColor = extractedPrimary,
                        activeTrackColor = extractedPrimary
                    )
                )

                val toastMsg = stringResource(R.string.custom_theme_click_image_toast)
                // ── 提取的主色调 ──
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        android.widget.Toast.makeText(context, toastMsg, android.widget.Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(stringResource(R.string.custom_theme_button_color), fontSize = 13.sp, fontWeight = FontWeight.Medium,
                        color = DarkSubtleText)
                    Spacer(Modifier.width(12.dp))
                    if (isLoadingColor) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = extractedPrimary
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = extractedPrimary,
                            modifier = Modifier.size(28.dp),
                            border = BorderStroke(2.dp, Color.White)
                        ) {}
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "#%06X".format(extractedPrimary.value.toLong() and 0xFFFFFF),
                            fontSize = 12.sp,
                            color = SubtleText
                        )
                    }
                }

                // ── 按钮 ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            // Reset
                            onConfirm(CustomThemeConfig())
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.custom_theme_reset)) }
                    Button(
                        onClick = {
                            onConfirm(
                                CustomThemeConfig(
                                    imageUri = imageUri,
                                    blurRadius = blurRadius,
                                    glassOpacity = glassOpacity,
                                    extractedPrimary = extractedPrimary.value.toLong()
                                )
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = extractedPrimary)
                    ) { Text(stringResource(R.string.custom_theme_confirm)) }
                }
            }
        }
    }
}
