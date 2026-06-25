package com.nomitype.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "clipboard_items")
data class ClipboardItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val pinned: Boolean = false,
    val category: String = "All",
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface ClipboardDao {
    @Query("SELECT * FROM clipboard_items ORDER BY pinned DESC, createdAt DESC")
    fun all(): Flow<List<ClipboardItem>>

    @Query("SELECT * FROM clipboard_items ORDER BY pinned DESC, createdAt DESC")
    suspend fun allOnce(): List<ClipboardItem>

    @Insert
    suspend fun insert(item: ClipboardItem): Long

    @Update
    suspend fun update(item: ClipboardItem)

    @Delete
    suspend fun delete(item: ClipboardItem)

    @Query("DELETE FROM clipboard_items WHERE pinned = 0 AND createdAt < :before")
    suspend fun trimOlderThan(before: Long)

    @Query("SELECT COUNT(*) FROM clipboard_items WHERE pinned = 0")
    suspend fun unpinnedCount(): Int

    @Query("DELETE FROM clipboard_items WHERE id IN (SELECT id FROM clipboard_items WHERE pinned = 0 ORDER BY createdAt ASC LIMIT :n)")
    suspend fun trimOldest(n: Int)
}

@Database(
    entities = [ClipboardItem::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clipboardDao(): ClipboardDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nomitype.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
