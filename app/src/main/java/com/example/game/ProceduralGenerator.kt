package com.example.game

import kotlin.random.Random

object ProceduralGenerator {

    fun generateLevel(levelId: Int, isHard: Boolean = false): Level {
        val random = Random(levelId + 420)
        val type = random.nextInt(0, 4)
        return when(type) {
            0 -> generateCrossLevel(levelId, random)
            1 -> generateThreeCrossLevel(levelId, random)
            2 -> generateTwoMixOnePureLevel(levelId, random)
            else -> generateMixerLevel(levelId, random)
        }
    }

    private fun generateCrossLevel(levelId: Int, random: Random): Level {
        val width = 5
        val height = 5

        // Two distinct colors
        val primaryColors = listOf(
            FluidColor(r = true),
            FluidColor(g = true),
            FluidColor(b = true)
        )
        val colorA = primaryColors[0]
        val colorB = primaryColors[1]

        // Source A on Left, Target A on Right
        val yA_src = random.nextInt(1, height - 1)
        val yA_tgt = random.nextInt(1, height - 1)

        // Source B on Top, Target B on Bottom
        val xB_src = random.nextInt(1, width - 1)
        val xB_tgt = random.nextInt(1, width - 1)

        val srcA = GridCell(0, yA_src, GridCellType.Source(colorA, Direction.RIGHT))
        val tgtA = GridCell(width - 1, yA_tgt, GridCellType.Target(colorA))

        val srcB = GridCell(xB_src, 0, GridCellType.Source(colorB, Direction.DOWN))
        val tgtB = GridCell(xB_tgt, height - 1, GridCellType.Target(colorB))

        val cellMap = mutableMapOf<Pair<Int, Int>, GridCell>()
        for (y in 0 until height) {
            for (x in 0 until width) {
                cellMap[Pair(x, y)] = GridCell(x, y, GridCellType.Normal)
            }
        }

        cellMap[Pair(0, yA_src)] = srcA
        cellMap[Pair(width - 1, yA_tgt)] = tgtA
        cellMap[Pair(xB_src, 0)] = srcB
        cellMap[Pair(xB_tgt, height - 1)] = tgtB

        val solvedPipes = mutableMapOf<Pair<Int, Int>, PipeDef>()

        var pathACells: MutableList<Pair<Int, Int>>
        var pathBCells: MutableList<Pair<Int, Int>>
        var allPathCoords: MutableList<Pair<Int, Int>>
        var isValidCrossing: Boolean

        do {
            // Path A Trace (Left to Right)
            pathACells = mutableListOf<Pair<Int, Int>>()
            for (x in 1..2) pathACells.add(Pair(x, yA_src))
            val yMidA = random.nextInt(1, height - 1)
            if (yA_src < yMidA) {
                for (y in yA_src..yMidA) if (!pathACells.contains(Pair(2, y))) pathACells.add(Pair(2, y))
            } else {
                for (y in yA_src downTo yMidA) if (!pathACells.contains(Pair(2, y))) pathACells.add(Pair(2, y))
            }
            for (x in 2..3) if (!pathACells.contains(Pair(x, yMidA))) pathACells.add(Pair(x, yMidA))
            if (yMidA < yA_tgt) {
                for (y in yMidA..yA_tgt) if (!pathACells.contains(Pair(3, y))) pathACells.add(Pair(3, y))
            } else {
                for (y in yMidA downTo yA_tgt) if (!pathACells.contains(Pair(3, y))) pathACells.add(Pair(3, y))
            }
    
            // Path B Trace (Top to Bottom)
            pathBCells = mutableListOf<Pair<Int, Int>>()
            val midY_B = random.nextInt(1, height - 1)
            for (y in 1..midY_B) pathBCells.add(Pair(xB_src, y))
            if (xB_src < xB_tgt) {
                for (x in xB_src..xB_tgt) if (!pathBCells.contains(Pair(x, midY_B))) pathBCells.add(Pair(x, midY_B))
            } else {
                for (x in xB_src downTo xB_tgt) if (!pathBCells.contains(Pair(x, midY_B))) pathBCells.add(Pair(x, midY_B))
            }
            for (y in midY_B until height-1) if (!pathBCells.contains(Pair(xB_tgt, y))) pathBCells.add(Pair(xB_tgt, y))
            
            val intersection = pathACells.intersect(pathBCells)
            isValidCrossing = true
            
            // For a valid crossing, there must be NO shared segments (lines or corners).
            // A perfect crossing means at the intersection point, Path A is horizontal and Path B is vertical (or vice versa).
            // If they share more than 1 cell, or if one path turns exactly at the intersection, it's invalid.
            for (cross in intersection) {
                var aHoriz = false; var aVert = false
                var bHoriz = false; var bVert = false
                
                val aIdx = pathACells.indexOf(cross)
                if (aIdx > 0) {
                    val prev = pathACells[aIdx - 1]
                    if (prev.first != cross.first) aHoriz = true
                    if (prev.second != cross.second) aVert = true
                }
                if (aIdx < pathACells.size - 1) {
                    val next = pathACells[aIdx + 1]
                    if (next.first != cross.first) aHoriz = true
                    if (next.second != cross.second) aVert = true
                }
                
                val bIdx = pathBCells.indexOf(cross)
                if (bIdx > 0) {
                    val prev = pathBCells[bIdx - 1]
                    if (prev.first != cross.first) bHoriz = true
                    if (prev.second != cross.second) bVert = true
                }
                if (bIdx < pathBCells.size - 1) {
                    val next = pathBCells[bIdx + 1]
                    if (next.first != cross.first) bHoriz = true
                    if (next.second != cross.second) bVert = true
                }
                
                if (aHoriz && aVert) isValidCrossing = false // Path A turns at cross
                if (bHoriz && bVert) isValidCrossing = false // Path B turns at cross
                if (aHoriz && bHoriz) isValidCrossing = false // Both horizontal
                if (aVert && bVert) isValidCrossing = false // Both vertical
            }
        } while (!isValidCrossing)

        allPathCoords = mutableListOf<Pair<Int, Int>>()
        allPathCoords.addAll(pathACells)
        allPathCoords.addAll(pathBCells)

        for (coord in allPathCoords.distinct()) {
            if (cellMap[coord]?.isSource == true || cellMap[coord]?.isTarget == true) continue
            val neighbors = mutableSetOf<Direction>()
            
            // Check connected paths
            for (d in Direction.entries) {
                val nx = coord.first + d.dx
                val ny = coord.second + d.dy
                val nCoord = Pair(nx, ny)
                
                // If it's on Path A, check Path A connectivity
                if (pathACells.contains(coord) && (pathACells.contains(nCoord) || nCoord == Pair(0, yA_src) || nCoord == Pair(width - 1, yA_tgt))) {
                    neighbors.add(d)
                }
                // If it's on Path B, check Path B connectivity
                if (pathBCells.contains(coord) && (pathBCells.contains(nCoord) || nCoord == Pair(xB_src, 0) || nCoord == Pair(xB_tgt, height - 1))) {
                    neighbors.add(d)
                }
            }

            val pipeDef = determineOptimalPipe(neighbors)
            solvedPipes[coord] = pipeDef
        }

        val scrambledGrid = mutableMapOf<Pair<Int, Int>, GridCell>()
        cellMap.forEach { (coord, cell) -> scrambledGrid[coord] = cell }

        val dockPipesList = mutableListOf<PipeDef>()
        solvedPipes.forEach { (coord, solvedPipe) ->
            val toDock = !solvedPipe.isFixed && random.nextFloat() < 0.60f
            val scrambleRotation = random.nextInt(0, 4)
            val scrambledPipe = solvedPipe.copy(rotation = (solvedPipe.rotation + scrambleRotation) % 4)

            if (toDock) {
                scrambledGrid[coord] = scrambledGrid[coord]!!.copy(pipe = null)
                dockPipesList.add(scrambledPipe.copy(isMovable = true))
            } else {
                scrambledGrid[coord] = scrambledGrid[coord]!!.copy(pipe = scrambledPipe)
            }
        }

        val finalCells = mutableListOf<GridCell>()
        for (y in 0 until height) {
            for (x in 0 until width) {
                finalCells.add(scrambledGrid[Pair(x, y)]!!)
            }
        }

        return Level(
            id = -levelId,
            name = "Intreccio #${100 + levelId}",
            description = "Fai incrociare i flussi di colori diversi senza mescolarli.",
            width = width,
            height = height,
            initialCells = finalCells,
            dockPipes = dockPipesList.shuffled(random),
            isProcedural = true,
            maxMovesForThreeStars = 6,
            maxMovesForTwoStars = 12
        )
    }

