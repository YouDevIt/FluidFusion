package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.game.*
import kotlin.math.sin

@Composable
fun PipeCellRenderer(
    cell: GridCell,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Infinite transition for fluid neon glow pulse and floating particles
    val infiniteTransition = rememberInfiniteTransition(label = "fluidPulse")
    
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    val flowOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flowOffset"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        
        // Steel casing thickness and neon core thickness
        val casingThickness = minOf(w, h) * 0.28f
        val fluidThickness = minOf(w, h) * 0.16f

        // Draw deep background based on cell type to match High Density theme
        val cellBgColor = when (cell.cellType) {
            is GridCellType.Source -> Color(0xFF2E3033)
            is GridCellType.Target -> Color(0xFF1A1C1E)
            else -> Color(0xFF232429)
        }
        drawRect(
            color = cellBgColor,
            size = size
        )

        // Draw sub-grid frame lines using theme borders color
        drawRect(
            color = Color(0xFF44474E),
            size = size,
            style = Stroke(width = 3f)
        )

        // Selected cell highlight using high density neon cyan
        if (isSelected) {
            drawRect(
                color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                size = size
            )
            drawRect(
                color = Color(0xFF00E5FF),
                size = size,
                style = Stroke(width = 6f)
            )
        }

        when (val ct = cell.cellType) {
            is GridCellType.Source -> {
                drawSource(cx, cy, w, h, ct.color, ct.emissionDir, glowScale)
            }
            is GridCellType.Target -> {
                drawTarget(cx, cy, w, h, ct.requiredColor, cell.actualColor, glowScale)
            }
            GridCellType.Normal -> {
                cell.pipe?.let { pipe ->
                    val r = pipe.rotation % 4
                    rotate(degrees = pipe.rotation * 90f, pivot = Offset(cx, cy)) {
                        drawPipeBody(cx, cy, w, h, pipe.type, casingThickness, fluidThickness, cell.actualColor, flowOffset, glowScale, cell.outFlows, pipe.rotation)
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawSource(
    cx: Float,
    cy: Float,
    w: Float,
    h: Float,
    color: FluidColor,
    direction: Direction,
    glowScale: Float
) {
    val r = minOf(w, h) * 0.35f
    
    // Draw outer generator casing
    drawCircle(
        color = Color(0xFF44474E),
        radius = r,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = Color(0xFF37474F),
        radius = r * 0.8f,
        center = Offset(cx, cy)
    )

    // Glowing emitter core
    val activeColor = color.toNeonColor()
    drawCircle(
        color = activeColor.copy(alpha = 0.3f * glowScale),
        radius = r * 0.7f * glowScale,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = activeColor,
        radius = r * 0.5f,
        center = Offset(cx, cy)
    )

    // Emission outlet nozzle (a small metal pipe extending in emission direction)
    val nozzleLen = w * 0.22f
    val outWidth = w * 0.28f
    val outX = cx + direction.dx * (r + nozzleLen / 2)
    val outY = cy + direction.dy * (r + nozzleLen / 2)

    val sizeNozzle = if (direction.dx != 0) Size(nozzleLen, outWidth) else Size(outWidth, nozzleLen)
    drawRect(
        color = Color(0xFF546E7A),
        topLeft = Offset(outX - sizeNozzle.width / 2, outY - sizeNozzle.height / 2),
        size = sizeNozzle
    )

    // Inner emitting neon canal
    val flowSize = if (direction.dx != 0) Size(nozzleLen + 10f, outWidth * 0.5f) else Size(outWidth * 0.5f, nozzleLen + 10f)
    drawRect(
        color = activeColor,
        topLeft = Offset(outX - flowSize.width / 2, outY - flowSize.height / 2),
        size = flowSize
    )
}

private fun DrawScope.drawTarget(
    cx: Float,
    cy: Float,
    w: Float,
    h: Float,
    requiredColor: FluidColor,
    actualColor: FluidColor,
    glowScale: Float
) {
    val sizeTank = minOf(w, h) * 0.8f
    val tankLeft = cx - sizeTank / 2f
    val tankTop = cy - sizeTank / 2f

    val reqNeonColor = requiredColor.toNeonColor()

    // Draw square-beaker metal skeleton
    drawRoundRect(
        color = reqNeonColor.copy(alpha = 0.8f),
        topLeft = Offset(tankLeft, tankTop),
        size = Size(sizeTank, sizeTank),
        cornerRadius = CornerRadius(12f, 12f),
        style = Stroke(width = 12f)
    )

    // Hollow core background
    drawRoundRect(
        color = Color(0xFF10171D),
        topLeft = Offset(tankLeft + 6f, tankTop + 6f),
        size = Size(sizeTank - 12f, sizeTank - 12f),
        cornerRadius = CornerRadius(8f, 8f)
    )

    // Label of required color
    val reqColorStr = requiredColor.nameInItalian().uppercase()

    // Required color glowing indicator dots/rings inside
    drawCircle(
        color = reqNeonColor.copy(alpha = 0.6f),
        radius = sizeTank * 0.35f * glowScale,
        center = Offset(cx, cy),
        style = Stroke(width = 6f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f))
    )
    
    // Solid inner indicator
    drawCircle(
        color = reqNeonColor.copy(alpha = 0.3f),
        radius = sizeTank * 0.2f,
        center = Offset(cx, cy)
    )

    // Fill fluid according to incoming color
    if (!actualColor.isEmpty) {
        val filledFraction = if (actualColor == requiredColor) 0.85f else 0.55f
        val fillHeight = (sizeTank - 20f) * filledFraction
        val fillWidth = sizeTank - 20f
        val fillColor = actualColor.toNeonColor()

        drawRoundRect(
            color = fillColor.copy(alpha = 0.4f),
            topLeft = Offset(tankLeft + 10f, tankTop + sizeTank - 10f - fillHeight),
            size = Size(fillWidth, fillHeight),
            cornerRadius = CornerRadius(6f, 6f)
        )

        // Liquid bubbling effect (little circles of matching color)
        for (i in 0..3) {
            val bx = tankLeft + 20f + (fillWidth * 0.2f * i + (10f * sin(flowOffsetPlusSeed(i)))) % (fillWidth - 20f)
            val by = tankTop + sizeTank - 15f - (fillHeight * 0.8f * ((i + 1) * 0.25f))
            drawCircle(
                color = fillColor.copy(alpha = 0.7f),
                radius = 5f,
                center = Offset(bx, by)
            )
        }
    }

    // Success glowing border if satisfied exactly
    if (actualColor == requiredColor) {
        drawRoundRect(
            color = Color(0xFF00E676).copy(alpha = 0.25f * glowScale),
            topLeft = Offset(tankLeft - 4f, tankTop - 4f),
            size = Size(sizeTank + 8f, sizeTank + 8f),
            cornerRadius = CornerRadius(16f, 16f)
        )
        drawRoundRect(
            color = Color(0xFF00E676),
            topLeft = Offset(tankLeft - 4f, tankTop - 4f),
            size = Size(sizeTank + 8f, sizeTank + 8f),
            cornerRadius = CornerRadius(16f, 16f),
            style = Stroke(width = 4f)
        )
    } else {
        // Draw dashed target info color
        drawRoundRect(
            color = reqNeonColor,
            topLeft = Offset(tankLeft + 4f, tankTop + 4f),
            size = Size(sizeTank - 8f, sizeTank - 8f),
            cornerRadius = CornerRadius(10f, 10f),
            style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f))
        )
    }
}

private fun flowOffsetPlusSeed(seed: Int): Float {
    return (System.currentTimeMillis() / 250f + seed * 1.57f)
}

private fun DrawScope.drawPipeBody(
    cx: Float,
    cy: Float,
    w: Float,
    h: Float,
    type: PipeType,
    casingT: Float,
    fluidT: Float,
    fluidColor: FluidColor,
    flowOffset: Float,
    glowScale: Float,
    outFlows: Map<Direction, FluidColor>,
    gridRotation: Int
) {
    val isFluidActive = !fluidColor.isEmpty
    val casingColor = Color(0xFF44474E)
    val innerTrackColor = Color(0xFF10161C)
    val neonColor = fluidColor.toNeonColor()

    when (type) {
        PipeType.STRAIGHT -> {
            // Standard straight points UP-DOWN under 0 rotation
            // 1. Draw outer casing
            drawLine(color = casingColor, start = Offset(cx, 0f), end = Offset(cx, h), strokeWidth = casingT, cap = StrokeCap.Round)
            // 2. Draw hollow tracks
            drawLine(color = innerTrackColor, start = Offset(cx, 0f), end = Offset(cx, h), strokeWidth = fluidT, cap = StrokeCap.Round)
            
            // 3. Draw flowing liquid
            if (isFluidActive) {
                drawLine(color = neonColor, start = Offset(cx, 0f), end = Offset(cx, h), strokeWidth = fluidT * 0.8f, cap = StrokeCap.Round)
                // Flow dynamic pulse line
                drawLine(color = Color.White.copy(alpha = 0.5f), start = Offset(cx, flowOffset % h), end = Offset(cx, (flowOffset + h * 0.15f) % h), strokeWidth = fluidT * 0.25f, cap = StrokeCap.Round)
            }
        }
        PipeType.ELBOW -> {
            // Standard elbow is UP-RIGHT under 0 rotation
            // 1. Draw outer casing lines meeting at center
            drawLine(color = casingColor, start = Offset(cx, 0f), end = Offset(cx, cy), strokeWidth = casingT, cap = StrokeCap.Square)
            drawLine(color = casingColor, start = Offset(cx, cy), end = Offset(w, cy), strokeWidth = casingT, cap = StrokeCap.Square)

            // Corner joint cosmetic circle
            drawCircle(color = casingColor, radius = casingT / 2f, center = Offset(cx, cy))

            // 2. Draw hollow tracks
            drawLine(color = innerTrackColor, start = Offset(cx, 0f), end = Offset(cx, cy), strokeWidth = fluidT, cap = StrokeCap.Square)
            drawLine(color = innerTrackColor, start = Offset(cx, cy), end = Offset(w, cy), strokeWidth = fluidT, cap = StrokeCap.Square)
            drawCircle(color = innerTrackColor, radius = fluidT / 2f, center = Offset(cx, cy))

            if (isFluidActive) {
                // 3. Draw neon fluid
                drawLine(color = neonColor, start = Offset(cx, 0f), end = Offset(cx, cy), strokeWidth = fluidT * 0.8f, cap = StrokeCap.Square)
                drawLine(color = neonColor, start = Offset(cx, cy), end = Offset(w, cy), strokeWidth = fluidT * 0.8f, cap = StrokeCap.Square)
                drawCircle(color = neonColor, radius = (fluidT / 2f) * 0.8f, center = Offset(cx, cy))
                
                // Draw flowing beads
                val flowPoint = flowOffset % (cx + cy)
                if (flowPoint < cy) {
                    drawCircle(color = Color.White.copy(alpha = 0.6f), radius = fluidT * 0.2f, center = Offset(cx, flowPoint))
                } else {
                    drawCircle(color = Color.White.copy(alpha = 0.6f), radius = fluidT * 0.2f, center = Offset(cx + (flowPoint - cy), cy))
                }
            }
        }
        PipeType.TEE -> {
            // Standard Tee has LEFT, UP, RIGHT under 0 rotation (no DOWN)
            // 1. Casing
            drawLine(color = casingColor, start = Offset(0f, cy), end = Offset(w, cy), strokeWidth = casingT, cap = StrokeCap.Round)
            drawLine(color = casingColor, start = Offset(cx, 0f), end = Offset(cx, cy), strokeWidth = casingT, cap = StrokeCap.Square)
            drawCircle(color = casingColor, radius = casingT / 2f, center = Offset(cx, cy))

            // 2. Tracks
            drawLine(color = innerTrackColor, start = Offset(0f, cy), end = Offset(w, cy), strokeWidth = fluidT, cap = StrokeCap.Round)
            drawLine(color = innerTrackColor, start = Offset(cx, 0f), end = Offset(cx, cy), strokeWidth = fluidT, cap = StrokeCap.Square)
            drawCircle(color = innerTrackColor, radius = fluidT / 2f, center = Offset(cx, cy))

            if (isFluidActive) {
                // 3. Neon fluid
                drawLine(color = neonColor, start = Offset(0f, cy), end = Offset(w, cy), strokeWidth = fluidT * 0.8f, cap = StrokeCap.Round)
                drawLine(color = neonColor, start = Offset(cx, 0f), end = Offset(cx, cy), strokeWidth = fluidT * 0.8f, cap = StrokeCap.Square)
                drawCircle(color = neonColor, radius = (fluidT / 2f) * 0.8f, center = Offset(cx, cy))
            }
        }
        PipeType.CROSS -> {
            // Standard cross links all 4 directions, non-mixing
            // 1. Casing (horizontal first, then vertical)
            drawLine(color = casingColor, start = Offset(0f, cy), end = Offset(w, cy), strokeWidth = casingT, cap = StrokeCap.Round)
            drawLine(color = casingColor, start = Offset(cx, 0f), end = Offset(cx, h), strokeWidth = casingT, cap = StrokeCap.Round)

            // Calculate colors based on outFlows
            // Canvas Y represents Grid UP/DOWN if rotation % 2 == 0
            // Canvas X represents Grid LEFT/RIGHT if rotation % 2 == 0
            val r = gridRotation % 4
            val gridUpDownColor = outFlows[Direction.DOWN] ?: outFlows[Direction.UP] ?: FluidColor()
            val gridLeftRightColor = outFlows[Direction.RIGHT] ?: outFlows[Direction.LEFT] ?: FluidColor()

            val canvasYColor = (if (r == 0 || r == 2) gridUpDownColor else gridLeftRightColor).toNeonColor()
            val canvasXColor = (if (r == 0 || r == 2) gridLeftRightColor else gridUpDownColor).toNeonColor()

            val hasCanvasYFluid = (if (r == 0 || r == 2) !gridUpDownColor.isEmpty else !gridLeftRightColor.isEmpty)
            val hasCanvasXFluid = (if (r == 0 || r == 2) !gridLeftRightColor.isEmpty else !gridUpDownColor.isEmpty)

            // Inner casing/tracks
            drawLine(color = innerTrackColor, start = Offset(0f, cy), end = Offset(w, cy), strokeWidth = fluidT, cap = StrokeCap.Round)
            drawLine(color = innerTrackColor, start = Offset(cx, 0f), end = Offset(cx, h), strokeWidth = fluidT, cap = StrokeCap.Round)

            // X-axis (Horizontal) fluid
            if (hasCanvasXFluid) {
                drawLine(color = canvasXColor, start = Offset(0f, cy), end = Offset(w, cy), strokeWidth = fluidT * 0.8f, cap = StrokeCap.Round)
            }
            
            // Y-axis (Vertical) fluid - drawn over horizontal to show non-mixing layering
            // Small casing bridge
            drawRect(
                color = innerTrackColor,
                topLeft = Offset(cx - casingT / 2f, cy - casingT / 3f),
                size = Size(casingT, casingT * 0.66f)
            )
            drawLine(color = innerTrackColor, start = Offset(cx, 0f), end = Offset(cx, h), strokeWidth = fluidT, cap = StrokeCap.Round)

            if (hasCanvasYFluid) {
                drawLine(color = canvasYColor, start = Offset(cx, 0f), end = Offset(cx, h), strokeWidth = fluidT * 0.8f, cap = StrokeCap.Round)
            }
            
            // Bridge connector decor
            drawRoundRect(
                color = casingColor,
                topLeft = Offset(cx - casingT * 0.35f, cy - casingT * 0.45f),
                size = Size(casingT * 0.7f, casingT * 0.9f),
                cornerRadius = CornerRadius(4f, 4f),
                style = Stroke(width = 3f)
            )
        }
        PipeType.VALVE -> {
            // Valve has input from DOWN, output to UP (flow is UP under 0 rotation)
            // 1. Draw central tube casing
            drawLine(color = casingColor, start = Offset(cx, 0f), end = Offset(cx, h), strokeWidth = casingT, cap = StrokeCap.Round)
            
            // Draw valve regulator collar/box in the middle
            drawRect(
                color = Color(0xFF37474F),
                topLeft = Offset(cx - casingT * 0.8f, cy - casingT * 0.5f),
                size = Size(casingT * 1.6f, casingT)
            )
            drawRect(
                color = Color(0xFF78909C),
                topLeft = Offset(cx - casingT * 0.8f, cy - casingT * 0.5f),
                size = Size(casingT * 1.6f, casingT),
                style = Stroke(width = 4f)
            )

            // Inner tracks
            drawLine(color = innerTrackColor, start = Offset(cx, 0f), end = Offset(cx, h), strokeWidth = fluidT, cap = StrokeCap.Round)

            if (isFluidActive) {
                // Glow core
                drawLine(color = neonColor, start = Offset(cx, 0f), end = Offset(cx, h), strokeWidth = fluidT * 0.8f, cap = StrokeCap.Round)
            }

            // Draw beautiful flow-direction indicator arrow pointing UP (▲)
            val arrowW = casingT * 0.8f
            val arrowH = casingT * 0.6f
            val path = Path().apply {
                moveTo(cx, cy - arrowH / 2f) // tip pointing UP
                lineTo(cx - arrowW / 2f, cy + arrowH / 2f)
                lineTo(cx + arrowW / 2f, cy + arrowH / 2f)
                close()
            }
            
            val arrowColor = if (isFluidActive) neonColor else Color(0xFFFFFFFF)
            drawPath(
                path = path,
                color = arrowColor
            )
            
            if (isFluidActive) {
                // Glow boundary for key check valves
                drawPath(
                    path = path,
                    color = arrowColor.copy(alpha = 0.3f * glowScale),
                    style = Stroke(width = 6f * glowScale)
                )
            }
        }
    }
}
