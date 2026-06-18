package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_resources")
data class SavedRes(
  @PrimaryKey val resourceId: String, // Constructed as "org_name_category_lat"
  val isBookmarked: Boolean = false,
  val notes: String = "",
  val timestamp: Long = System.currentTimeMillis()
)