    private fun generateMixerLevel(levelId: Int, random: Random): Level {
        val width = 5
        val height = 5

        // 1. Determine colors and positions
        val yA = random.nextInt(0, height)
        var yB = random.nextInt(0, height)
        while (yB == yA) {
            yB = random.nextInt(0, height)
        }

        val primaryColors = listOf(
            FluidColor(r = true),
            FluidColor(g = true),
            FluidColor(b = true)
        )
        val idxA = random.nextInt(0, 3)
        var idxB = (idxA + random.nextInt(1, 3)) % 3
        
        val colorA = primaryColors[idxA]
        val colorB = primaryColors[idxB]
        val targetColor = colorA.mix(colorB) // Yellow, Magenta, or Cyan!

        val yTarget = random.nextInt(0, height) // Target on col 4 (right side)
        val yJunction = random.nextInt(0, height) // Random junction row

        val srcA = GridCell(0, yA, GridCellType.Source(colorA, Direction.RIGHT))
        val srcB = GridCell(0, yB, GridCellType.Source(colorB, Direction.RIGHT))
        val target = GridCell(4, yTarget, GridCellType.Target(targetColor))

        // Create initial grid empty
        val cellMap = mutableMapOf<Pair<Int, Int>, GridCell>()
        for (y in 0 until height) {
            for (x in 0 until width) {
                cellMap[Pair(x, y)] = GridCell(x, y, GridCellType.Normal)
            }
        }

        // Overwrite indices for sources and target
        cellMap[Pair(0, yA)] = srcA
        cellMap[Pair(0, yB)] = srcB
        cellMap[Pair(4, yTarget)] = target

        // Track the pipes we are adding for the solved state
        // Key: coordinates, Value: PipeDef
        val solvedPipes = mutableMapOf<Pair<Int, Int>, PipeDef>()

        // Path A: from (1, yA) to (2, yJunction)
        // Travels horizontally to x=2, then vertically to yJunction
        // Path B: from (1, yB) to (2, yJunction)
        // Travels horizontally to x=2, then vertically to yJunction

        // Path A Trace:
        val pathACells = mutableListOf<Pair<Int, Int>>()
        for (x in 1..2) {
            pathACells.add(Pair(x, yA))
        }
        if (yA < yJunction) {
            for (y in yA..yJunction) {
                val p = Pair(2, y)
                if (!pathACells.contains(p)) pathACells.add(p)
            }
        } else if (yA > yJunction) {
            for (y in yA downTo yJunction) {
                val p = Pair(2, y)
                if (!pathACells.contains(p)) pathACells.add(p)
            }
        }

        // Path B Trace:
        val pathBCells = mutableListOf<Pair<Int, Int>>()
        for (x in 1..2) {
            pathBCells.add(Pair(x, yB))
        }
        if (yB < yJunction) {
            for (y in yB..yJunction) {
                val p = Pair(2, y)
                if (!pathBCells.contains(p)) pathBCells.add(p)
            }
        } else if (yB > yJunction) {
            for (y in yB downTo yJunction) {
                val p = Pair(2, y)
                if (!pathBCells.contains(p)) pathBCells.add(p)
            }
        }

        // Shared Path (Merge Output):
        // From (2, yJunction) to (3, yJunction), then to (3, yTarget) to (4, yTarget)
        val pathCCells = mutableListOf<Pair<Int, Int>>()
        pathCCells.add(Pair(2, yJunction))
        pathCCells.add(Pair(3, yJunction))
        if (yJunction < yTarget) {
            for (y in yJunction..yTarget) {
                val p = Pair(3, y)
                if (!pathCCells.contains(p)) pathCCells.add(p)
            }
        } else if (yJunction > yTarget) {
            for (y in yJunction downTo yTarget) {
                val p = Pair(3, y)
                if (!pathCCells.contains(p)) pathCCells.add(p)
            }
        }
        pathCCells.add(Pair(3, yTarget))

        // Assign pipe types based on connections
        val allPathCoords = (pathACells + pathBCells + pathCCells).distinct()
        
        for (coord in allPathCoords) {
            val (cx, cy) = coord
            if (cellMap[coord]?.isSource == true || cellMap[coord]?.isTarget == true) continue

            // Determine what neighbors are part of our paths for this cell
            val neighbors = mutableSetOf<Direction>()
            for (d in Direction.entries) {
                val nx = cx + d.dx
                val ny = cy + d.dy
                val nCoord = Pair(nx, ny)
                if (allPathCoords.contains(nCoord) || cellMap[nCoord]?.isSource == true || cellMap[nCoord]?.isTarget == true) {
                    // Check if there is an actual flow link
                    // Direct linkage verification
                    if (cellMap[nCoord]?.isSource == true) {
                        if (cellMap[nCoord]?.hasOutputPort(d.opposite) == true) {
                            neighbors.add(d)
                        }
                    } else if (cellMap[nCoord]?.isTarget == true) {
                        neighbors.add(d)
                    } else {
                        neighbors.add(d)
                    }
                }
            }

            // Based on connected neighbor directions, assign optimal pipe type & rotation
            val pipeDef = determineOptimalPipe(neighbors)
            solvedPipes[coord] = pipeDef
        }

        // If possible, replace one straight pipe on the shared Path C with a One-Way Check VALVE!
        for (coord in pathCCells) {
            if (cellMap[coord]?.isSource == true || cellMap[coord]?.isTarget == true) continue
            val existing = solvedPipes[coord]
            if (existing != null && existing.type == PipeType.STRAIGHT) {
                // If straight is horizontal (rotation 1 or 3): pointing Left or Right
                // Flow on Path C is primarily left to right: inputs LEFT, outputs RIGHT
                // Let's place a valve flowing to the right (rotation = 1)
                val valveRot = if (existing.rotation % 2 == 1) 1 else 0
                solvedPipes[coord] = PipeDef(
                    type = PipeType.VALVE,
                    rotation = valveRot,
                    isFixed = false,
                    isRotatable = true,
                    isMovable = false
                )
                break // Only place one valve
            }
        }

        // 4. Scramble the generated solution
        // Separate grid cells and dock spares.
        // Some cells will be empty on grid, and their correct pipe placed in dock
        val scrambledGrid = mutableMapOf<Pair<Int, Int>, GridCell>()
        cellMap.forEach { (coord, cell) ->
            scrambledGrid[coord] = cell
        }

        val dockPipesList = mutableListOf<PipeDef>()

        solvedPipes.forEach { (coord, solvedPipe) ->
            // 50% chance to put the pipe into the spare dock (if not fixed)
            val toDock = !solvedPipe.isFixed && random.nextFloat() < 0.50f
            val scrambleRotation = random.nextInt(0, 4)
            val scrambledPipe = solvedPipe.copy(
                rotation = (solvedPipe.rotation + scrambleRotation) % 4
            )

            if (toDock) {
                // Remove pipe from grid cell, place in dock
                scrambledGrid[coord] = scrambledGrid[coord]!!.copy(pipe = null)
                dockPipesList.add(scrambledPipe.copy(isMovable = true))
            } else {
                // Keep on grid, but apply scrambled rotation
                scrambledGrid[coord] = scrambledGrid[coord]!!.copy(pipe = scrambledPipe)
            }
        }

        val name = "Sintesi #${100 + levelId}"
        val desc = "Miscela colorata procedurale. Convoglia i flussi a destra, superando le valvole."

        // Order the final grid cell list col-by-row
        val finalCells = mutableListOf<GridCell>()
        for (y in 0 until height) {
            for (x in 0 until width) {
                finalCells.add(scrambledGrid[Pair(x, y)]!!)
            }
        }

        return Level(
            id = -levelId, // procedural levels have negative IDs to distinguish from tutorial levels
            name = name,
            description = desc,
            width = width,
            height = height,
            initialCells = finalCells,
            dockPipes = dockPipesList.shuffled(random),
            isProcedural = true,
            maxMovesForThreeStars = 8,
            maxMovesForTwoStars = 15
        )
    }

