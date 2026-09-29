package com.example.ui

import android.graphics.Paint as AndroidPaint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.model.AimTrajectory
import com.example.model.BubbleColor
import com.example.model.FloatingText
import com.example.model.Particle

object BubblePainter {

    // Pre-allocated static PathEffects to avoid runtime allocations during draw
    private val trajectoryDashEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
    private val dangerDashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
    private val targetHitRingEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
    private val stroke15 = Stroke(width = 1.5f)
    private val stroke25Dash = Stroke(width = 2.5f, pathEffect = targetHitRingEffect)

    // Pre-allocated Android text paint for floating text
    private val textPaint = AndroidPaint().apply {
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = AndroidPaint.Align.CENTER
    }

    // Cached pre-rendered bubble bitmaps for ultra-fast GPU rendering
    private var cachedRadius: Float = 0f
    private val bitmapCache = HashMap<BubbleColor, ImageBitmap>()

    private fun ensureBitmaps(radius: Float) {
        if (radius <= 0f) return
        if (radius == cachedRadius && bitmapCache.isNotEmpty()) return

        cachedRadius = radius
        bitmapCache.clear()

        val size = (radius * 2f).toInt().coerceAtLeast(1)

        for (color in BubbleColor.entries) {
            val bitmap = ImageBitmap(size, size)
            val canvas = Canvas(bitmap)
            val center = Offset(radius, radius)
            val r = radius * 0.92f

            // 1. Drop shadow
            val shadowPaint = androidx.compose.ui.graphics.Paint().apply {
                this.color = Color.Black.copy(alpha = 0.25f)
            }
            canvas.drawCircle(center + Offset(0f, r * 0.12f), r * 0.95f, shadowPaint)

            // 2. Main spherical radial gradient
            val mainGradient = Brush.radialGradient(
                colors = listOf(color.light, color.primary, color.dark),
                center = center - Offset(r * 0.25f, r * 0.25f),
                radius = r * 1.15f
            )
            val mainPaint = androidx.compose.ui.graphics.Paint()
            mainGradient.applyTo(Size(size.toFloat(), size.toFloat()), mainPaint, 1f)
            canvas.drawCircle(center, r, mainPaint)

            // 3. Subtle dark rim
            val rimPaint = androidx.compose.ui.graphics.Paint().apply {
                this.color = color.dark.copy(alpha = 0.5f)
                this.style = androidx.compose.ui.graphics.PaintingStyle.Stroke
                this.strokeWidth = 1.5f
            }
            canvas.drawCircle(center, r, rimPaint)

            // 4. Specular glossy highlight
            val highlightCenter = center - Offset(r * 0.32f, r * 0.32f)
            val highlightRadius = r * 0.32f
            val highlightGradient = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.88f),
                    Color.White.copy(alpha = 0.35f),
                    Color.Transparent
                ),
                center = highlightCenter,
                radius = highlightRadius
            )
            val highlightPaint = androidx.compose.ui.graphics.Paint()
            highlightGradient.applyTo(Size(size.toFloat(), size.toFloat()), highlightPaint, 1f)
            canvas.drawCircle(highlightCenter, highlightRadius, highlightPaint)

            // 5. Bottom-right inner reflection
            val reflectionCenter = center + Offset(r * 0.28f, r * 0.28f)
            val reflectionRadius = r * 0.26f
            val reflectionGradient = Brush.radialGradient(
                colors = listOf(color.light.copy(alpha = 0.45f), Color.Transparent),
                center = reflectionCenter,
                radius = reflectionRadius
            )
            val refPaint = androidx.compose.ui.graphics.Paint()
            reflectionGradient.applyTo(Size(size.toFloat(), size.toFloat()), refPaint, 1f)
            canvas.drawCircle(reflectionCenter, reflectionRadius, refPaint)

            bitmapCache[color] = bitmap
        }
    }

    fun drawGlossyBubble(
        drawScope: DrawScope,
        center: Offset,
        radius: Float,
        color: BubbleColor,
        alpha: Float = 1f,
        scale: Float = 1f
    ) {
        if (radius <= 0f || alpha <= 0f) return
        ensureBitmaps(radius)

        val bitmap = bitmapCache[color] ?: return

        with(drawScope) {
            if (scale == 1f) {
                // Fast path: direct 1:1 image blit
                drawImage(
                    image = bitmap,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    alpha = alpha.coerceIn(0f, 1f)
                )
            } else {
                // Scaled blit (e.g. popping bubble)
                val targetRadius = radius * scale
                val targetSize = (targetRadius * 2f).toInt().coerceAtLeast(1)
                drawImage(
                    image = bitmap,
                    dstOffset = IntOffset(
                        (center.x - targetRadius).toInt(),
                        (center.y - targetRadius).toInt()
                    ),
                    dstSize = IntSize(targetSize, targetSize),
                    alpha = alpha.coerceIn(0f, 1f)
                )
            }
        }
    }

    fun drawAimTrajectory(
        drawScope: DrawScope,
        trajectory: AimTrajectory,
        bubbleColor: BubbleColor,
        bubbleRadius: Float
    ) {
        val points = trajectory.points
        if (points.size < 2) return

        with(drawScope) {
            for (i in 0 until points.size - 1) {
                val start = points[i]
                val end = points[i + 1]

                // Outer glow line
                drawLine(
                    color = bubbleColor.glow.copy(alpha = 0.4f),
                    start = start,
                    end = end,
                    strokeWidth = 6f,
                    pathEffect = trajectoryDashEffect
                )
                // Sharp inner guide line
                drawLine(
                    color = Color.White.copy(alpha = 0.9f),
                    start = start,
                    end = end,
                    strokeWidth = 2.5f,
                    pathEffect = trajectoryDashEffect
                )
            }

            // Target hit circle indicator
            trajectory.targetHitPoint?.let { hit ->
                drawCircle(
                    color = bubbleColor.glow.copy(alpha = 0.35f),
                    radius = bubbleRadius,
                    center = hit
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    radius = bubbleRadius,
                    center = hit,
                    style = stroke25Dash
                )
            }
        }
    }

    fun drawCeilingBar(
        drawScope: DrawScope,
        startX: Float,
        endX: Float,
        y: Float,
        isWarning: Boolean,
        shotsUntilDescent: Int
    ) {
        with(drawScope) {
            val barHeight = 14f
            val top = y - barHeight

            // Base metallic bar
            drawRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(startX, top),
                size = Size(endX - startX, barHeight)
            )

            // Warning diagonal hazard stripes
            val stripeColor = if (isWarning || shotsUntilDescent <= 1) Color(0xFFEF4444) else Color(0xFFF59E0B)
            val stripeWidth = 14f
            var currX = startX
            while (currX < endX) {
                drawRect(
                    color = stripeColor.copy(alpha = 0.85f),
                    topLeft = Offset(currX, top),
                    size = Size(stripeWidth, barHeight)
                )
                currX += stripeWidth * 2f
            }

            // Lower rim steel edge
            drawLine(
                color = Color(0xFF94A3B8),
                start = Offset(startX, y),
                end = Offset(endX, y),
                strokeWidth = 2.5f
            )

            // Shadow under ceiling
            drawLine(
                color = Color.Black.copy(alpha = 0.4f),
                start = Offset(startX, y + 2f),
                end = Offset(endX, y + 2f),
                strokeWidth = 3f
            )

            // Warning LED indicator beacons
            val beaconY = top + barHeight / 2f
            val ledColor = if (isWarning || shotsUntilDescent <= 1) Color(0xFFEF4444) else Color(0xFF10B981)
            drawCircle(color = ledColor, radius = 3.5f, center = Offset(startX + 14f, beaconY))
            drawCircle(color = ledColor, radius = 3.5f, center = Offset(endX - 14f, beaconY))
            drawCircle(color = ledColor, radius = 3.5f, center = Offset((startX + endX) / 2f, beaconY))
        }
    }

    fun drawDangerLine(
        drawScope: DrawScope,
        startX: Float,
        endX: Float,
        y: Float,
        isNearDanger: Boolean = false
    ) {
        with(drawScope) {
            val color = if (isNearDanger) Color(0xFFFF1E1E) else Color(0xFFEF4444).copy(alpha = 0.6f)
            val strokeW = if (isNearDanger) 4.5f else 3f
            drawLine(
                color = color,
                start = Offset(startX, y),
                end = Offset(endX, y),
                strokeWidth = strokeW,
                pathEffect = dangerDashEffect
            )
        }
    }

    fun drawParticles(
        drawScope: DrawScope,
        particles: List<Particle>
    ) {
        if (particles.isEmpty()) return
        with(drawScope) {
            particles.forEach { p ->
                drawCircle(
                    color = p.color.copy(alpha = p.life.coerceIn(0f, 1f)),
                    radius = p.radius * p.life,
                    center = Offset(p.x, p.y)
                )
            }
        }
    }

    fun drawFloatingTexts(
        drawScope: DrawScope,
        floatingTexts: List<FloatingText>
    ) {
        if (floatingTexts.isEmpty()) return
        with(drawScope) {
            drawContext.canvas.nativeCanvas.let { native ->
                floatingTexts.forEach { item ->
                    textPaint.color = item.color.toArgb()
                    textPaint.alpha = (item.alpha * 255).toInt().coerceIn(0, 255)
                    textPaint.textSize = if (item.isCombo) 48f else 38f
                    native.drawText(item.text, item.x, item.y, textPaint)
                }
            }
        }
    }
}
