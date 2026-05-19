package com.example.billtracker.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billtracker.data.TransactionSource
import com.example.billtracker.ui.AlipayBlue
import com.example.billtracker.ui.SubtleText
import com.example.billtracker.ui.WechatGreen

fun sourceDisplayName(source: TransactionSource): String = when (source) {
    TransactionSource.WECHAT -> "微信"
    TransactionSource.ALIPAY -> "支付宝"
    TransactionSource.BANK -> "银行"
    TransactionSource.MANUAL -> "其他"
}

fun sourceColor(source: TransactionSource): Color = when (source) {
    TransactionSource.WECHAT -> WechatGreen
    TransactionSource.ALIPAY -> AlipayBlue
    TransactionSource.BANK -> Color(0xFFE65100)
    TransactionSource.MANUAL -> SubtleText
}

@Composable
fun SourceLabel(
    source: TransactionSource,
    modifier: Modifier = Modifier
) {
    val color = sourceColor(source)
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.12f),
        modifier = modifier.semantics(mergeDescendants = true) { }
    ) {
        Text(
            text = sourceDisplayName(source),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}
