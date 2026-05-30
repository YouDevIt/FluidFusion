package com.example.game

object TutorialLevels {

    val LIST: List<Level> = listOf(
        // Level 1: Basics of Rotation (3x3)
        Level(
            id = 1,
            name = "Le Basi del Flusso",
            description = "Tocca il tubo dritto al centro per ruotarlo e far scorrere il liquido rosso verso il contenitore.",
            width = 3,
            height = 3,
            initialCells = listOf(
                // Row 0
                GridCell(0, 0), GridCell(1, 0), GridCell(2, 0),
                // Row 1
                GridCell(0, 1, GridCellType.Source(FluidColor(r = true), Direction.RIGHT)),
                GridCell(1, 1, GridCellType.Normal, PipeDef(PipeType.STRAIGHT, rotation = 0, isFixed = false, isRotatable = true, isMovable = false)),
                GridCell(2, 1, GridCellType.Target(FluidColor(r = true))),
                // Row 2
                GridCell(0, 2), GridCell(1, 2), GridCell(2, 2)
            ),
            dockPipes = emptyList(),
            maxMovesForThreeStars = 2,
            maxMovesForTwoStars = 5
        ),

        // Level 2: The Elbow Turn (3x3)
        Level(
            id = 2,
            name = "Curva d'Apprendimento",
            description = "Usa raccordi a gomito per aggirare l'ostacolo. Tocca le curve per orientarle correttamente.",
            width = 3,
            height = 3,
            initialCells = listOf(
                // Row 0
                GridCell(0, 0, GridCellType.Source(FluidColor(g = true), Direction.RIGHT)),
                GridCell(1, 0, GridCellType.Normal, PipeDef(PipeType.ELBOW, rotation = 0, isFixed = false, isRotatable = true, isMovable = false)),
                GridCell(2, 0),
                // Row 1
                GridCell(0, 1),
                GridCell(1, 1, GridCellType.Normal, PipeDef(PipeType.STRAIGHT, rotation = 0, isFixed = true, isRotatable = false, isMovable = false)), // Scaffolding block
                GridCell(2, 1),
                // Row 2
                GridCell(0, 2),
                GridCell(1, 2, GridCellType.Normal, PipeDef(PipeType.ELBOW, rotation = 2, isFixed = false, isRotatable = true, isMovable = false)),
                GridCell(2, 2, GridCellType.Target(FluidColor(g = true)))
            ),
            dockPipes = emptyList(),
            maxMovesForThreeStars = 4,
            maxMovesForTwoStars = 8
        ),

        // Level 3: Color Mixing (4x4)
        Level(
            id = 3,
            name = "Sintesi Gialla",
            description = "Il contenitore richiede Giallo, ottenuto mescolando Rosso e Verde. Collega i due flussi nel tubo a T al centro!",
            width = 4,
            height = 4,
            initialCells = listOf(
                // Row 0
                GridCell(0, 0),
                GridCell(1, 0, GridCellType.Source(FluidColor(r = true), Direction.DOWN)),
                GridCell(2, 0), GridCell(3, 0),
                // Row 1
                GridCell(0, 1),
                GridCell(1, 1, GridCellType.Normal, PipeDef(PipeType.TEE, rotation = 0, isFixed = false, isRotatable = true, isMovable = false)),
                GridCell(2, 1, GridCellType.Normal, PipeDef(PipeType.STRAIGHT, rotation = 0, isFixed = false, isRotatable = true, isMovable = false)),
                GridCell(3, 1, GridCellType.Target(FluidColor(r = true, g = true))), // Yellow
                // Row 2
                GridCell(0, 2),
                GridCell(1, 2, GridCellType.Source(FluidColor(g = true), Direction.UP)),
                GridCell(2, 2), GridCell(3, 2),
                // Row 3
                GridCell(0, 3), GridCell(1, 3), GridCell(2, 3), GridCell(3, 3)
            ),
            dockPipes = emptyList(),
            maxMovesForThreeStars = 4,
            maxMovesForTwoStars = 10
        ),

        // Level 4: One-way Check Valves (4x4)
        Level(
            id = 4,
            name = "Il Senso Unico",
            description = "Attenzione alle valvole antiritorno (con la freccia)! Permettono il passaggio in una sola direzione.",
            width = 4,
            height = 4,
            initialCells = listOf(
                // Row 0
                GridCell(0, 0, GridCellType.Source(FluidColor(b = true), Direction.DOWN)),
                GridCell(1, 0), GridCell(2, 0), GridCell(3, 0),
                // Row 1
                GridCell(0, 1, GridCellType.Normal, PipeDef(PipeType.ELBOW, rotation = 2, isFixed = false, isRotatable = true, isMovable = false)),
                GridCell(1, 1, GridCellType.Normal, PipeDef(PipeType.VALVE, rotation = 3, isFixed = false, isRotatable = true, isMovable = false)),
                GridCell(2, 1, GridCellType.Normal, PipeDef(PipeType.STRAIGHT, rotation = 0, isFixed = false, isRotatable = true, isMovable = false)),
                GridCell(3, 1, GridCellType.Target(FluidColor(b = true))),
                // Row 2
                GridCell(0, 2), GridCell(1, 2), GridCell(2, 2), GridCell(3, 2),
                // Row 3
                GridCell(0, 3), GridCell(1, 3), GridCell(2, 3), GridCell(3, 3)
            ),
            dockPipes = emptyList(),
            maxMovesForThreeStars = 5,
            maxMovesForTwoStars = 11
        ),

        // Level 5: The Spare Dock (4x4)
        Level(
            id = 5,
            name = "Officina Idraulica",
            description = "Alcuni tubi si trovano sul banco di lavoro (Dock). Tocca un tubo nel Dock per selezionarlo, poi tocca una casella libera per posizionarlo. Tocca un tubo posizionato per rimuoverlo.",
            width = 4,
            height = 4,
            initialCells = listOf(
                // Row 0
                GridCell(0, 0, GridCellType.Source(FluidColor(r = true), Direction.RIGHT)),
                GridCell(1, 0), // Place Elbow here
                GridCell(2, 0), GridCell(3, 0),
                // Row 1
                GridCell(0, 1),
                GridCell(1, 1, GridCellType.Normal, PipeDef(PipeType.STRAIGHT, rotation = 0, isFixed = false, isRotatable = true, isMovable = false)), // mid point straight vertical
                GridCell(2, 1), GridCell(3, 1),
                // Row 2
                GridCell(0, 2),
                GridCell(1, 2), // Place elbow or bend here
                GridCell(2, 2, GridCellType.Normal, PipeDef(PipeType.STRAIGHT, rotation = 1, isFixed = false, isRotatable = true, isMovable = false)),
                GridCell(3, 2, GridCellType.Target(FluidColor(r = true))),
                // Row 3
                GridCell(0, 3), GridCell(1, 3), GridCell(2, 3), GridCell(3, 3)
            ),
            dockPipes = listOf(
                PipeDef(PipeType.ELBOW, rotation = 1, isFixed = false, isRotatable = true, isMovable = true),
                PipeDef(PipeType.ELBOW, rotation = 3, isFixed = false, isRotatable = true, isMovable = true)
            ),
            maxMovesForThreeStars = 6,
            maxMovesForTwoStars = 14
        )
    )
}