    private fun traceHorizontalPath(
        startX: Int, startY: Int,
        endX: Int, endY: Int,
        midX1: Int, midX2: Int,
        midY: Int
    ): MutableList<Pair<Int, Int>> {
        val path = mutableListOf<Pair<Int, Int>>()
        for (x in startX..midX1) path.add(Pair(x, startY))
        
        if (startY < midY) {
            for (y in startY..midY) if (!path.contains(Pair(midX1, y))) path.add(Pair(midX1, y))
        } else {
            for (y in startY downTo midY) if (!path.contains(Pair(midX1, y))) path.add(Pair(midX1, y))
        }
        
        for (x in midX1..midX2) if (!path.contains(Pair(x, midY))) path.add(Pair(x, midY))
        
        if (midY < endY) {
            for (y in midY..endY) if (!path.contains(Pair(midX2, y))) path.add(Pair(midX2, y))
        } else {
            for (y in midY downTo endY) if (!path.contains(Pair(midX2, y))) path.add(Pair(midX2, y))
        }
        
        for (x in midX2..endX) if (!path.contains(Pair(x, endY))) path.add(Pair(x, endY))
        
        return path
    }

    private fun traceVerticalPath(
        startX: Int, startY: Int,
        endX: Int, endY: Int,
        midY1: Int, midY2: Int,
        midX: Int
    ): MutableList<Pair<Int, Int>> {
        val path = mutableListOf<Pair<Int, Int>>()
        for (y in startY..midY1) path.add(Pair(startX, y))
        
        if (startX < midX) {
            for (x in startX..midX) if (!path.contains(Pair(x, midY1))) path.add(Pair(x, midY1))
        } else {
            for (x in startX downTo midX) if (!path.contains(Pair(x, midY1))) path.add(Pair(x, midY1))
        }
        
        for (y in midY1..midY2) if (!path.contains(Pair(midX, y))) path.add(Pair(midX, y))
        
        if (midX < endX) {
            for (x in midX..endX) if (!path.contains(Pair(x, midY2))) path.add(Pair(x, midY2))
        } else {
            for (x in midX downTo endX) if (!path.contains(Pair(x, midY2))) path.add(Pair(x, midY2))
        }
        
        for (y in midY2..endY) if (!path.contains(Pair(endX, y))) path.add(Pair(endX, y))
        
        return path
    }

