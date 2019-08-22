package com.artf.chatapp.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.artf.chatapp.App
import com.artf.chatapp.model.Message
import com.artf.chatapp.model.User
import com.artf.chatapp.utils.Utility
import com.artf.chatapp.utils.convertToString
import com.bumptech.glide.load.HttpException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await

class NotificationWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    companion object {
        const val WORK_NAME = "NotificationWorker"
    }

    private val firebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val dbRefUsers by lazy { firebaseFirestore.collection("users") }

    override suspend fun doWork(): Result {
        Log.e(WORK_NAME, WORK_NAME)
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return Result.success()
        return try {
            val querySnapshot = setSingleMsgListener(currentUser.uid)
            if (querySnapshot == null || querySnapshot.metadata.isFromCache) return Result.success()
            createNotifications(querySnapshot)
            querySnapshot.documents.map { it.reference.delete() }
            Result.success()
        } catch (e: HttpException) {
            Result.retry()
        }
    }

    private suspend fun setSingleMsgListener(uid: String): QuerySnapshot? {
        return dbRefUsers.document(uid)
            .collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get().await()
    }

    private suspend fun createNotifications(querySnapshot: QuerySnapshot) {
        for (queryDocumentSnapshot in querySnapshot) {
            if (queryDocumentSnapshot.metadata.isFromCache) continue
            val msg = queryDocumentSnapshot.toObject(Message::class.java)
            if (msg.senderId == App.receiverId) continue
            val senderId = msg.senderId ?: continue

            val documentSnapshot = dbRefUsers.document(senderId).get().await()
            val user = documentSnapshot.toObject(User::class.java) ?: continue

            val notificationId = Utility.getTimeStamp().toString().takeLast(6).toInt()

            makeStatusNotification(
                context = applicationContext,
                notificationId = notificationId,
                userString = convertToString(user),
                senderName = user.username,
                senderPhotoUrl = user.photoUrl,
                message = getNotificationText(msg)
            )
        }
    }

    private fun getNotificationText(message: Message): String {
        var text = ""
        message.audioUrl?.let { text = "\uD83C\uDFA4 Record" }
        message.photoUrl?.let { text = "\uD83D\uDCF7 Photo" }
        message.text?.let { text = it }
        return text
    }

}
