package com.example.db

import android.content.Context
import androidx.room.*
import com.example.game.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "level_scores")
data class LevelScoreEntity(
    @PrimaryKey val levelId: Int,
    val stars: Int,
    val bestScore: Int,
    val bestMoves: Int,
    val completedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_session")
data class SavedSessionEntity(
    @PrimaryKey val id: Int = 1, // Singleton session state
    val levelId: Int,
    val isProcedural: Boolean,
    val gridWidth: Int,
    val gridHeight: Int,
    val serializedGrid: String, // JSON representation of GridCell list
    val serializedDock: String, // JSON representation of PipeDef list
    val movesCount: Int,
    val elapsedTimeSec: Int,
    val isChallenge: Boolean = false,
    val challengeScore: Int = 0,
    val targetTimeLimit: Int = 90
)

@Entity(tableName = "leaderboard")
data class LeaderboardEntryEntity(
    @PrimaryKey(autoGenerate = true) val entryId: Int = 0,
    val playerName: String,
    val score: Int,
    val isLocalPlayer: Boolean = false,
    val dateLong: Long = System.currentTimeMillis()
)

class Converters {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // Since GridCell contains polymorph GridCellType, we serialize coordinates, cell type info and pipes
    // We can use simplified flat JSON objects to guarantee stable, painless Moshi parsing
    @TypeConverter
    fun gridCellListToJson(list: List<GridCell>?): String {
        if (list == null) return "[]"
        val cellDetailsList = list.map { FlatCellDef.fromGridCell(it) }
        val adapter = moshi.adapter<List<FlatCellDef>>(
            Types.newParameterizedType(List::class.java, FlatCellDef::class.java)
        )
        return adapter.toJson(cellDetailsList) ?: "[]"
    }

    @TypeConverter
    fun jsonToGridCellList(json: String?): List<GridCell> {
        if (json.isNullOrEmpty()) return emptyList()
        val adapter = moshi.adapter<List<FlatCellDef>>(
            Types.newParameterizedType(List::class.java, FlatCellDef::class.java)
        )
        val list = adapter.fromJson(json) ?: return emptyList()
        return list.map { it.toGridCell() }
    }

    @TypeConverter
    fun pipeListToJson(list: List<PipeDef>?): String {
        if (list == null) return "[]"
        val adapter = moshi.adapter<List<PipeDef>>(
            Types.newParameterizedType(List::class.java, PipeDef::class.java)
        )
        return adapter.toJson(list) ?: "[]"
    }

    @TypeConverter
    fun jsonToPipeList(json: String?): List<PipeDef> {
        if (json.isNullOrEmpty()) return emptyList()
        val adapter = moshi.adapter<List<PipeDef>>(
            Types.newParameterizedType(List::class.java, PipeDef::class.java)
        )
        return adapter.fromJson(json) ?: emptyList()
    }
}

// Flat definition to help Moshi serialize polymorphic structures painlessly
data class FlatCellDef(
    val x: Int,
    val y: Int,
    val typeName: String, // "SOURCE", "TARGET", "NORMAL"
    val colorR: Boolean = false,
    val colorG: Boolean = false,
    val colorB: Boolean = false,
    val emissionDirName: String = "UP",
    val pipeType: String? = null,
    val pipeRotation: Int = 0,
    val pipeFixed: Boolean = false,
    val pipeRotatable: Boolean = true,
    val pipeMovable: Boolean = false
) {
    companion object {
        fun fromGridCell(cell: GridCell): FlatCellDef {
            val (typeName, r, g, b, dir) = when (val ct = cell.cellType) {
                is GridCellType.Source -> FlatCellDefTuple("SOURCE", ct.color.r, ct.color.g, ct.color.b, ct.emissionDir.name)
                is GridCellType.Target -> FlatCellDefTuple("TARGET", ct.requiredColor.r, ct.requiredColor.g, ct.requiredColor.b, "UP")
                GridCellType.Normal -> FlatCellDefTuple("NORMAL", false, false, false, "UP")
            }
            return FlatCellDef(
                x = cell.x,
                y = cell.y,
                typeName = typeName,
                colorR = r,
                colorG = g,
                colorB = b,
                emissionDirName = dir,
                pipeType = cell.pipe?.type?.name,
                pipeRotation = cell.pipe?.rotation ?: 0,
                pipeFixed = cell.pipe?.isFixed ?: false,
                pipeRotatable = cell.pipe?.isRotatable ?: true,
                pipeMovable = cell.pipe?.isMovable ?: false
            )
        }
    }

    fun toGridCell(): GridCell {
        val cellType = when (typeName) {
            "SOURCE" -> GridCellType.Source(FluidColor(colorR, colorG, colorB), Direction.valueOf(emissionDirName))
            "TARGET" -> GridCellType.Target(FluidColor(colorR, colorG, colorB))
            else -> GridCellType.Normal
        }
        val pipeObj = pipeType?.let {
            PipeDef(
                type = PipeType.valueOf(it),
                rotation = pipeRotation,
                isFixed = pipeFixed,
                isRotatable = pipeRotatable,
                isMovable = pipeMovable
            )
        }
        return GridCell(
            x = x,
            y = y,
            cellType = cellType,
            pipe = pipeObj
        )
    }
}

private data class FlatCellDefTuple(
    val typeName: String,
    val r: Boolean,
    val g: Boolean,
    val b: Boolean,
    val dir: String
)

@Dao
interface GameDao {
    @Query("SELECT * FROM level_scores")
    fun getAllScores(): Flow<List<LevelScoreEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveScore(score: LevelScoreEntity)

    @Query("SELECT * FROM saved_session WHERE id = 1")
    suspend fun getSavedSession(): SavedSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSession(session: SavedSessionEntity)

    @Query("DELETE FROM saved_session WHERE id = 1")
    suspend fun clearSession()

    @Query("SELECT * FROM leaderboard ORDER BY score DESC LIMIT 100")
    fun getLeaderboard(): Flow<List<LeaderboardEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeaderboardEntry(entry: LeaderboardEntryEntity)

    @Query("SELECT COUNT(*) FROM leaderboard")
    suspend fun getLeaderboardCount(): Int
}

@Database(
    entities = [LevelScoreEntity::class, SavedSessionEntity::class, LeaderboardEntryEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PipeFlowDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        @Volatile
        private var INSTANCE: PipeFlowDatabase? = null

        fun getDatabase(context: Context): PipeFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PipeFlowDatabase::class.java,
                    "pipe_flow_puzzle_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class GameRepository(private val gameDao: GameDao) {
    val allScores: Flow<List<LevelScoreEntity>> = gameDao.getAllScores()
    val leaderboard: Flow<List<LeaderboardEntryEntity>> = gameDao.getLeaderboard()

    suspend fun saveScore(score: LevelScoreEntity) = gameDao.saveScore(score)
    suspend fun getSavedSession() = gameDao.getSavedSession()
    suspend fun saveSession(session: SavedSessionEntity) = gameDao.saveSession(session)
    suspend fun clearSession() = gameDao.clearSession()
    suspend fun insertLeaderboardEntry(entry: LeaderboardEntryEntity) = gameDao.insertLeaderboardEntry(entry)
    suspend fun getLeaderboardCount() = gameDao.getLeaderboardCount()
}
