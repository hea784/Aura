package com.heasafe.aura.data

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    @Volatile
    private var instance: AuraDatabase? = null

    fun get(context: Context): AuraDatabase = instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(
            context.applicationContext,
            AuraDatabase::class.java,
            "aura.db"
        ).build().also { instance = it }
    }
}
