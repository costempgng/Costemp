package com.costemp.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.costemp.app.ui.theme.CostempPalette

/** Monospaced technical readout style used by the status bar and numeric fields. */
val TechnicalText = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 11.sp,
    letterSpacing = 0.2.sp,
)

val SectionLabel = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontSize = 10.sp,
    letterSpacing = 1.4.sp,
    fontWeight = FontWeight.SemiBold,
)

@Composable
fun Chip(text: String, modifier: Modifier = Modifier, color: Color = CostempPalette.TextSecondary) {
    Text(
        text = text,
        modifier = modifier
            .background(Color(0xCC111116), RoundedCornerShape(6.dp))
            .border(1.dp, CostempPalette.Outline, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        style = TechnicalText.copy(fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Medium),
        color = color,
    )
}
