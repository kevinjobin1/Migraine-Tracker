package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "migraine_logs")
data class MigraineLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateString: String, // e.g., "YYYY-MM-DD"
    val year: Int,
    val month: Int, // 1-indexed, e.g., 1 for January
    val day: Int,
    val note: String,
    val intensity: Int, // 1 to 10 scale
    val symptoms: String = "", // comma-separated
    val triggers: String = "", // comma-separated
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface MigraineDao {
    @Query("SELECT * FROM migraine_logs ORDER BY dateString DESC, timestamp DESC")
    fun getAllLogs(): Flow<List<MigraineLog>>

    @Query("SELECT * FROM migraine_logs WHERE year = :year AND month = :month ORDER BY day ASC")
    fun getLogsForMonth(year: Int, month: Int): Flow<List<MigraineLog>>

    @Query("SELECT * FROM migraine_logs WHERE dateString = :dateString LIMIT 1")
    suspend fun getLogByDate(dateString: String): MigraineLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: MigraineLog): Long

    @Query("DELETE FROM migraine_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    @Query("DELETE FROM migraine_logs WHERE dateString = :dateString")
    suspend fun deleteLogByDate(dateString: String)
}

@Database(entities = [MigraineLog::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun migraineDao(): MigraineDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "migraine_tracker_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class MigraineRepository(private val migraineDao: MigraineDao) {
    val allLogs: Flow<List<MigraineLog>> = migraineDao.getAllLogs()

    fun getLogsForMonth(year: Int, month: Int): Flow<List<MigraineLog>> {
        return migraineDao.getLogsForMonth(year, month)
    }

    suspend fun getLogByDate(dateString: String): MigraineLog? {
        return migraineDao.getLogByDate(dateString)
    }

    suspend fun insertLog(log: MigraineLog): Long {
        return migraineDao.insertLog(log)
    }

    suspend fun deleteLogById(id: Long) {
        migraineDao.deleteLogById(id)
    }

    suspend fun deleteLogByDate(dateString: String) {
        migraineDao.deleteLogByDate(dateString)
    }
}
