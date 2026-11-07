package com.example.data

data class RealTimeService(
  val id: String,
  val name: String,
  val category: String, // shelter, food, hygiene, medical, work_exchange
  val address: String,
  val phone: String,
  val operatingHours: String,
  val description: String,
  val latitude: Double,
  val longitude: Double,
  val sourceUrl: String = "",
  val sourceTitle: String = "Google Search Grounded",
  val isLiveVerified: Boolean = true
)

enum class SearchSource {
  GOOGLE_SEARCH_LIVE,
  VERIFIED_OFFLINE_CACHE
}

data class RileyDialogueMessage(
  val id: String = java.util.UUID.randomUUID().toString(),
  val sender: MessageSender,
  val text: String,
  val timestamp: String = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date()),
  val serviceReferences: List<RealTimeService> = emptyList()
)

enum class MessageSender {
  RILEY,
  USER,
  SYSTEM
}
