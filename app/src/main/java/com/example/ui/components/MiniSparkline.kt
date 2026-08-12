package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * High-performance smooth canvas sparkline for market price preview cards.
 */
@Composable
fun MiniSparkline(
    data: List<Double>,
    lineColor: Color,
    modifier: Modifier = Modifier,
    fillGradient: Boolean = true,
    strokeWidth: Float = 2.5f
) {
    if (data.size < 2) {
        Canvas(modifier = modifier) {
            val y = size.height / 2
            drawLine(
                color = lineColor.copy(alpha = 0.3f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = strokeWidth
            )
        }
        return
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val minVal = data.minOrNull() ?: 0.0
        val maxVal = data.maxOrNull() ?: 1.0
        val range = if (maxVal == minVal) 1.0 else maxVal - minVal

        val points = data.mapIndexed { index, value ->
            val x = (index.toFloat() / (data.size - 1)) * width
            val normalizedY = ((value - minVal) / range).toFloat()
            // Invert Y so highest price is at top
            val y = height - (normalizedY * (height * 0.8f) + (height * 0.1f))
            Offset(x, y)
        }

        // Draw gradient fill area under the line if requested
        if (fillGradient && points.isNotEmpty()) {
            val fillPath = Path().apply {
                moveTo(points.first().x, height)
                lineTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    val prev = points[i - 1]
                    val curr = points[i]
                    val cx = (prev.x + curr.x) / 2
                    cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
                }
                lineTo(points.last().x, height)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = 0.25f),
                        lineColor.copy(alpha = 0.02f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = height
                )
            )
        }

        // Draw smooth bezier line
        val strokePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                val prev = points[i - 1]
                val curr = points[i]
                val cx = (prev.x + curr.x) / 2
                cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
            }
        }

        drawPath(
            path = strokePath,
            color = lineColor,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Draw active pulsing endpoint dot
        val lastPoint = points.last()
        drawCircle(
            color = lineColor,
            radius = strokeWidth * 1.5f,
            center = lastPoint
        )
    }
}
