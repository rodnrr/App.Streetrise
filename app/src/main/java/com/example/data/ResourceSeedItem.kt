package com.example.data

data class ResourceSeedItem(
  val id: String, // Constructed as "name_category_lat"
  val org: String,
  val name: String,
  val category: String,
  val subcategory: String,
  val address: String,
  val phone: String,
  val website: String,
  val hours: String,
  val description: String,
  val county: String,
  val lat: Double,
  val lng: Double,
  val isBookmarked: Boolean = false,
  val notes: String = ""
)
