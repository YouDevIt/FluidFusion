package com.example

import com.example.game.*
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testAllTutorialLevelsAreSolvable() {
        for (level in TutorialLevels.LIST) {
            println("=== Checking Solvability for Level ${level.id}: ${level.name} ===")
            val solved = solveLevel(level)
            assertTrue("Level ${level.id} (${level.name}) has no possible solutions!", solved)
            println("Result: Level ${level.id} (${level.name}) is fully SOLVABLE!")
        }
    }

    private fun solveLevel(level: Level): Boolean {
        return searchPlacements(level.width, level.height, level.initialCells, level.dockPipes)
    }

    // Recursively place all dock pipes
    private fun searchPlacements(width: Int, height: Int, cells: List<GridCell>, dock: List<PipeDef>): Boolean {
        if (dock.isEmpty()) {
            // Once all dock pipes are placed, solve the rotations of all rotatable pipes
            val rotatableIndices = cells.indices.filter { idx ->
                val cell = cells[idx]
                cell.pipe != null && cell.pipe.isRotatable && !cell.pipe.isFixed
            }
            return searchRotations(width, height, cells, rotatableIndices, 0)
        }

        val nextPipe = dock.first()
        val remainingDock = dock.drop(1)

        for (idx in cells.indices) {
            val cell = cells[idx]
            if (cell.cellType is GridCellType.Normal && cell.pipe == null) {
                val nextCells = cells.toMutableList()
                nextCells[idx] = cell.copy(pipe = nextPipe)
                if (searchPlacements(width, height, nextCells, remainingDock)) {
                    return true
                }
            }
        }
        return false
    }

    // Recursively search rotations for each rotatable pipe on the grid
    private fun searchRotations(
        width: Int,
        height: Int,
        cells: List<GridCell>,
        rotatableIndices: List<Int>,
        currentIndex: Int
    ): Boolean {
        if (currentIndex == rotatableIndices.size) {
            val resolved = propagateFlows(width, height, cells)
            return checkCompleted(resolved)
        }

        val cellIndex = rotatableIndices[currentIndex]
        val cell = cells[cellIndex]
        val pipe = cell.pipe ?: return false

        for (rotation in 0..3) {
            val nextCells = cells.toMutableList()
            nextCells[cellIndex] = cell.copy(pipe = pipe.copy(rotation = rotation))
            if (searchRotations(width, height, nextCells, rotatableIndices, currentIndex + 1)) {
                return true
            }
        }
        return false
    }

    private fun checkCompleted(cells: List<GridCell>): Boolean {
        var targetCount = 0
        for (cell in cells) {
            if (cell.cellType is GridCellType.Target) {
                targetCount++
                val reqColor = cell.cellType.requiredColor
                val gotColor = cell.actualColor
                if (reqColor != gotColor) return false
            }
        }
        return targetCount > 0
    }

    private fun propagateFlows(width: Int, height: Int, cells: List<GridCell>): List<GridCell> {
        var current = cells.map { cell ->
            if (cell.cellType is GridCellType.Source) {
                val sc = cell.cellType as GridCellType.Source
                cell.copy(actualColor = sc.color, outFlows = mapOf(sc.emissionDir to sc.color))
            } else {
                cell.copy(actualColor = FluidColor(), outFlows = emptyMap())
            }
        }

        val maxIter = width * height + 2
        var changed = true
        var iter = 0

        while (changed && iter < maxIter) {
            changed = false
            val next = current.map { cell ->
                if (cell.cellType is GridCellType.Source) {
                    cell
                } else {
                    val inFlows = mutableMapOf<Direction, FluidColor>()
                    for (dir in Direction.entries) {
                        val nx = cell.x + dir.dx
                        val ny = cell.y + dir.dy
                        
                        if (nx in 0 until width && ny in 0 until height) {
                            val neighbor = current[ny * width + nx]
                            
                            if (neighbor.hasOutputPort(dir.opposite) && cell.hasInputPort(dir)) {
                                val flowFromNeighbor = neighbor.outFlows[dir.opposite]
                                if (flowFromNeighbor != null && !flowFromNeighbor.isEmpty) {
                                    inFlows[dir] = flowFromNeighbor
                                }
                            }
                        }
                    }

                    val newOutFlows = mutableMapOf<Direction, FluidColor>()
                    var centerColor = FluidColor()

                    if (cell.cellType is GridCellType.Target) {
                        inFlows.values.forEach { centerColor = centerColor.mix(it) }
                    } else if (cell.pipe != null) {
                        if (cell.pipe.type == PipeType.CROSS) {
                            val vFlow = FluidColor().mix(inFlows[Direction.UP] ?: FluidColor()).mix(inFlows[Direction.DOWN] ?: FluidColor())
                            val hFlow = FluidColor().mix(inFlows[Direction.LEFT] ?: FluidColor()).mix(inFlows[Direction.RIGHT] ?: FluidColor())
                            
                            if (cell.pipe.hasOutputPort(Direction.UP)) newOutFlows[Direction.UP] = vFlow
                            if (cell.pipe.hasOutputPort(Direction.DOWN)) newOutFlows[Direction.DOWN] = vFlow
                            if (cell.pipe.hasOutputPort(Direction.LEFT)) newOutFlows[Direction.LEFT] = hFlow
                            if (cell.pipe.hasOutputPort(Direction.RIGHT)) newOutFlows[Direction.RIGHT] = hFlow
                            
                            centerColor = vFlow.mix(hFlow)
                        } else {
                            inFlows.values.forEach { centerColor = centerColor.mix(it) }
                            for (dir in Direction.entries) {
                                if (cell.pipe.hasOutputPort(dir)) {
                                    newOutFlows[dir] = centerColor
                                }
                            }
                        }
                    }

                    if (centerColor != cell.actualColor || newOutFlows != cell.outFlows) {
                        changed = true
                        cell.copy(actualColor = centerColor, outFlows = newOutFlows)
                    } else {
                        cell
                    }
                }
            }
            current = next
            iter++
        }
        return current
    }
}
