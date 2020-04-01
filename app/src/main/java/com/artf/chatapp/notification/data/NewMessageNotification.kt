package com.artf.chatapp.notification.data

data class NewMessageNotification(
    val notificationId: Int,
    val userString: String,
    val senderName: String?,
    val senderPhotoUrl: String?,
    val message: String?
)