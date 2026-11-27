package com.heasafe.aura.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** 持久化日志条目：安全事件 / 视频探测 / 更新检查 / 应用事件，跨会话保留。 */
@Entity(tableName = "logs")
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val time: Long,
    val type: String,
    val detail: String
)

@Dao
interface LogDao {
    @Query("SELECT * FROM logs ORDER BY time DESC LIMIT :limit")
    fun recent(limit: Int): Flow<List<LogEntry>>

    @Insert
    suspend fun insert(entry: LogEntry)

    @Query("DELETE FROM logs")
    suspend fun clear()
}

@Database(entities = [LogEntry::class], version = 1, exportSchema = false)
abstract class LogDatabase : RoomDatabase() {
    abstract fun logDao(): LogDao
}

object LogProvider {
    @Volatile
    private var instance: LogDatabase? = null

    fun get(context: Context): LogDatabase = instance ?: synchronized(this) {
        instance ?: build(context.applicationContext).also { instance = it }
    }

    private fun build(context: Context): LogDatabase {
        System.loadLibrary("sqlcipher")
        val factory = net.zetetic.database.sqlcipher.SupportOpenHelperFactory(
            com.heasafe.aura.security.DbKeyStore.passphrase(context)
        )
        // 旧明文日志库不迁移（日志非关键），直接删除改用加密库
        context.getDatabasePath("aura_logs.db").delete()
        return Room.databaseBuilder(context, LogDatabase::class.java, "aura_logs_enc.db")
            .openHelperFactory(factory)
            .build()
    }
}

/** 异步落盘日志，绝不阻塞 WebView 主线程回调。 */
object Logger {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun add(context: Context, type: String, detail: String) {
        scope.launch {
            try {
                LogProvider.get(context).logDao()
                    .insert(LogEntry(time = System.currentTimeMillis(), type = type, detail = detail))
            } catch (_: Exception) { /* 日志失败不影响主流程 */ }
        }
    }

    fun clear(context: Context) {
        scope.launch {
            try { LogProvider.get(context).logDao().clear() } catch (_: Exception) {}
        }
    }
}
