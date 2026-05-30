package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.*
import com.example.game.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScreenStatus {
    MAIN_MENU,
    LEVEL_SELECT,
    GAMEPLAY,
    LEADERBOARD,
    HELP
}

class PipeGameViewModel(application: Application) : AndroidViewModel(application) {
    private val db = PipeFlowDatabase.getDatabase(application)
    private val repository = GameRepository(db.gameDao())

    // Exposed Flows to UI
    val allScores: StateFlow<List<LevelScoreEntity>> = repository.allScores.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val leaderboard: StateFlow<List<LeaderboardEntryEntity>> = repository.leaderboard.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Screen State
    var currentScreen by mutableStateOf(ScreenStatus.MAIN_MENU)
        private set

    // Active Level State
    var activeLevel by mutableStateOf<Level?>(null)
        private set

    var gridCells by mutableStateOf<List<GridCell>>(emptyList())
        private set

    var dockPipes by mutableStateOf<List<PipeDef>>(emptyList())
        private set

    var selectedDockPipeIndex by mutableStateOf<Int?>(null)

    // Stats
    var movesCount by mutableStateOf(0)
        private set

    var elapsedTimeSec by mutableStateOf(0)
        private set

    var isSolved by mutableStateOf(false)
        private set

    var earnedStars by mutableStateOf(0)
        private set

    var activeScore by mutableStateOf(0)
        private set

    // Time Challenge Mode properties
    var isTimeChallengeActive by mutableStateOf(false)
        private set

    var challengeRemainingTimeSec by mutableStateOf(90)
        private set

    var challengeScoreTotal by mutableStateOf(0)
        private set

    var challengeSolvedPuzzlesCount by mutableStateOf(0)
        private set

    var hasSessionToResume by mutableStateOf(false)
        private set

    // Timer Job
    private var timerJob: Job? = null

    init {
        // Core initialization: pre-seed leaderboard list if empty
        viewModelScope.launch {
            val count = repository.getLeaderboardCount()
            if (count == 0) {
                seedLeaderboard()
            }
            // Check if there is an autosaved session waiting to resume
            val saved = repository.getSavedSession()
            if (saved != null && !saved.isProcedural) {
                val tutorialLevel = TutorialLevels.LIST.firstOrNull { it.id == saved.levelId }
                if (tutorialLevel == null) {
                    repository.clearSession()
                    hasSessionToResume = false
                } else {
                    val savedCells = Converters().jsonToGridCellList(saved.serializedGrid)
                    val realSources = tutorialLevel.initialCells.filter { it.cellType is GridCellType.Source }
                    val savedSources = savedCells.filter { it.cellType is GridCellType.Source }
                    val sourceDiscrepancy = realSources.size != savedSources.size || realSources.any { real ->
                        savedSources.none { it.x == real.x && it.y == real.y }
                    }
                    if (sourceDiscrepancy) {
                        repository.clearSession()
                        hasSessionToResume = false
                    } else {
                        hasSessionToResume = true
                    }
                }
            } else {
                hasSessionToResume = saved != null
            }
        }
    }

    private suspend fun seedLeaderboard() {
        val seed = listOf(
            LeaderboardEntryEntity(playerName = "Matteo_Flow", score = 3850),
            LeaderboardEntryEntity(playerName = "GiuliaHydra", score = 3100),
            LeaderboardEntryEntity(playerName = "WaterLord99", score = 2750),
            LeaderboardEntryEntity(playerName = "AlphaConduit", score = 2500),
            LeaderboardEntryEntity(playerName = "IdroSapiens", score = 2100),
            LeaderboardEntryEntity(playerName = "PipeWiz_IT", score = 1850),
            LeaderboardEntryEntity(playerName = "NeonSprinkler", score = 1400),
            LeaderboardEntryEntity(playerName = "NablaPlumber", score = 1250),
            LeaderboardEntryEntity(playerName = "Mario_U", score = 950),
            LeaderboardEntryEntity(playerName = "Luigi_L", score = 700)
        )
        seed.forEach { repository.insertLeaderboardEntry(it) }
    }

    fun navigateTo(screen: ScreenStatus) {
        currentScreen = screen
        if (screen != ScreenStatus.GAMEPLAY) {
            stopTimer()
        }
    }

    fun startTutorialLevel(levelId: Int) {
        val level = TutorialLevels.LIST.firstOrNull { it.id == levelId } ?: return
        loadLevel(level)
    }

    fun startProceduralLevel(levelId: Int) {
        val level = ProceduralGenerator.generateLevel(levelId)
        loadLevel(level)
    }

