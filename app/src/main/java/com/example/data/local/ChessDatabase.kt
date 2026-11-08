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
import kotlinx.coroutines.flow.Flow

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

@Database(
    entities = [SavedGameEntity::class, UserPreferencesEntity::class, JoinedItemEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ChessDatabase : RoomDatabase() {
    abstract fun chessDao(): ChessDao

    companion object {
        @Volatile
        private var INSTANCE: ChessDatabase? = null

        fun getInstance(context: Context): ChessDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChessDatabase::class.java,
                    "librechess_studio.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
