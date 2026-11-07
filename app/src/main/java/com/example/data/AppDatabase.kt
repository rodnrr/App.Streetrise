package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SavedRes::class, CachedService::class, CachedProviderProfile::class, CommunityMessage::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

  abstract fun savedResDao(): SavedResDao
  abstract fun cachedServiceDao(): CachedServiceDao
  abstract fun communityMessageDao(): CommunityMessageDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "streetrise_local_db"
        )
        .fallbackToDestructiveMigration()
        .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