    private fun isValidCrossing(pathA: List<Pair<Int, Int>>, pathB: List<Pair<Int, Int>>): Boolean {
        val intersection = pathA.intersect(pathB.toSet())
        for (cross in intersection) {
            var aHoriz = false; var aVert = false
            var bHoriz = false; var bVert = false
            
            val aIdx = pathA.indexOf(cross)
            if (aIdx > 0) {
                val prev = pathA[aIdx - 1]
                if (prev.first != cross.first) aHoriz = true
                if (prev.second != cross.second) aVert = true
            }
            if (aIdx < pathA.size - 1) {
                val next = pathA[aIdx + 1]
                if (next.first != cross.first) aHoriz = true
                if (next.second != cross.second) aVert = true
            }
            
            val bIdx = pathB.indexOf(cross)
            if (bIdx > 0) {
                val prev = pathB[bIdx - 1]
                if (prev.first != cross.first) bHoriz = true
                if (prev.second != cross.second) bVert = true
            }
            if (bIdx < pathB.size - 1) {
                val next = pathB[bIdx + 1]
                if (next.first != cross.first) bHoriz = true
                if (next.second != cross.second) bVert = true
            }
            
            if (aHoriz && aVert) return false // Path A turns at cross
            if (bHoriz && bVert) return false // Path B turns at cross
            if (aHoriz && bHoriz) return false // Both horizontal
            if (aVert && bVert) return false // Both vertical
        }
        return true
    }

