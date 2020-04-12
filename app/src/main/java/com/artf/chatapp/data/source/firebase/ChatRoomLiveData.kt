package com.artf.chatapp.data.source.firebase

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import com.artf.chatapp.App
import com.artf.chatapp.data.model.Chat
import com.artf.chatapp.data.model.Message
import com.artf.chatapp.data.model.User
import com.artf.chatapp.utils.states.NetworkState
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import kotlin.properties.Delegates

class ChatRoomLiveData : MutableLiveData<List<Message>>() {

    private val firebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val firebaseStorage by lazy { FirebaseStorage.getInstance() }

    private val dbRefChatRooms by lazy { firebaseFirestore.collection("chatRooms") }
    private val dbRefUsers by lazy { firebaseFirestore.collection("users") }
    private val sRefPhotos by lazy { firebaseStorage.reference.child("chat_photos") }
    private val sRefRecords by lazy { firebaseStorage.reference.child("chat_records") }

    private var chatRoomLr: ListenerRegistration? = null

    var user: User? = null
    private var chatRoomId: String? = null
    private var isNewSenderChatRoom: Boolean? = null
    private var isNewReceiverChatRoom: Boolean? = null
    var receiverId by Delegates.observable<String?>(null) { _, _, newValue ->
        if (newValue != null) {
            App.receiverId = newValue
            setChatRoomId(newValue)
            onInactive()
            onActive()
        }
    }

    private val snapshotListener = object : EventListener<QuerySnapshot> {
        override fun onEvent(
            querySnapshot: QuerySnapshot?,
            firebaseFirestoreException: FirebaseFirestoreException?
        ) {
            querySnapshot ?: return
            firebaseFirestoreException?.let { return@onEvent }
            // querySnapshot?.let { if (it.metadata.isFromCache) return@addSnapshotListener }

            val msgList = mutableListOf<Message>()

            for (documentSnapshot in querySnapshot.documents) {
                val msg = documentSnapshot.toObject(Message::class.java) ?: continue
                msg.setMessageId()
                msg.isOwner = msg.senderId!! == user?.userId.toString()
                msgList.add(msg)
                if (documentSnapshot.metadata.isFromCache) continue
                if (documentSnapshot.metadata.hasPendingWrites()) continue
                if (msg.readTimestamp != null) continue
                if (msg.isOwner!!.not()) {
                    val map = mutableMapOf<String, Any>()
                    map["readTimestamp"] = FieldValue.serverTimestamp()
                    documentSnapshot.reference.update(map)
                }
            }
            value = msgList
        }
    }

    private fun getChatRoomRef(chatRoomId: String): Query {
        // TODO editable messages
        return dbRefChatRooms.document(chatRoomId).collection("chatRoom")
            .orderBy("timestamp", Query.Direction.ASCENDING)
    }

    private fun setChatRoomId(receiverId: String) {
        val userId = user?.userId ?: return
        val roomId = if (receiverId > userId) "${receiverId}_$userId" else "${userId}_$receiverId"
        chatRoomId = roomId
        isNewSenderChatRoom = null
        getUserChatRoom(userId, receiverId, roomId)
    }

    private fun getUserChatRoom(userId: String, receiverId: String, chatRoomId: String) {
        dbRefUsers.document(userId).collection("chatRooms").document(chatRoomId).get()
            .addOnSuccessListener { documentSnapshot ->
                isNewSenderChatRoom = !documentSnapshot.exists()
            }
            .addOnFailureListener {
            }

        dbRefUsers.document(receiverId).collection("chatRooms").document(chatRoomId).get()
            .addOnSuccessListener { documentSnapshot ->
                isNewReceiverChatRoom = !documentSnapshot.exists()
            }
            .addOnFailureListener {
            }
    }

    private fun addSenderChatRoom() {
        val chatRoomId = chatRoomId ?: return
        val chatSender = Chat(chatRoomId, user?.userId, receiverId)
        dbRefUsers.document(user?.userId!!).collection("chatRooms").document(chatRoomId)
            .set(chatSender)
            .addOnSuccessListener {
                isNewSenderChatRoom = false
            }
            .addOnFailureListener {
            }
    }

    private fun addReceiverChatRoom() {
        val chatRoomId = chatRoomId ?: return
        val chatReceiver = Chat(chatRoomId, receiverId, user?.userId)
        dbRefUsers.document(receiverId!!).collection("chatRooms").document(chatRoomId)
            .set(chatReceiver)
            .addOnSuccessListener {
                isNewReceiverChatRoom = false
            }
            .addOnFailureListener {
            }
    }

    fun pushMsg(
        msg: String? = null,
        photoUrl: String? = null,
        audioUrl: String? = null,
        audioFile: String? = null,
        audioDuration: Long? = null,
        callBack: ((usernameStatus: NetworkState) -> Unit)? = null
    ) {
        val chatRoomId = chatRoomId ?: run {
            callBack?.invoke(NetworkState.FAILED)
            return@pushMsg
        }
        val friendlyMessage = Message(
            senderId = user?.userId,
            receiverId = receiverId,
            name = user?.username,
            photoUrl = photoUrl,
            audioUrl = audioUrl,
            audioFile = audioFile,
            audioDuration = audioDuration,
            text = msg,
            timestamp = FieldValue.serverTimestamp()
        )
        dbRefChatRooms.document(chatRoomId).collection("chatRoom").document().set(friendlyMessage)
            .addOnSuccessListener {
                isNewSenderChatRoom?.let { if (it) addSenderChatRoom() }
                isNewReceiverChatRoom?.let { if (it) addReceiverChatRoom() }
                callBack?.invoke(NetworkState.LOADED)
            }
            .addOnFailureListener {
                callBack?.invoke(NetworkState.FAILED)
            }
    }

    fun pushAudio(
        audioPath: String,
        audioDuration: Long,
        callBack: (usernameStatus: NetworkState) -> Unit
    ) {
        callBack(NetworkState.LOADING)
        val selectedImageUri = Uri.fromFile(File(audioPath))
        sRefRecords.child(selectedImageUri.lastPathSegment!!).putFile(selectedImageUri)
            .addOnSuccessListener { taskSnapshot ->
                val urlTask = taskSnapshot.storage.downloadUrl
                urlTask.addOnSuccessListener { uri ->
                    pushMsg(
                        audioUrl = uri.toString(),
                        audioFile = audioPath,
                        audioDuration = audioDuration
                    ) { callBack(it) }
                }.addOnFailureListener {
                    callBack(NetworkState.FAILED)
                }
            }.addOnFailureListener { callBack(NetworkState.FAILED) }
    }

    fun pushPicture(pictureUri: Uri, callBack: (usernameStatus: NetworkState) -> Unit) {
        callBack(NetworkState.LOADING)
        sRefPhotos.child(pictureUri.lastPathSegment!!).putFile(pictureUri)
            .addOnSuccessListener { taskSnapshot ->
                val urlTask = taskSnapshot.storage.downloadUrl
                urlTask.addOnSuccessListener { uri ->
                    pushMsg(photoUrl = uri.toString()) { callBack(it) }
                }.addOnFailureListener {
                    callBack(NetworkState.FAILED)
                }
            }.addOnFailureListener { callBack(NetworkState.FAILED) }
    }

    override fun onActive() {
        chatRoomLr = chatRoomId?.let { getChatRoomRef(it).addSnapshotListener(snapshotListener) }
    }

    public override fun onInactive() {
        chatRoomLr?.remove()
        chatRoomLr = null
    }
}