    // High performance session RESTORATION
    fun resumeAutosavedSession() {
        viewModelScope.launch {
            val saved = repository.getSavedSession() ?: return@launch
            val cellList = Converters().jsonToGridCellList(saved.serializedGrid)
            val dockList = Converters().jsonToPipeList(saved.serializedDock)

            // Reconstruct Level metadata
            val levelName = if (saved.isProcedural) "Sintesi Procedurale" else "Livello Tutorial #${saved.levelId}"
            val levelDesc = "Riapertura sessione salvata in automatico."

            val dummyLevel = Level(
                id = saved.levelId,
                name = levelName,
                description = levelDesc,
                width = saved.gridWidth,
                height = saved.gridHeight,
                initialCells = cellList,
                dockPipes = dockList,
                isProcedural = saved.isProcedural,
                maxMovesForThreeStars = 8,
                maxMovesForTwoStars = 15
            )

            activeLevel = dummyLevel
            gridCells = cellList
            dockPipes = dockList
            movesCount = saved.movesCount
            elapsedTimeSec = saved.elapsedTimeSec
            isSolved = false
            earnedStars = 0
            isTimeChallengeActive = saved.isChallenge
            challengeScoreTotal = saved.challengeScore
            if (isTimeChallengeActive) {
                challengeRemainingTimeSec = saved.targetTimeLimit
            }

            // Run flows
            recalculateFlowsAndCheckCompletion()
            currentScreen = ScreenStatus.GAMEPLAY

            // Restart timer
            startTimer()
        }
    }

    private fun loadLevel(level: Level) {
        activeLevel = level
        gridCells = level.initialCells.map { it.copy() }
        dockPipes = level.dockPipes.map { it.copy() }
        selectedDockPipeIndex = null
        movesCount = 0
        elapsedTimeSec = 0
        isSolved = false
        earnedStars = 0
        activeScore = 3000
        isTimeChallengeActive = false

        recalculateFlowsAndCheckCompletion()
        currentScreen = ScreenStatus.GAMEPLAY
        startTimer()

        // Trigger safe autosave state
        triggerSessionAutosave()
    }

    fun startTimeChallengeMode() {
        activeLevel = null
        isTimeChallengeActive = true
        challengeRemainingTimeSec = 90
        challengeScoreTotal = 0
        challengeSolvedPuzzlesCount = 0
        selectedDockPipeIndex = null
        movesCount = 0
        elapsedTimeSec = 0
        isSolved = false
        currentScreen = ScreenStatus.GAMEPLAY

        generateNextChallengeLevel()
        startTimer()
    }

    private fun generateNextChallengeLevel() {
        val randomLevelId = (1000..9999).random()
        val level = ProceduralGenerator.generateLevel(randomLevelId)
        activeLevel = level
        gridCells = level.initialCells.map { it.copy() }
        dockPipes = level.dockPipes.map { it.copy() }
        selectedDockPipeIndex = null
        isSolved = false

        recalculateFlowsAndCheckCompletion()
        triggerSessionAutosave()
    }

    // Tap to rotate pipe
    fun rotatePipeAt(x: Int, y: Int) {
        if (isSolved && !isTimeChallengeActive) return

        val width = activeLevel?.width ?: return
        val height = activeLevel?.height ?: return
        val idx = y * width + x
        if (idx !in gridCells.indices) return

        val cell = gridCells[idx]
        val pipe = cell.pipe ?: return
        if (pipe.isFixed || !pipe.isRotatable) return

        // Update rotation
        val newRotation = (pipe.rotation + 1) % 4
        val updatedPipe = pipe.copy(rotation = newRotation)
        
        val updatedCells = gridCells.toMutableList()
        updatedCells[idx] = cell.copy(pipe = updatedPipe)
        gridCells = updatedCells

        movesCount++
        recalculateFlowsAndCheckCompletion()
        triggerSessionAutosave()
    }

    // Select a pipe piece from dock
    fun selectDockPipe(index: Int) {
        if (index in dockPipes.indices) {
            selectedDockPipeIndex = if (selectedDockPipeIndex == index) null else index
        }
    }

    fun removePipeAt(x: Int, y: Int) {
        if (isSolved && !isTimeChallengeActive) return
        val level = activeLevel ?: return
        val idx = y * level.width + x
        if (idx !in gridCells.indices) return

        val cell = gridCells[idx]
        val cellPipe = cell.pipe
        if (cellPipe != null && cellPipe.isMovable) {
            // Return to dock
            val updatedDock = dockPipes.toMutableList()
            updatedDock.add(cellPipe)
            dockPipes = updatedDock

            // Empty cell
            val updatedCells = gridCells.toMutableList()
            updatedCells[idx] = cell.copy(pipe = null)
            gridCells = updatedCells

            selectedDockPipeIndex = null
            movesCount++
            recalculateFlowsAndCheckCompletion()
            triggerSessionAutosave()
        }
    }

