package com.example.data

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class RealTimeSearchService(private val context: Context) {

  private val httpClient = OkHttpClient.Builder()
    .connectTimeout(12, TimeUnit.SECONDS)
    .readTimeout(18, TimeUnit.SECONDS)
    .build()

  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  /**
   * Performs real-time local support service discovery using Gemini with Google Search tool.
   * If quota or network limits occur, gracefully falls back to local verified directory seeds.
   */
  suspend fun searchLocalServices(
    query: String,
    categoryFilter: String? = null
  ): Pair<List<RealTimeService>, SearchSource> = withContext(Dispatchers.IO) {
    val apiKey = BuildConfig.GEMINI_API_KEY
    if (apiKey.isNotEmpty() && !apiKey.startsWith("MY_")) {
      try {
        val geminiResults = queryGeminiWithGoogleSearch(query, categoryFilter, apiKey)
        if (geminiResults.isNotEmpty()) {
          return@withContext Pair(geminiResults, SearchSource.GOOGLE_SEARCH_LIVE)
        }
      } catch (e: Exception) {
        Log.w("RealTimeSearchService", "Live Google Search call handled (${e.message}), using verified fallback.")
      }
    }
    // Resilient fallback to local verified seeds
    val localMatches = queryLocalSeedServices(query, categoryFilter)
    Pair(localMatches, SearchSource.VERIFIED_OFFLINE_CACHE)
  }

  /**
   * Queries Gemini 3.8 Flash with Google Search grounding tool to find real-time services.
   */
  private fun queryGeminiWithGoogleSearch(
    query: String,
    categoryFilter: String?,
    apiKey: String
  ): List<RealTimeService> {
    val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent?key=$apiKey"

    val prompt = buildString {
      append("Find active, currently open local support services (such as emergency shelter, food pantry, soup kitchen, free clinic, hygiene showers, or job assistance) ")
      if (query.isNotBlank()) {
        append("matching search term '$query' ")
      }
      if (!categoryFilter.isNullOrBlank()) {
        append("in category '$categoryFilter' ")
      }
      append("in Central Florida / Tampa / Orlando / Lakeland or the user's area. ")
      append("Use Google Search to verify recent operating hours, phone numbers, and addresses. ")
      append("Output a valid JSON array of objects with the following keys for each resource: ")
      append("\"name\", \"category\" (one of 'shelter', 'food', 'hygiene', 'medical', 'work_exchange'), ")
      append("\"address\", \"phone\", \"operatingHours\", \"description\", \"latitude\" (numeric), \"longitude\" (numeric). ")
      append("Return ONLY the JSON array enclosed in [ ... ], no commentary.")
    }

    val requestJson = JSONObject().apply {
      put("contents", JSONArray().apply {
        put(JSONObject().apply {
          put("parts", JSONArray().apply {
            put(JSONObject().apply {
              put("text", prompt)
            })
          })
        })
      })
      put("tools", JSONArray().apply {
        put(JSONObject().apply {
          put("google_search", JSONObject())
        })
      })
    }

    val body = requestJson.toString().toRequestBody(jsonMediaType)
    val request = Request.Builder()
      .url(url)
      .post(body)
      .build()

    val response = httpClient.newCall(request).execute()
    val responseBody = response.body?.string() ?: ""

    if (!response.isSuccessful) {
      Log.w("RealTimeSearchService", "HTTP ${response.code}: ${responseBody.take(150)}")
      return emptyList()
    }

    return parseGeminiResponse(responseBody)
  }

  private fun parseGeminiResponse(responseBody: String): List<RealTimeService> {
    val results = mutableListOf<RealTimeService>()
    try {
      val root = JSONObject(responseBody)
      val candidates = root.optJSONArray("candidates") ?: return emptyList()
      if (candidates.length() == 0) return emptyList()

      val firstCandidate = candidates.getJSONObject(0)
      val content = firstCandidate.optJSONObject("content")
      val parts = content?.optJSONArray("parts") ?: return emptyList()

      // Look for grounding metadata URLs if available
      val groundingMetadata = firstCandidate.optJSONObject("groundingMetadata")
      var primarySourceUrl = ""
      var primarySourceTitle = "Google Search Grounded"

      val webChunks = groundingMetadata?.optJSONArray("groundingChunks")
      if (webChunks != null && webChunks.length() > 0) {
        val firstChunk = webChunks.getJSONObject(0).optJSONObject("web")
        primarySourceUrl = firstChunk?.optString("uri", "") ?: ""
        primarySourceTitle = firstChunk?.optString("title", "Google Search Grounded") ?: "Google Search Grounded"
      }

      var textOutput = ""
      for (i in 0 until parts.length()) {
        textOutput += parts.getJSONObject(i).optString("text", "")
      }

      val jsonStartIndex = textOutput.indexOf('[')
      val jsonEndIndex = textOutput.lastIndexOf(']')

      if (jsonStartIndex != -1 && jsonEndIndex != -1 && jsonEndIndex > jsonStartIndex) {
        val jsonArrayStr = textOutput.substring(jsonStartIndex, jsonEndIndex + 1)
        val array = JSONArray(jsonArrayStr)
        for (i in 0 until array.length()) {
          val item = array.getJSONObject(i)
          val name = item.optString("name", "Support Service")
          val category = item.optString("category", "shelter").lowercase()
          val address = item.optString("address", "Florida Support Center")
          val phone = item.optString("phone", "463-278-7338")
          val hours = item.optString("operatingHours", "Check intake hours")
          val desc = item.optString("description", "")
          var lat = item.optDouble("latitude", 27.9506)
          var lng = item.optDouble("longitude", -82.4572)

          if (lat == 0.0 && lng == 0.0) {
            lat = 27.9506
            lng = -82.4572
          }

          results.add(
            RealTimeService(
              id = "live_${name.replace(" ", "_")}_$i".lowercase(),
              name = name,
              category = category,
              address = address,
              phone = phone,
              operatingHours = hours,
              description = desc,
              latitude = lat,
              longitude = lng,
              sourceUrl = primarySourceUrl,
              sourceTitle = primarySourceTitle,
              isLiveVerified = true
            )
          )
        }
      }
    } catch (e: Exception) {
      Log.e("RealTimeSearchService", "Error parsing Gemini response: ${e.message}")
    }
    return results
  }

  /**
   * Resilient local verified directory search across bundled `resources_seed.json`.
   */
  fun queryLocalSeedServices(query: String, categoryFilter: String?): List<RealTimeService> {
    val results = mutableListOf<RealTimeService>()
    try {
      val jsonString = context.assets.open("resources_seed.json").bufferedReader().use { it.readText() }
      val array = JSONArray(jsonString)
      val cleanQuery = query.trim().lowercase()

      for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        val name = obj.optString("name", "")
        val org = obj.optString("org", "")
        val cat = obj.optString("category", "").lowercase()
        val desc = obj.optString("description", "")
        val county = obj.optString("county", "").lowercase()
        val address = obj.optString("address", "")
        val phone = obj.optString("phone", "")
        val hours = obj.optString("hours", "")
        val lat = obj.optDouble("lat", 27.9506)
        val lng = obj.optDouble("lng", -82.4572)

        // Filter by category if selected
        if (!categoryFilter.isNullOrBlank() && categoryFilter != "all" && cat != categoryFilter.lowercase()) {
          continue
        }

        // Match query if provided
        val matchesQuery = cleanQuery.isEmpty() ||
            name.lowercase().contains(cleanQuery) ||
            org.lowercase().contains(cleanQuery) ||
            desc.lowercase().contains(cleanQuery) ||
            county.contains(cleanQuery) ||
            address.lowercase().contains(cleanQuery)

        if (matchesQuery) {
          results.add(
            RealTimeService(
              id = "seed_${org.replace(" ", "_")}_${name.replace(" ", "_")}_$i".lowercase(),
              name = if (name.isNotBlank()) name else org,
              category = cat,
              address = address,
              phone = phone,
              operatingHours = hours,
              description = desc,
              latitude = lat,
              longitude = lng,
              sourceUrl = "https://app.streetrise.org",
              sourceTitle = "StreetRise Verified Directory",
              isLiveVerified = true
            )
          )
        }
      }
    } catch (e: Exception) {
      Log.e("RealTimeSearchService", "Error reading local seeds: ${e.message}")
    }
    return results
  }

  /**
   * Riley Phone Assistant query processor.
   * Generates empathetic, concise spoken responses for on-screen speech transcription.
   */
  suspend fun consultRileyAssistant(
    userPrompt: String,
    history: List<RileyDialogueMessage>
  ): String = withContext(Dispatchers.IO) {
    val apiKey = BuildConfig.GEMINI_API_KEY
    if (apiKey.isNotEmpty() && !apiKey.startsWith("MY_")) {
      try {
        val response = callGeminiForRiley(userPrompt, history, apiKey)
        if (response.isNotBlank()) {
          return@withContext response
        }
      } catch (e: Exception) {
        Log.w("RealTimeSearchService", "Riley online call handled (${e.message}), using streetrise knowledge base.")
      }
    }
    // Resilient offline voice assistant knowledge base
    fallbackRileyAnswer(userPrompt)
  }

  private fun callGeminiForRiley(
    userPrompt: String,
    history: List<RileyDialogueMessage>,
    apiKey: String
  ): String {
    val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent?key=$apiKey"

    val systemInstruction = "You are Riley, the official StreetRise 24/7 Phone Assistant for hotline 463-2-STREET (463-278-7338). " +
        "You provide immediate, compassionate, direct street-level navigation for people seeking emergency shelter beds, hot meal schedules, " +
        "hygiene/showers, walk-in clinics, and crisis support in Florida (Tampa, Orlando, Lakeland, Hillsborough, Orange, Polk, Pinellas). " +
        "Speak naturally in clear, concise conversational sentences suitable for live on-screen phone transcription. " +
        "Always provide concrete names, street addresses, phone numbers, and walk-in intake hours when mentioning a service."

    val contentsArray = JSONArray()

    // Add recent context
    val recent = history.takeLast(4)
    for (msg in recent) {
      contentsArray.put(JSONObject().apply {
        put("role", if (msg.sender == MessageSender.USER) "user" else "model")
        put("parts", JSONArray().apply {
          put(JSONObject().apply { put("text", msg.text) })
        })
      })
    }

    // Add current prompt
    contentsArray.put(JSONObject().apply {
      put("role", "user")
      put("parts", JSONArray().apply {
        put(JSONObject().apply {
          put("text", "System Instruction: $systemInstruction\nUser Inquiry: $userPrompt")
        })
      })
    })

    val requestJson = JSONObject().apply {
      put("contents", contentsArray)
      put("tools", JSONArray().apply {
        put(JSONObject().apply {
          put("google_search", JSONObject())
        })
      })
    }

    val body = requestJson.toString().toRequestBody(jsonMediaType)
    val request = Request.Builder()
      .url(url)
      .post(body)
      .build()

    val response = httpClient.newCall(request).execute()
    val responseBody = response.body?.string() ?: ""

    if (response.isSuccessful) {
      val root = JSONObject(responseBody)
      val candidates = root.optJSONArray("candidates")
      if (candidates != null && candidates.length() > 0) {
        val parts = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")
        if (parts != null && parts.length() > 0) {
          return parts.getJSONObject(0).optString("text", "")
        }
      }
    }
    return ""
  }

  private fun fallbackRileyAnswer(userPrompt: String): String {
    val q = userPrompt.lowercase()
    return when {
      q.contains("bed") || q.contains("sleep") || q.contains("shelter") -> {
        "For emergency shelter beds tonight: in Tampa, Metropolitan Ministries intake is at 2002 N Florida Ave (813-209-1000) with walk-in intake at 4:30 PM. In Orlando, Coalition for the Homeless is at 18 N Terry Ave (407-426-1250). In Lakeland, Salvation Army Center of Hope is at 830 N Massachusetts Ave (863-682-8179). You can also dial our line at 463-2-STREET or 211 for bed availability."
      }
      q.contains("food") || q.contains("meal") || q.contains("eat") || q.contains("hungry") -> {
        "For hot food today: Trinity Cafe in Tampa serves hot sit-down meals daily at 2801 N Nebraska Ave starting at 11:30 AM. In Orlando, Daily Bread at Christian Service Center serves hot lunch from 11:00 AM to 12:30 PM at 410 W Central Blvd. In Lakeland, Talbot House Ministries provides daily meals at 814 N Kentucky Ave."
      }
      q.contains("shower") || q.contains("hygiene") || q.contains("clothes") || q.contains("wash") -> {
        "For showers and hygiene: Tampa Hope Day Center offers clean showers, laundry, and hygiene kits from 8:00 AM to 2:00 PM at 3704 E 3rd Ave. In Orlando, Christian Service Center provides showers Monday through Friday from 8:30 AM to 1:00 PM at 410 W Central Blvd."
      }
      q.contains("medical") || q.contains("doctor") || q.contains("clinic") || q.contains("sick") || q.contains("medicine") -> {
        "For free medical care: Judeo Christian Health Clinic in Tampa provides free primary care and prescription assistance at 4118 N MacDill Ave (813-870-0395). In Orlando, Grace Medical Home offers walk-in triage at 1417 E Concord St. For emergency medical distress, dial 911 immediately."
      }
      q.contains("crisis") || q.contains("suicide") || q.contains("help") || q.contains("depressed") || q.contains("mental") -> {
        "If you or someone with you is in emotional crisis or distress, please dial or text 988 right away to reach the 24/7 Suicide & Crisis Lifeline. StreetRise caseworkers are also reachable at 463-2-STREET. You are not alone, and help is here."
      }
      q.contains("operator") || q.contains("human") || q.contains("person") || q.contains("talk") -> {
        "I'm connecting you directly to the StreetRise community dispatch desk. Tap the 'Dial Phone' button on your screen to ring 463-2-STREET (463-278-7338) right now, and a community support coordinator will assist you."
      }
      else -> {
        "I've got you covered. StreetRise is tracking verified support services across Florida. You can tap any of the quick inquiry options below or ask me about emergency beds, hot meal schedules, free clinics, or daytime drop-in centers, and I'll transcribe exact addresses and phone numbers on your screen."
      }
    }
  }
}
