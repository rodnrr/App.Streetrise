package com.example

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

object PushNotificationManager {
    private val _fcmToken = MutableStateFlow<String?>(null)
    val fcmToken = _fcmToken.asStateFlow()

    private val _incomingNotification = MutableSharedFlow<Map<String, String>>()
    val incomingNotification = _incomingNotification.asSharedFlow()

    fun updateToken(token: String) {
        _fcmToken.value = token
    }

    suspend fun receiveNotification(title: String, body: String, data: Map<String, String>) {
        val payload = data.toMutableMap()
        payload["title"] = title
        payload["body"] = body
        _incomingNotification.emit(payload)
    }
}
