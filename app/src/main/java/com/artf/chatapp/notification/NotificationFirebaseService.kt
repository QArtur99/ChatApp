package com.artf.chatapp.notification

import com.artf.chatapp.App
import com.artf.chatapp.data.model.Message
import com.artf.chatapp.data.model.User
import com.artf.chatapp.notification.data.NewMessageNotification
import com.artf.chatapp.utils.convertToString
import com.artf.chatapp.utils.mapper.RemoteMessageMapper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NotificationFirebaseService : FirebaseMessagingService() {

    private val firebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val dbRefUsers by lazy { firebaseFirestore.collection("users") }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        remoteMessage.data.isNotEmpty().let {
            val msg = RemoteMessageMapper.map(remoteMessage.data)
            if (msg.senderId == App.receiverId) return
            GlobalScope.launch { createNotifications(msg) }
        }
    }

    override fun onNewToken(newToken: String) {
        super.onNewToken(newToken)
        FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener {
            val refreshedToken = it.result?.token ?: return@addOnCompleteListener
        }
    }

    private suspend fun createNotifications(message: Message) {
        val senderId = message.senderId ?: return

        val documentSnapshot = dbRefUsers.document(senderId).get().await()
        val user = documentSnapshot.toObject(User::class.java) ?: return

        val newMessageNotification = NewMessageNotification(
            notificationId = senderId.hashCode(),
            userString = convertToString(user),
            senderName = user.username,
            senderPhotoUrl = user.photoUrl,
            message = getNotificationText(message)
        )
        NotificationUtils.makeStatusNotification(applicationContext, newMessageNotification)
    }

    private fun getNotificationText(message: Message): String {
        var text = ""
        message.text?.let { if (it.isNotEmpty()) text = it }
        message.audioUrl?.let { if (it.isNotEmpty()) text = "\uD83C\uDFA4 Record" }
        message.photoUrl?.let { if (it.isNotEmpty()) text = "\uD83D\uDCF7 Photo" }
        return text
    }
}