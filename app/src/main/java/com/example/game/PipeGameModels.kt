package com.example.game

import androidx.compose.ui.graphics.Color

// Represents a fluid color in RGB additive space (e.g. Red + Green = Yellow)
data class FluidColor(
    val r: Boolean = false,
    val g: Boolean = false,
    val b: Boolean = false
) {
    val isEmpty: Boolean get() = !r && !g && !b

    fun mix(other: FluidColor): FluidColor {
        return FluidColor(r = this.r || other.r, g = this.g || other.g, b = this.b || other.b)
    }

    fun toComposeColor(): Color {
        return when {
            r && g && b -> Color(0xFFE0E0E0) // White (Clean light grey)
            r && g -> Color(0xFFFFD54F)      // Yellow
            r && b -> Color(0xFFBA68C8)      // Magenta / Purple
            g && b -> Color(0xFF4DD0E1)      // Cyan
            r -> Color(0xFFE57373)           // Pure Red
            g -> Color(0xFF81C784)           // Pure Green
            b -> Color(0xFF64B5F6)           // Pure Blue
            else -> Color(0xFF424242)        // Dark grey empty fluid outline
        }
    }

    fun toNeonColor(): Color {
        return when {
            r && g && b -> Color(0xFFFFFFFF) // Brilliant White
            r && g -> Color(0xFFFFEB3B)      // Neon Yellow
            r && b -> Color(0xFFE040FB)      // Bright Magenta
            g && b -> Color(0xFF00E5FF)      // Electric Cyan
            r -> Color(0xFFFF1744)           // Vibrant Red
            g -> Color(0xFF00E676)           // Radioactive Green
            b -> Color(0xFF2979FF)           // Neon Blue
            else -> Color(0xFF303030)        // Empty Pipe Core
        }
    }

    fun nameInItalian(): String {
        return when {
            r && g && b -> "Bianco"
            r && g -> "Giallo"
            r && b -> "Magenta"
            g && b -> "Ciano"
            r -> "Rosso"
            g -> "Verde"
            b -> "Blu"
            else -> "Vuoto"
        }
    }
}

enum class Direction(val dx: Int, val dy: Int, val angle: Float) {
    UP(0, -1, 0f),
    RIGHT(1, 0, 90f),
    DOWN(0, 1, 180f),
    LEFT(-1, 0, 270f);

    fun rotate(steps: Int): Direction {
        val values = entries
        val newOrdinal = (ordinal + steps) % 4
        return values[newOrdinal]
    }

    val opposite: Direction get() = when(this) {
        UP -> DOWN
        RIGHT -> LEFT
        DOWN -> UP
        LEFT -> RIGHT
    }
}

enum class PipeType {
    STRAIGHT, // I-shape
    ELBOW,    // L-shape
    TEE,      // T-shape
    CROSS,    // X-shape
    VALVE     // One-way check valve
}

data class PipeDef(
    val type: PipeType,
    val rotation: Int = 0,        // 0 to 3 (clockwise)
    val isFixed: Boolean = false,   // Cannot be edited, rotated, or moved
    val isRotatable: Boolean = true, // Can click to rotate 90 degrees
    val isMovable: Boolean = false   // Can drag off/on the grid or place from dock
) {
    // Return directions this pipe can connect to
    fun getPorts(): Set<Direction> {
        val r = rotation % 4
        return when (type) {
            PipeType.STRAIGHT -> setOf(Direction.UP.rotate(r), Direction.DOWN.rotate(r))
            PipeType.ELBOW -> setOf(Direction.UP.rotate(r), Direction.RIGHT.rotate(r))
            PipeType.TEE -> setOf(Direction.LEFT.rotate(r), Direction.UP.rotate(r), Direction.RIGHT.rotate(r))
            PipeType.CROSS -> Direction.entries.toSet()
            PipeType.VALVE -> setOf(Direction.UP.rotate(r), Direction.DOWN.rotate(r)) // Valve has input & output on these sides
        }
    }

    // Checking specifically for input vs output direction for valves
    fun hasInputPort(fromDir: Direction): Boolean {
        // fromDir is the direction we are coming INTO the pipe
        // Ex: if we go DOWN, the direction is DOWN (pointing down)
        // If a valve points UP (rotation 0), its input is from DOWN (Direction.DOWN)
        val r = rotation % 4
        return when (type) {
            PipeType.VALVE -> {
                val inputDir = Direction.DOWN.rotate(r) // Input is DOWN side rotated
                fromDir == inputDir
            }
            else -> getPorts().contains(fromDir)
        }
    }

    fun hasOutputPort(toDir: Direction): Boolean {
        // toDir is direction liquid goes OUT from the pipe
        val r = rotation % 4
        return when (type) {
            PipeType.VALVE -> {
                val outputDir = Direction.UP.rotate(r) // Output is UP side rotated
                toDir == outputDir
            }
            else -> getPorts().contains(toDir)
        }
    }
}

sealed class GridCellType {
    data class Source(val color: FluidColor, val emissionDir: Direction) : GridCellType()
    data class Target(val requiredColor: FluidColor) : GridCellType()
    data object Normal : GridCellType()
}

data class GridCell(
    val x: Int,
    val y: Int,
    val cellType: GridCellType = GridCellType.Normal,
    val pipe: PipeDef? = null,
    val actualColor: FluidColor = FluidColor(), // Computed during dynamic flow propagation
    val outFlows: Map<Direction, FluidColor> = emptyMap()
) {
    val isSource: Boolean get() = cellType is GridCellType.Source
    val isTarget: Boolean get() = cellType is GridCellType.Target

    fun hasInputPort(fromDir: Direction): Boolean {
        return when (val ct = cellType) {
            is GridCellType.Source -> false
            is GridCellType.Target -> true // Target accepts from any side
            GridCellType.Normal -> pipe?.hasInputPort(fromDir) ?: false
        }
    }

    fun hasOutputPort(toDir: Direction): Boolean {
        return when (val ct = cellType) {
            is GridCellType.Source -> toDir == ct.emissionDir
            is GridCellType.Target -> false // Target never outputs
            GridCellType.Normal -> pipe?.hasOutputPort(toDir) ?: false
        }
    }
}

data class Level(
    val id: Int,
    val name: String,
    val description: String,
    val width: Int,
    val height: Int,
    val initialCells: List<GridCell>,
    val dockPipes: List<PipeDef>,
    val isProcedural: Boolean = false,
    val maxMovesForThreeStars: Int = 10,
    val maxMovesForTwoStars: Int = 20
)
