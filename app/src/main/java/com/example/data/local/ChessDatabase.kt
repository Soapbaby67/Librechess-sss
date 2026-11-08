package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.util.AppDiagnosticLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update

@Entity(tableName = "saved_games")
data class SavedGameEntity(
    @PrimaryKey val gameId: String,
    val whitePlayer: String,
    val whiteRating: Int,
    val blackPlayer: String,
    val blackRating: Int,
    val speed: String,
    val openingEco: String,
    val openingName: String,
    val resultText: String,
    val playedAtLabel: String,
    val movesSanOrUci: String,
    val evalsCsv: String,
    val clocksCsv: String,
    val whiteAccuracy: Double,
    val blackAccuracy: Double,
    val blundersCount: Int,
    val savedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey val id: Int = 1,
    val appUiThemeId: String = "OBSIDIAN_GOLD",
    val boardSkinId: String = "CHESS_COM_GREEN",
    val pieceStyleId: String = "NEO_CLASSIC",
    val lichessUsername: String = "DrNykterstein",
    val lichessApiToken: String = "",
    val showCoordinates: Boolean = true,
    val showMoveClassificationBadges: Boolean = true,
    val showEvalBar: Boolean = true,
    val showBestMoveArrowInAnalysis: Boolean = true
)

@Entity(tableName = "joined_items")
data class JoinedItemEntity(
    @PrimaryKey val itemId: String,
    val itemType: String, // "TOURNAMENT" or "CLUB"
    val title: String,
    val joinedAt: Long = System.currentTimeMillis()
)

@Dao
interface ChessDao {
    @Query("SELECT * FROM saved_games ORDER BY savedTimestamp DESC")
    fun observeSavedGames(): Flow<List<SavedGameEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGames(games: List<SavedGameEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: SavedGameEntity)

    @Query("SELECT * FROM user_preferences WHERE id = 1")
    fun observePreferences(): Flow<UserPreferencesEntity?>

    @Query("SELECT * FROM user_preferences WHERE id = 1")
    suspend fun getPreferencesOnce(): UserPreferencesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePreferences(prefs: UserPreferencesEntity)

    @Query("SELECT * FROM joined_items ORDER BY joinedAt DESC")
    fun observeJoinedItems(): Flow<List<JoinedItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJoinedItem(item: JoinedItemEntity)

    @Query("DELETE FROM joined_items WHERE itemId = :itemId")
    suspend fun deleteJoinedItem(itemId: String)
}

/**
 * Fault-tolerant DAO that delegates to Room SQLite when available and automatically
 * falls back to SharedPreferences + in-memory StateFlow if Room/SQLite/KSP fails on device.
 */