    private fun buildLevelFromSolved(
        levelId: Int,
        title: String,
        description: String,
        width: Int,
        height: Int,
        cellMap: Map<Pair<Int, Int>, GridCell>,
        solvedPipes: Map<Pair<Int, Int>, PipeDef>,
        random: Random,
        dockChance: Float = 0.5f
    ): Level {
        val scrambledGrid = mutableMapOf<Pair<Int, Int>, GridCell>()
        cellMap.forEach { (coord, cell) -> scrambledGrid[coord] = cell }

        val dockPipesList = mutableListOf<PipeDef>()
        solvedPipes.forEach { (coord, solvedPipe) ->
            val toDock = !solvedPipe.isFixed && random.nextFloat() < dockChance
            val scrambleRotation = random.nextInt(0, 4)
            val scrambledPipe = solvedPipe.copy(rotation = (solvedPipe.rotation + scrambleRotation) % 4)

            if (toDock) {
                scrambledGrid[coord] = scrambledGrid[coord]!!.copy(pipe = null)
                dockPipesList.add(scrambledPipe.copy(isMovable = true))
            } else {
                scrambledGrid[coord] = scrambledGrid[coord]!!.copy(pipe = scrambledPipe)
            }
        }

        val finalCells = mutableListOf<GridCell>()
        for (y in 0 until height) {
            for (x in 0 until width) {
                finalCells.add(scrambledGrid[Pair(x, y)]!!)
            }
        }

        return Level(
            id = -levelId,
            name = "$title #${100 + levelId}",
            description = description,
            width = width,
            height = height,
            initialCells = finalCells,
            dockPipes = dockPipesList.shuffled(random),
            isProcedural = true,
            maxMovesForThreeStars = (dockPipesList.size + (width * height) * 0.3).toInt(),
            maxMovesForTwoStars = (dockPipesList.size + (width * height) * 0.6).toInt()
        )
    }

