package com.example.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.IOException

class SavedResViewModel(application: Application) : AndroidViewModel(application) {

  private val repository: SavedResRepository
  private val assetResources = mutableListOf<ResourceSeedItem>()

  // UI constraints / filters
  val searchQuery = MutableStateFlow("")
  val selectedCategory = MutableStateFlow<String?>(null)

  init {
    val database = AppDatabase.getDatabase(application)
    repository = SavedResRepository(database.savedResDao())
    loadResourcesFromAssets()
  }

  // Load the curated seed resources JSON
  private fun loadResourcesFromAssets() {
    try {
      val context = getApplication<Application>().applicationContext
      val jsonString = context.assets.open("resources_seed.json").bufferedReader().use { it.readText() }
      val jsonArray = JSONArray(jsonString)
      for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)
        val orgName = obj.optString("org", "")
        val resourceName = obj.optString("name", "")
        val category = obj.optString("category", "")
        val subcategory = obj.optString("subcategory", "")
        val address = obj.optString("address", "")
        val phone = obj.optString("phone", "")
        val website = obj.optString("website", "")
        val hours = obj.optString("hours", "")
        val description = obj.optString("description", "")
        val county = obj.optString("county", "")
        val lat = obj.optDouble("lat", 0.0)
        val lng = obj.optDouble("lng", 0.0)

        // Unique ID combination
        val id = "${orgName.replace(" ", "_")}_${resourceName.replace(" ", "_")}_$lat".lowercase()

        assetResources.add(
          ResourceSeedItem(
            id = id,
            org = orgName,
            name = resourceName,
            category = category,
            subcategory = subcategory,
            address = address,
            phone = phone,
            website = website,
            hours = hours,
            description = description,
            county = county,
            lat = lat,
            lng = lng
          )
        )
      }
    } catch (e: IOException) {
      e.printStackTrace()
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  // Combine static assets resources with the local SQLite custom states reactive flow
  val resourcesState: StateFlow<List<ResourceSeedItem>> = repository.allSaved
    .combine(searchQuery) { savedList, query ->
      Pair(savedList, query)
    }
    .combine(selectedCategory) { pair, cat ->
      val savedList = pair.first
      val query = pair.second
      val savedMap = savedList.associateBy { it.resourceId }

      assetResources.map { item ->
        val savedState = savedMap[item.id]
        item.copy(
          isBookmarked = savedState?.isBookmarked ?: false,
          notes = savedState?.notes ?: ""
        )
      }.filter { item ->
        // Search filter matching
        val matchesQuery = query.isEmpty() ||
            item.name.contains(query, ignoreCase = true) ||
            item.org.contains(query, ignoreCase = true) ||
            item.description.contains(query, ignoreCase = true) ||
            item.county.contains(query, ignoreCase = true) ||
            item.address.contains(query, ignoreCase = true)

        // Category filter matching
        val matchesCategory = cat == null || item.category.equals(cat, ignoreCase = true)

        matchesQuery && matchesCategory
      }
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

  // Expose ONLY bookmarked listings dynamically
  val bookmarkedResources: StateFlow<List<ResourceSeedItem>> = resourcesState
    .combine(MutableStateFlow(true)) { list, _ ->
      list.filter { it.isBookmarked }
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

  fun toggleBookmark(resourceId: String) {
    viewModelScope.launch {
      val existing = repository.getById(resourceId)
      if (existing != null) {
        val updated = existing.copy(isBookmarked = !existing.isBookmarked, timestamp = System.currentTimeMillis())
        if (!updated.isBookmarked && updated.notes.isEmpty()) {
          // Clean up database if no longer bookmarked or has notes
          repository.deleteById(resourceId)
        } else {
          repository.save(updated)
        }
      } else {
        // Create new row
        val initial = SavedRes(resourceId = resourceId, isBookmarked = true)
        repository.save(initial)
      }
    }
  }

  fun updateNotes(resourceId: String, notes: String) {
    viewModelScope.launch {
      val existing = repository.getById(resourceId)
      if (existing != null) {
        val updated = existing.copy(notes = notes, timestamp = System.currentTimeMillis())
        if (!updated.isBookmarked && updated.notes.trim().isEmpty()) {
          repository.deleteById(resourceId)
        } else {
          repository.save(updated)
        }
      } else {
        if (notes.trim().isNotEmpty()) {
          val initial = SavedRes(resourceId = resourceId, notes = notes.trim())
          repository.save(initial)
        }
      }
    }
  }
}