class ResilientChessDao(
    context: Context,
    private val roomDao: ChessDao?
) : ChessDao {

    private val prefs = context.applicationContext.getSharedPreferences("librechess_prefs_store", Context.MODE_PRIVATE)

    private val fallbackPrefsFlow = MutableStateFlow<UserPreferencesEntity?>(loadPrefsFromSharedPreferences())
    private val fallbackGamesFlow = MutableStateFlow<List<SavedGameEntity>>(emptyList())
    private val fallbackJoinedFlow = MutableStateFlow<List<JoinedItemEntity>>(emptyList())

    private fun loadPrefsFromSharedPreferences(): UserPreferencesEntity {
        return UserPreferencesEntity(
            appUiThemeId = prefs.getString("appUiThemeId", "OBSIDIAN_GOLD") ?: "OBSIDIAN_GOLD",
            boardSkinId = prefs.getString("boardSkinId", "CHESS_COM_GREEN") ?: "CHESS_COM_GREEN",
            pieceStyleId = prefs.getString("pieceStyleId", "NEO_CLASSIC") ?: "NEO_CLASSIC",
            lichessUsername = prefs.getString("lichessUsername", "DrNykterstein") ?: "DrNykterstein",
            lichessApiToken = prefs.getString("lichessApiToken", "") ?: "",
            showCoordinates = prefs.getBoolean("showCoordinates", true),
            showMoveClassificationBadges = prefs.getBoolean("showMoveClassificationBadges", true),
            showEvalBar = prefs.getBoolean("showEvalBar", true),
            showBestMoveArrowInAnalysis = prefs.getBoolean("showBestMoveArrowInAnalysis", true)
        )
    }

    private fun savePrefsToSharedPreferences(entity: UserPreferencesEntity) {
        runCatching {
            prefs.edit()
                .putString("appUiThemeId", entity.appUiThemeId)
                .putString("boardSkinId", entity.boardSkinId)
                .putString("pieceStyleId", entity.pieceStyleId)
                .putString("lichessUsername", entity.lichessUsername)
                .putString("lichessApiToken", entity.lichessApiToken)
                .putBoolean("showCoordinates", entity.showCoordinates)
                .putBoolean("showMoveClassificationBadges", entity.showMoveClassificationBadges)
                .putBoolean("showEvalBar", entity.showEvalBar)
                .putBoolean("showBestMoveArrowInAnalysis", entity.showBestMoveArrowInAnalysis)
                .apply()
        }
    }

    override fun observeSavedGames(): Flow<List<SavedGameEntity>> {
        val delegate = roomDao
        return if (delegate != null) {
            runCatching {
                delegate.observeSavedGames().catch { e ->
                    AppDiagnosticLogger.error("RoomDB", "Ошибка чтения saved_games, переключение на память", e)
                    emit(fallbackGamesFlow.value)
                }
            }.getOrElse { fallbackGamesFlow.asStateFlow() }
        } else {
            fallbackGamesFlow.asStateFlow()
        }
    }

    override suspend fun insertGames(games: List<SavedGameEntity>) {
        fallbackGamesFlow.update { existing ->
            (games + existing).distinctBy { it.gameId }
        }
        runCatching { roomDao?.insertGames(games) }.onFailure { e ->
            AppDiagnosticLogger.warn("RoomDB", "insertGames сохранен в кэш памяти: ${e.message}")
        }
    }

    override suspend fun insertGame(game: SavedGameEntity) {
        fallbackGamesFlow.update { existing ->
            (listOf(game) + existing).distinctBy { it.gameId }
        }
        runCatching { roomDao?.insertGame(game) }.onFailure { e ->
            AppDiagnosticLogger.warn("RoomDB", "insertGame сохранен в кэш памяти: ${e.message}")
        }
    }

    override fun observePreferences(): Flow<UserPreferencesEntity?> {
        val delegate = roomDao
        return if (delegate != null) {
            runCatching {
                delegate.observePreferences().catch { e ->
                    AppDiagnosticLogger.error("RoomDB", "Ошибка чтения user_preferences, резерв SharedPreferences", e)
                    emit(fallbackPrefsFlow.value)
                }
            }.getOrElse { fallbackPrefsFlow.asStateFlow() }
        } else {
            fallbackPrefsFlow.asStateFlow()
        }
    }

    override suspend fun getPreferencesOnce(): UserPreferencesEntity? {
        return runCatching { roomDao?.getPreferencesOnce() }.getOrNull() ?: fallbackPrefsFlow.value
    }

    override suspend fun savePreferences(prefs: UserPreferencesEntity) {
        savePrefsToSharedPreferences(prefs)
        fallbackPrefsFlow.value = prefs
        runCatching { roomDao?.savePreferences(prefs) }.onFailure { e ->
            AppDiagnosticLogger.warn("RoomDB", "Настройки сохранены в SharedPreferences (${e.message})")
        }
    }

    override fun observeJoinedItems(): Flow<List<JoinedItemEntity>> {
        val delegate = roomDao
        return if (delegate != null) {
            runCatching {
                delegate.observeJoinedItems().catch { e ->
                    AppDiagnosticLogger.error("RoomDB", "Ошибка чтения joined_items", e)
                    emit(fallbackJoinedFlow.value)
                }
            }.getOrElse { fallbackJoinedFlow.asStateFlow() }
        } else {
            fallbackJoinedFlow.asStateFlow()
        }
    }

    override suspend fun insertJoinedItem(item: JoinedItemEntity) {
        fallbackJoinedFlow.update { current ->
            (listOf(item) + current).distinctBy { it.itemId }
        }
        runCatching { roomDao?.insertJoinedItem(item) }
    }

    override suspend fun deleteJoinedItem(itemId: String) {
        fallbackJoinedFlow.update { current ->
            current.filterNot { it.itemId == itemId }
        }
        runCatching { roomDao?.deleteJoinedItem(itemId) }
    }
}

@Database(
    entities = [SavedGameEntity::class, UserPreferencesEntity::class, JoinedItemEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ChessDatabase : RoomDatabase() {
    abstract fun chessDao(): ChessDao

    companion object {
        @Volatile
        private var SAFE_DAO_INSTANCE: ChessDao? = null

        fun getSafeDao(context: Context): ChessDao {
            return SAFE_DAO_INSTANCE ?: synchronized(this) {
                SAFE_DAO_INSTANCE ?: run {
                    val roomDao = runCatching {
                        val db = Room.databaseBuilder(
                            context.applicationContext,
                            ChessDatabase::class.java,
                            "librechess_studio.db"
                        ).fallbackToDestructiveMigration().build()
                        db.chessDao()
                    }.onSuccess {
                        AppDiagnosticLogger.ok("RoomDB", "База данных Room SQLite инициализирована")
                    }.onFailure { err ->
                        AppDiagnosticLogger.error(
                            "RoomDB",
                            "Не удалось инициализировать Room, включен резервный драйвер SharedPreferences",
                            err
                        )
                    }.getOrNull()

                    val resilient = ResilientChessDao(context.applicationContext, roomDao)
                    SAFE_DAO_INSTANCE = resilient
                    resilient
                }
            }
        }
    }
}
