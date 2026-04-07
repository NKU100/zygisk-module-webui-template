package io.github.nku100.webui.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.capsule.ContinuousRoundedRectangle
import top.yukonga.miuix.kmp.basic.Text

@Composable
fun StatusTag(
    label: String,
    backgroundColor: Color,
    contentColor: Color,
) {
    Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = FontWeight(750),
        color = contentColor,
        modifier = Modifier
            .clip(ContinuousRoundedRectangle(6.dp))
            .background(backgroundColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
