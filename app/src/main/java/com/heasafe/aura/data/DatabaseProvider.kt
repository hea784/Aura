package com.heasafe.aura.data

import android.content.Context
import androidx.room.Room
import com.heasafe.aura.security.DbKeyStore
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

object DatabaseProvider {
    @Volatile
    private var instance: AuraDatabase? = null

    fun get(context: Context): AuraDatabase = instance ?: synchronized(this) {
        instance ?: build(context.applicationContext).also { instance = it }
    }

    private fun build(context: Context): AuraDatabase {
        System.loadLibrary("sqlcipher")
        val factory = SupportOpenHelperFactory(DbKeyStore.passphrase(context))
        val db = Room.databaseBuilder(context, AuraDatabase::class.java, "aura_enc.db")
            .openHelperFactory(factory)
            .build()

        // 一次性迁移：旧明文库 aura.db 的站点导入加密库后删除明文
        val plain = context.getDatabasePath("aura.db")
        val enc = context.getDatabasePath("aura_enc.db")
        if (plain.exists() && !enc.exists()) {
            try {
                val old = Room.databaseBuilder(context, AuraDatabase::class.java, "aura.db").build()
                val sites = runBlocking { old.savedSiteDao().allOnce() }
                runBlocking { sites.forEach { db.savedSiteDao().insert(it.copy(id = 0)) } }
                old.close()
                plain.delete()
            } catch (_: Exception) { /* 迁移失败不阻塞启动 */ }
        }
        return db
    }
}
