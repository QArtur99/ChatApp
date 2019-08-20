package com.artf.chatapp.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.artf.chatapp.App
import com.artf.chatapp.model.Message
import com.artf.chatapp.model.User
import com.artf.chatapp.utils.convertToString
import com.bumptech.glide.load.HttpException
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await

class RefreshDataWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    companion object {
        const val WORK_NAME = "RefreshDataWorker"
    }

    private val firebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val dbRefUsers by lazy { firebaseFirestore.collection("users") }

    override suspend fun doWork(): Result {
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
            if (msg.receiverId == App.receiverId) continue
            val senderId = msg.senderId ?: continue

            val documentSnapshot = dbRefUsers.document(senderId).get().await()
            val user = documentSnapshot.toObject(User::class.java) ?: continue

            val notificationId = (msg.timestamp as Timestamp).toDate().time.toString().takeLast(6).toInt()

            makeStatusNotification(
                context = applicationContext,
                notificationId = notificationId,
                userString = convertToString(user),
                senderName = user.username,
                senderPhotoUrl = user.photoUrl,
                message = msg.text
            )
        }
    }
}