    private fun generateThreeCrossLevel(levelId: Int, random: Random): Level {
        val width = 6
        val height = 6

        val primaryColors = listOf(
            FluidColor(r = true),
            FluidColor(g = true),
            FluidColor(b = true)
        )
        val colorA = primaryColors[0]
        val colorB = primaryColors[1]
        val colorC = primaryColors[2]

        val yA_src = random.nextInt(1, 3)
        val yA_tgt = random.nextInt(1, 3)

        val xB_src = random.nextInt(1, 5)
        val xB_tgt = random.nextInt(1, 5)

        val yC_src = random.nextInt(3, 5)
        val yC_tgt = random.nextInt(3, 5)

        val srcA = GridCell(0, yA_src, GridCellType.Source(colorA, Direction.RIGHT))
        val tgtA = GridCell(width - 1, yA_tgt, GridCellType.Target(colorA))

        val srcB = GridCell(xB_src, 0, GridCellType.Source(colorB, Direction.DOWN))
        val tgtB = GridCell(xB_tgt, height - 1, GridCellType.Target(colorB))
        
        val srcC = GridCell(0, yC_src, GridCellType.Source(colorC, Direction.RIGHT))
        val tgtC = GridCell(width - 1, yC_tgt, GridCellType.Target(colorC))

        val cellMap = mutableMapOf<Pair<Int, Int>, GridCell>()
        for (y in 0 until height) {
            for (x in 0 until width) {
                cellMap[Pair(x, y)] = GridCell(x, y, GridCellType.Normal)
            }
        }
        cellMap[Pair(0, yA_src)] = srcA
        cellMap[Pair(width - 1, yA_tgt)] = tgtA
        cellMap[Pair(xB_src, 0)] = srcB
        cellMap[Pair(xB_tgt, height - 1)] = tgtB
        cellMap[Pair(0, yC_src)] = srcC
        cellMap[Pair(width - 1, yC_tgt)] = tgtC

        var pathA: MutableList<Pair<Int, Int>>
        var pathB: MutableList<Pair<Int, Int>>
        var pathC: MutableList<Pair<Int, Int>>
        var isValid: Boolean

        do {
            val midXA = random.nextInt(1, 3)
            val midXB = random.nextInt(3, 5)
            val midYA = random.nextInt(1, 3)
            pathA = traceHorizontalPath(0, yA_src, width - 1, yA_tgt, midXA, midXB, midYA)

            val midXC = random.nextInt(1, 3)
            val midXD = random.nextInt(3, 5)
            val midYC = random.nextInt(3, 5)
            pathC = traceHorizontalPath(0, yC_src, width - 1, yC_tgt, midXC, midXD, midYC)

            val midYB1 = random.nextInt(1, 3)
            val midYB2 = random.nextInt(3, 5)
            val midXB_mid = random.nextInt(1, 5)
            pathB = traceVerticalPath(xB_src, 0, xB_tgt, height - 1, midYB1, midYB2, midXB_mid)

            isValid = isValidCrossing(pathA, pathB) && isValidCrossing(pathC, pathB) && isValidCrossing(pathA, pathC)
        } while (!isValid)

        val allPathCoords = (pathA + pathB + pathC).distinct()
        val solvedPipes = mutableMapOf<Pair<Int, Int>, PipeDef>()

        for (coord in allPathCoords) {
            if (cellMap[coord]?.isSource == true || cellMap[coord]?.isTarget == true) continue
            val neighbors = mutableSetOf<Direction>()
            for (d in Direction.entries) {
                val nx = coord.first + d.dx
                val ny = coord.second + d.dy
                val nCoord = Pair(nx, ny)
                
                if (pathA.contains(coord) && (pathA.contains(nCoord) || nCoord == Pair(0, yA_src) || nCoord == Pair(width-1, yA_tgt))) neighbors.add(d)
                if (pathB.contains(coord) && (pathB.contains(nCoord) || nCoord == Pair(xB_src, 0) || nCoord == Pair(xB_tgt, height-1))) neighbors.add(d)
                if (pathC.contains(coord) && (pathC.contains(nCoord) || nCoord == Pair(0, yC_src) || nCoord == Pair(width-1, yC_tgt))) neighbors.add(d)
            }
            solvedPipes[coord] = determineOptimalPipe(neighbors)
        }

        return buildLevelFromSolved(levelId, "Triplo Incrocio", "Fai incrociare tre flussi separati senza mescolare.", width, height, cellMap, solvedPipes, random, 0.6f)
    }