    // Place selected pipe on grid
    fun placePipeAt(x: Int, y: Int) {
        if (isSolved && !isTimeChallengeActive) return
        val level = activeLevel ?: return
        val idx = y * level.width + x
        if (idx !in gridCells.indices) return

        val cell = gridCells[idx]

        // 1. If grid cell already has a pipe, we cannot place another one on top
        if (cell.pipe != null) {
            return
        }

        // 2. If slot is empty and we have a dock pipe selected, place it!
        val selectedIdx = selectedDockPipeIndex ?: return
        if (selectedIdx in dockPipes.indices) {
            val pipeToPlace = dockPipes[selectedIdx]

            // Place on grid
            val updatedCells = gridCells.toMutableList()
            updatedCells[idx] = cell.copy(pipe = pipeToPlace)
            gridCells = updatedCells

            // Remove from dock
            val updatedDock = dockPipes.toMutableList()
            updatedDock.removeAt(selectedIdx)
            dockPipes = updatedDock

            selectedDockPipeIndex = null
            movesCount++
            recalculateFlowsAndCheckCompletion()
            triggerSessionAutosave()
        }
    }

    private fun recalculateFlowsAndCheckCompletion() {
        val level = activeLevel ?: return
        
        // 1. Run color propagation
        val resolved = propagateFlows(level.width, level.height, gridCells)
        gridCells = resolved

        // 2. Check targets
        var allTargetsMet = true
        var targetCount = 0

        for (cell in gridCells) {
            if (cell.cellType is GridCellType.Target) {
                targetCount++
                val reqColor = cell.cellType.requiredColor
                val gotColor = cell.actualColor
                if (reqColor != gotColor) {
                    allTargetsMet = false
                }
            }
        }

        if (targetCount > 0 && allTargetsMet) {
            handleLevelSolved()
        }
    }

    private fun propagateFlows(width: Int, height: Int, cells: List<GridCell>): List<GridCell> {
        // Initialize active colors
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
                            
                            // Check link:
                            // Neighbor must output in our direction (dir.opposite)
                            // And this cell must accept input from neighbor direction (dir)
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

    private fun handleLevelSolved() {
        isSolved = true
        stopTimer()

        if (isTimeChallengeActive) {
            // Player solved a speed mystery!
            // Grant score bonus and time bonus
            challengeSolvedPuzzlesCount++
            challengeScoreTotal += 500 + maxOf(0, 50 - movesCount) * 10
            challengeRemainingTimeSec = minOf(180, challengeRemainingTimeSec + 25) // add time, max 3 min

            viewModelScope.launch {
                delay(1200) // allow solved animation to play beautifully
                generateNextChallengeLevel()
                startTimer()
            }
        } else {
            // Normal level complete
            val level = activeLevel ?: return
            
            // Calculate Stars
            earnedStars = when {
                movesCount <= level.maxMovesForThreeStars -> 3
                movesCount <= level.maxMovesForTwoStars -> 2
                else -> 1
            }

            // Calculate score
            activeScore = maxOf(200, 3000 - (movesCount * 8) - (elapsedTimeSec * 2))

            viewModelScope.launch {
                // Persist score achievement in database
                val entity = LevelScoreEntity(
                    levelId = level.id,
                    stars = earnedStars,
                    bestScore = activeScore,
                    bestMoves = movesCount
                )
                repository.saveScore(entity)
                
                // Clear active autosave
                repository.clearSession()
                hasSessionToResume = false
            }
        }
    }

    fun submitLeaderboardScore(name: String) {
        val trimmed = name.trim().ifEmpty { "Giocatore_ID" }
        viewModelScope.launch {
            repository.insertLeaderboardEntry(
                LeaderboardEntryEntity(
                    playerName = trimmed,
                    score = challengeScoreTotal,
                    isLocalPlayer = true
                )
            )
            repository.clearSession()
            hasSessionToResume = false
            navigateTo(ScreenStatus.LEADERBOARD)
        }
    }

    // AUTOSAVE Implementation
    private fun triggerSessionAutosave() {
        val level = activeLevel ?: return
        viewModelScope.launch {
            val converter = Converters()
            val gridJson = converter.gridCellListToJson(gridCells)
            val dockJson = converter.pipeListToJson(dockPipes)

            val session = SavedSessionEntity(
                levelId = level.id,
                isProcedural = level.isProcedural,
                gridWidth = level.width,
                gridHeight = level.height,
                serializedGrid = gridJson,
                serializedDock = dockJson,
                movesCount = movesCount,
                elapsedTimeSec = elapsedTimeSec,
                isChallenge = isTimeChallengeActive,
                challengeScore = challengeScoreTotal,
                targetTimeLimit = challengeRemainingTimeSec
            )
            repository.saveSession(session)
            hasSessionToResume = true
        }
    }

    private fun startTimer() {
        stopTimer()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (isTimeChallengeActive) {
                    challengeRemainingTimeSec--
                    if (challengeRemainingTimeSec <= 0) {
                        challengeRemainingTimeSec = 0
                        isSolved = true // stop play
                        stopTimer()
                        // Session gets cleared so you can't reuse game-over state
                        repository.clearSession()
                        hasSessionToResume = false
                        break
                    }
                } else {
                    elapsedTimeSec++
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}
