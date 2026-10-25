package com.aetheria.ai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.aetheria.ai.data.local.dao.ChatDao
import com.aetheria.ai.data.local.dao.CustomModelDao
import com.aetheria.ai.data.local.entity.ChatMessageEntity
import com.aetheria.ai.data.local.entity.CustomModelEntity

@Database(
    entities = [CustomModelEntity::class, ChatMessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AetheriaDatabase : RoomDatabase() {

    abstract fun customModelDao(): CustomModelDao
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: AetheriaDatabase? = null

        fun getDatabase(context: Context): AetheriaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AetheriaDatabase::class.java,
                    "aetheria_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