    private fun generateTwoMixOnePureLevel(levelId: Int, random: Random): Level {
        val width = 6
        val height = 6

        val primaryColors = listOf(
            FluidColor(r = true),
            FluidColor(g = true),
            FluidColor(b = true)
        )
        // Two mix colors and one pure color
        val colorA = primaryColors[0]
        val colorB = primaryColors[1]
        val colorC = primaryColors[2]
        
        val targetColorAB = colorA.mix(colorB)

        // Path A & B will mix. Let's do them horizontal in the top part.
        // Path C will be vertical crossing them.
        
        val yA_src = random.nextInt(1, 3)
        val yB_src = random.nextInt(3, 5)
        val targetAB_y = random.nextInt(1, 5)
        
        val xC_src = random.nextInt(1, 5)
        val xC_tgt = random.nextInt(1, 5)

        val srcA = GridCell(0, yA_src, GridCellType.Source(colorA, Direction.RIGHT))
        val srcB = GridCell(0, yB_src, GridCellType.Source(colorB, Direction.RIGHT))
        val tgtAB = GridCell(width - 1, targetAB_y, GridCellType.Target(targetColorAB))

        val srcC = GridCell(xC_src, 0, GridCellType.Source(colorC, Direction.DOWN))
        val tgtC = GridCell(xC_tgt, height - 1, GridCellType.Target(colorC))
        
        val cellMap = mutableMapOf<Pair<Int, Int>, GridCell>()
        for (y in 0 until height) {
            for (x in 0 until width) {
                cellMap[Pair(x, y)] = GridCell(x, y, GridCellType.Normal)
            }
        }
        cellMap[Pair(0, yA_src)] = srcA
        cellMap[Pair(0, yB_src)] = srcB
        cellMap[Pair(width - 1, targetAB_y)] = tgtAB
        cellMap[Pair(xC_src, 0)] = srcC
        cellMap[Pair(xC_tgt, height - 1)] = tgtC

        var pathA: MutableList<Pair<Int, Int>>
        var pathB: MutableList<Pair<Int, Int>>
        var pathAB: MutableList<Pair<Int, Int>>
        var pathC: MutableList<Pair<Int, Int>>
        var isValid: Boolean

        do {
            val junctionX = random.nextInt(2, 4)
            val junctionY = random.nextInt(1, 5)
            
            // Path A goes from (0, yA_src) to (junctionX, junctionY)
            pathA = traceHorizontalPath(0, yA_src, junctionX, junctionY, junctionX / 2 + 1, junctionX / 2 + 1, yA_src)
            // Path B goes from (0, yB_src) to (junctionX, junctionY)
            pathB = traceHorizontalPath(0, yB_src, junctionX, junctionY, junctionX / 2, junctionX / 2, yB_src)
            // Path AB goes from (junctionX, junctionY) to (width - 1, targetAB_y)
            pathAB = traceHorizontalPath(junctionX, junctionY, width - 1, targetAB_y, junctionX + 1, junctionX + 1, junctionY)
            
            // Path C is vertical, crosses the horizontal paths
            val midYB1 = random.nextInt(1, 3)
            val midYB2 = random.nextInt(3, 5)
            val midXB_mid = random.nextInt(1, 5)
            pathC = traceVerticalPath(xC_src, 0, xC_tgt, height - 1, midYB1, midYB2, midXB_mid)

            // Make sure the junction is not on pathC
            if (pathC.contains(Pair(junctionX, junctionY))) {
                isValid = false
                continue
            }
            
            // Ensure pathA and pathB only intersect at the junction
            val intersectAB = pathA.intersect(pathB.toSet())
            if (intersectAB.size != 1 || !intersectAB.contains(Pair(junctionX, junctionY))) {
                isValid = false
                continue
            }
            
            val unionMixPaths = (pathA + pathB + pathAB).distinct()
            // Check that unionMixPaths doesn't contain source/target of C (impossible due to boundary, but safe)
            // Check crossing validity
            isValid = isValidCrossing(unionMixPaths, pathC)
        } while (!isValid)

        val allPathCoords = (pathA + pathB + pathAB + pathC).distinct()
        val solvedPipes = mutableMapOf<Pair<Int, Int>, PipeDef>()

        for (coord in allPathCoords) {
            if (cellMap[coord]?.isSource == true || cellMap[coord]?.isTarget == true) continue
            val neighbors = mutableSetOf<Direction>()
            
            for (d in Direction.entries) {
                val nx = coord.first + d.dx
                val ny = coord.second + d.dy
                val nCoord = Pair(nx, ny)
                
                // Track Path C
                if (pathC.contains(coord) && (pathC.contains(nCoord) || nCoord == Pair(xC_src, 0) || nCoord == Pair(xC_tgt, height-1))) {
                    neighbors.add(d)
                }
                
                // Track Path Mix 
                val isMixPart = (pathA + pathB + pathAB).contains(coord)
                if (isMixPart) {
                    val inA = pathA.contains(coord) && (pathA.contains(nCoord) || nCoord == Pair(0, yA_src))
                    val inB = pathB.contains(coord) && (pathB.contains(nCoord) || nCoord == Pair(0, yB_src))
                    val inAB = pathAB.contains(coord) && (pathAB.contains(nCoord) || nCoord == Pair(width-1, targetAB_y))
                    
                    if (inA || inB || inAB) {
                        neighbors.add(d)
                    }
                }
            }
            solvedPipes[coord] = determineOptimalPipe(neighbors)
        }

        return buildLevelFromSolved(levelId, "Doppio e Singolo", "Miscela due colori e fai passare l'altro in purezza.", width, height, cellMap, solvedPipes, random, 0.6f)
    }

