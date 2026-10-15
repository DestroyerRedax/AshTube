package com.ashrin.ashtube.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ashrin.ashtube.data.local.dao.DownloadDao
import com.ashrin.ashtube.data.local.dao.FavoriteDao
import com.ashrin.ashtube.data.local.dao.HistoryDao
import com.ashrin.ashtube.data.local.entities.DownloadEntity
import com.ashrin.ashtube.data.local.entities.FavoriteEntity
import com.ashrin.ashtube.data.local.entities.HistoryEntity

@Database(
    entities = [
        FavoriteEntity::class,
        HistoryEntity::class,
        DownloadEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AshTubeDatabase : RoomDatabase() {

    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
    abstract fun downloadDao(): DownloadDao

    companion object {
        @Volatile
        private var INSTANCE: AshTubeDatabase? = null

        fun getInstance(context: Context): AshTubeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AshTubeDatabase::class.java,
                    "ashtube.db",
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
