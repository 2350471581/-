package com.jizhang.tracker.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.jizhang.tracker.ui.CustomThemeConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CustomThemeBackground(
    config: CustomThemeConfig,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var blurredBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(config.imageUri, config.blurRadius) {
        blurredBitmap = if (config.imageUri.isNotBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val uri = Uri.parse(config.imageUri)
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val original = BitmapFactory.decodeStream(stream)
                        if (original != null) blurBitmap(original, config.blurRadius)
                        else null
                    }
                } catch (_: Exception) { null }
            }
        } else null
    }

    val overlayColor = remember(config.extractedPrimary, config.glassOpacity) {
        Color(config.extractedPrimary).copy(alpha = config.glassOpacity)
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (blurredBitmap != null) {
            Image(
                bitmap = blurredBitmap!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(overlayColor)
        )
        content()
    }
}

private fun blurBitmap(original: Bitmap, radius: Float): Bitmap {
    val srcW = original.width
    val srcH = original.height

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && radius > 0f) {
        val scaledW = (srcW * 0.25f).toInt().coerceAtLeast(1)
        val scaledH = (srcH * 0.25f).toInt().coerceAtLeast(1)
        val small = Bitmap.createScaledBitmap(original, scaledW, scaledH, true)

        val blurredSmall = Bitmap.createBitmap(small.width, small.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(blurredSmall)
        val paint = android.graphics.Paint()
        paint.isAntiAlias = true
        canvas.drawBitmap(small, 0f, 0f, paint)

        val result = Bitmap.createScaledBitmap(blurredSmall, srcW, srcH, true)

        small.recycle()
        blurredSmall.recycle()
        if (original != result) original.recycle()

        return result
    }

    // Fallback for API < 31
    val scale = (1f / (1f + radius * 0.4f)).coerceIn(0.05f, 1f)
    val smallW = (srcW * scale).toInt().coerceAtLeast(1)
    val smallH = (srcH * scale).toInt().coerceAtLeast(1)

    val small = Bitmap.createScaledBitmap(original, smallW, smallH, true)
    val blurred = Bitmap.createScaledBitmap(small, srcW, srcH, true)

    if (small != blurred) small.recycle()
    if (original != blurred) original.recycle()

    return blurred
}
