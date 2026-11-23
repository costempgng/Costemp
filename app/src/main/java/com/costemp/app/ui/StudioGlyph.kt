package com.costemp.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size

/**
 * The studio icon set.
 *
 * These are vector paths drawn on a 24x24 style canvas rather than a font or bitmap so
 * the rail stays crisp at any density and adds no asset dependency.
 */
enum class StudioGlyph { ORBIT, PAN, ZOOM, WIREFRAME, GRID, RESET, SAVE, PANEL, UNDO, REDO }

@Composable
fun StudioGlyphIcon(
    glyph: StudioGlyph,
    modifier: Modifier = Modifier.size(20.dp),
    tint: Color = LocalContentColor.current,
) {
    Canvas(modifier) { drawGlyph(glyph, tint) }
}

private fun DrawScope.drawGlyph(glyph: StudioGlyph, tint: Color) {
    val w = size.width
    val h = size.height
    val strokeWidth = (size.minDimension * 0.085f).coerceAtLeast(1.2f)
    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun at(x: Float, y: Float) = Offset(x * w, y * h)

    when (glyph) {
        StudioGlyph.ORBIT -> {
            drawCircle(tint, radius = w * 0.19f, center = center, style = stroke)
            drawOval(
                tint,
                topLeft = at(0.06f, 0.35f),
                size = Size(w * 0.88f, h * 0.30f),
                style = stroke,
            )
            drawCircle(tint, radius = w * 0.065f, center = at(0.94f, 0.5f))
        }

        StudioGlyph.PAN -> {
            val c = center
            drawLine(tint, at(0.5f, 0.14f), at(0.5f, 0.86f), strokeWidth, StrokeCap.Round)
            drawLine(tint, at(0.14f, 0.5f), at(0.86f, 0.5f), strokeWidth, StrokeCap.Round)
            drawLine(tint, at(0.5f, 0.14f), at(0.37f, 0.28f), strokeWidth, StrokeCap.Round)
            drawLine(tint, at(0.5f, 0.14f), at(0.63f, 0.28f), strokeWidth, StrokeCap.Round)
            drawLine(tint, at(0.5f, 0.86f), at(0.37f, 0.72f), strokeWidth, StrokeCap.Round)
            drawLine(tint, at(0.5f, 0.86f), at(0.63f, 0.72f), strokeWidth, StrokeCap.Round)
            drawLine(tint, at(0.14f, 0.5f), at(0.28f, 0.37f), strokeWidth, StrokeCap.Round)
            drawLine(tint, at(0.14f, 0.5f), at(0.28f, 0.63f), strokeWidth, StrokeCap.Round)
            drawLine(tint, at(0.86f, 0.5f), at(0.72f, 0.37f), strokeWidth, StrokeCap.Round)
            drawLine(tint, at(0.86f, 0.5f), at(0.72f, 0.63f), strokeWidth, StrokeCap.Round)
            drawCircle(tint, radius = w * 0.06f, center = c)
        }

        StudioGlyph.ZOOM -> {
            drawCircle(tint, radius = w * 0.27f, center = at(0.42f, 0.42f), style = stroke)
            drawLine(tint, at(0.63f, 0.63f), at(0.88f, 0.88f), strokeWidth, StrokeCap.Round)
            drawLine(tint, at(0.28f, 0.42f), at(0.56f, 0.42f), strokeWidth * 0.85f, StrokeCap.Round)
            drawLine(tint, at(0.42f, 0.28f), at(0.42f, 0.56f), strokeWidth * 0.85f, StrokeCap.Round)
        }

        StudioGlyph.WIREFRAME -> {
            val path = Path().apply {
                moveTo(w * 0.5f, h * 0.1f)
                lineTo(w * 0.9f, h * 0.82f)
                lineTo(w * 0.1f, h * 0.82f)
                close()
            }
            drawPath(path, tint, style = stroke)
            drawLine(tint, at(0.5f, 0.1f), at(0.5f, 0.82f), strokeWidth * 0.7f, StrokeCap.Round)
            drawLine(tint, at(0.3f, 0.46f), at(0.7f, 0.46f), strokeWidth * 0.7f, StrokeCap.Round)
            drawLine(tint, at(0.3f, 0.46f), at(0.5f, 0.82f), strokeWidth * 0.7f, StrokeCap.Round)
            drawLine(tint, at(0.7f, 0.46f), at(0.5f, 0.82f), strokeWidth * 0.7f, StrokeCap.Round)
        }

        StudioGlyph.GRID -> {
            drawRect(tint, topLeft = at(0.12f, 0.12f), size = Size(w * 0.76f, h * 0.76f), style = stroke)
            drawLine(tint, at(0.12f, 0.45f), at(0.88f, 0.45f), strokeWidth * 0.7f, StrokeCap.Round)
            drawLine(tint, at(0.45f, 0.12f), at(0.45f, 0.88f), strokeWidth * 0.7f, StrokeCap.Round)
        }

        StudioGlyph.RESET -> {
            drawArc(
                tint,
                startAngle = -55f,
                sweepAngle = 300f,
                useCenter = false,
                topLeft = at(0.14f, 0.14f),
                size = Size(w * 0.72f, h * 0.72f),
                style = stroke,
            )
            drawLine(tint, at(0.72f, 0.08f), at(0.86f, 0.1f), strokeWidth, StrokeCap.Round)
            drawLine(tint, at(0.86f, 0.1f), at(0.84f, 0.26f), strokeWidth, StrokeCap.Round)
        }

        StudioGlyph.SAVE -> {
            drawRect(tint, topLeft = at(0.12f, 0.12f), size = Size(w * 0.76f, h * 0.76f), style = stroke)
            drawRect(
                tint,
                topLeft = at(0.3f, 0.58f),
                size = Size(w * 0.4f, h * 0.3f),
                style = Stroke(width = strokeWidth * 0.85f, cap = StrokeCap.Round),
            )
            drawRect(
                tint,
                topLeft = at(0.3f, 0.12f),
                size = Size(w * 0.4f, h * 0.24f),
                style = Stroke(width = strokeWidth * 0.85f, cap = StrokeCap.Round),
            )
        }

        StudioGlyph.PANEL -> {
            drawRect(tint, topLeft = at(0.12f, 0.16f), size = Size(w * 0.76f, h * 0.68f), style = stroke)
            drawLine(tint, at(0.64f, 0.16f), at(0.64f, 0.84f), strokeWidth * 0.8f, StrokeCap.Round)
        }

        StudioGlyph.UNDO -> drawHistoryArrow(tint, strokeWidth, mirrored = false)
        StudioGlyph.REDO -> drawHistoryArrow(tint, strokeWidth, mirrored = true)
    }
}

private fun DrawScope.drawHistoryArrow(tint: Color, strokeWidth: Float, mirrored: Boolean) {
    val w = size.width
    val h = size.height
    scale(if (mirrored) -1f else 1f, 1f, pivot = center) {
        drawArc(
            tint,
            startAngle = 155f,
            sweepAngle = 250f,
            useCenter = false,
            topLeft = Offset(w * 0.18f, h * 0.22f),
            size = Size(w * 0.64f, h * 0.6f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
        drawLine(tint, Offset(w * 0.18f, h * 0.42f), Offset(w * 0.18f, h * 0.18f), strokeWidth, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.18f, h * 0.18f), Offset(w * 0.42f, h * 0.18f), strokeWidth, StrokeCap.Round)
    }
}