    private fun determineOptimalPipe(neighbors: Set<Direction>): PipeDef {
        return when {
            neighbors.size >= 4 -> PipeDef(PipeType.CROSS, 0)
            neighbors.size == 3 -> {
                // T-junction
                // Standard unrotated TEE is LEFT, UP, RIGHT (excludes DOWN)
                // Determine missing direction
                val missing = Direction.entries.firstOrNull { !neighbors.contains(it) } ?: Direction.DOWN
                val rotation = when (missing) {
                    Direction.DOWN -> 0
                    Direction.LEFT -> 1
                    Direction.UP -> 2
                    Direction.RIGHT -> 3
                }
                PipeDef(PipeType.TEE, rotation)
            }
            neighbors.size == 2 -> {
                val list = neighbors.toList()
                val d1 = list[0]
                val d2 = list[1]
                if (d1.opposite == d2) {
                    // Straight line
                    // Standard straight is UP, DOWN (rotation 0)
                    // Horizontal is rotation 1 (RIGHT, LEFT)
                    val rot = if (d1 == Direction.LEFT || d1 == Direction.RIGHT) 1 else 0
                    PipeDef(PipeType.STRAIGHT, rot)
                } else {
                    // Elbow Bend
                    // Standard Elbow is UP, RIGHT (rotation 0)
                    val rotation = when {
                        neighbors.contains(Direction.UP) && neighbors.contains(Direction.RIGHT) -> 0
                        neighbors.contains(Direction.RIGHT) && neighbors.contains(Direction.DOWN) -> 1
                        neighbors.contains(Direction.DOWN) && neighbors.contains(Direction.LEFT) -> 2
                        neighbors.contains(Direction.LEFT) && neighbors.contains(Direction.UP) -> 3
                        else -> 0
                    }
                    PipeDef(PipeType.ELBOW, rotation)
                }
            }
            else -> {
                // Just put an arbitrary straight pipe
                PipeDef(PipeType.STRAIGHT, 0)
            }
        }
    }
}
