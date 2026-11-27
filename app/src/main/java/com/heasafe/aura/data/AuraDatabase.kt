package com.heasafe.aura.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** 持久化站点：用户添加后长期可用，只有手动删除才会消失。 */
@Entity(tableName = "saved_sites")
data class SavedSite(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val host: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "last_opened_at") val lastOpenedAt: Long = 0L,
    @ColumnInfo(name = "favorite") val favorite: Boolean = false,
    @ColumnInfo(name = "video_mode") val videoMode: String = "auto" // auto | short | normal
)

@Dao
interface SavedSiteDao {
    @Query("SELECT * FROM saved_sites ORDER BY favorite DESC, last_opened_at DESC, created_at DESC")
    fun all(): Flow<List<SavedSite>>

    @Query("SELECT * FROM saved_sites ORDER BY favorite DESC, last_opened_at DESC, created_at DESC")
    suspend fun allOnce(): List<SavedSite>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(site: SavedSite): Long

    @Delete
    suspend fun delete(site: SavedSite)

    @Query("UPDATE saved_sites SET last_opened_at = :ts WHERE id = :id")
    suspend fun touch(id: Long, ts: Long = System.currentTimeMillis())

    @Query("UPDATE saved_sites SET favorite = :fav WHERE id = :id")
    suspend fun setFavorite(id: Long, fav: Boolean)

    @Query("UPDATE saved_sites SET video_mode = :mode WHERE id = :id")
    suspend fun setVideoMode(id: Long, mode: String)
}

@Database(entities = [SavedSite::class], version = 1, exportSchema = false)
abstract class AuraDatabase : RoomDatabase() {
    abstract fun savedSiteDao(): SavedSiteDao
}
