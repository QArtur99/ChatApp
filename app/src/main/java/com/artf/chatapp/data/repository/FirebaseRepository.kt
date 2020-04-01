package com.artf.chatapp.data.repository

import android.net.Uri
import com.artf.chatapp.App
import com.artf.chatapp.data.model.Chat
import com.artf.chatapp.data.model.Message
import com.artf.chatapp.data.model.User
import com.artf.chatapp.utils.states.FragmentState
import com.artf.chatapp.utils.states.NetworkState
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject

class FirebaseRepository @Inject constructor(private val ioDispatcher: CoroutineDispatcher) {

    companion object {
        const val RC_SIGN_IN = 1
        const val RC_PHOTO_PICKER = 2
        const val ANONYMOUS = "anonymous"
        const val DEFAULT_MSG_LENGTH_LIMIT = 1000
        const val MSG_LENGTH_KEY = "friendly_msg_length"
        val TAG = FirebaseRepository::class.java.simpleName
    }

    private var mUser: User? = null

    private val firebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val firebaseRemoteConfig by lazy { FirebaseRemoteConfig.getInstance() }
    private val firebaseStorage by lazy { FirebaseStorage.getInstance() }

    private val dbRefChatRooms by lazy { firebaseFirestore.collection("chatRooms") }
    private val dbRefUsernames by lazy { firebaseFirestore.collection("usernames") }
    private val dbRefUsers by lazy { firebaseFirestore.collection("users") }
    private val sRefPhotos by lazy { firebaseStorage.reference.child("chat_photos") }
    private val sRefRecords by lazy { firebaseStorage.reference.child("chat_records") }

    private lateinit var chatRoomId: String

    private var chatRoomListner: ListenerRegistration? = null
    private var userChatRoomsListner: ListenerRegistration? = null

    private var isNewSenderChatRoom: Boolean? = null
    private var isNewReceiverChatRoom: Boolean? = null
    private var receiverId: String? = null

    var onChatRoomListSort: (() -> Unit)? = null
    var onFragmentStateChanged: ((state: FragmentState) -> Unit)? = null
    var onChatRoomList: ((chatRoomList: List<Chat>) -> Unit)? = null
    var onMsgList: ((msgList: List<Message>) -> Unit)? = null
    var onChildAdded: ((message: Message) -> Unit)? = null
    var onChildChanged: ((message: Message) -> Unit)? = null

    fun startListening() {
        fetchConfig()
    }

    suspend fun onSignedIn(userId: String) {
        mUser = getUser(userId)
        updateUser(userId, true)
        attachUserChatRoomsListener()
        receiverId?.let { setChatRoomId(it) }
    }

    suspend fun onSignedOut() {
        mUser?.userId?.let { updateUser(it, false) }
        detachDatabaseListeners()
        this.mUser = null
        this.receiverId = null
    }

    private fun fetchConfig() {
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600)
            .build()
        firebaseRemoteConfig.setConfigSettingsAsync(configSettings)

        val defaultConfigMap = HashMap<String, Any>()
        defaultConfigMap[MSG_LENGTH_KEY] = DEFAULT_MSG_LENGTH_LIMIT
        firebaseRemoteConfig.setDefaultsAsync(defaultConfigMap)
    }

    fun fetchConfigMsgLength(callBack: (msgLengh: Int) -> Unit) {
        firebaseRemoteConfig.fetchAndActivate()
            .addOnSuccessListener {
                callBack(firebaseRemoteConfig.getLong(MSG_LENGTH_KEY).toInt())
            }
            .addOnFailureListener { e ->
                callBack(firebaseRemoteConfig.getLong(MSG_LENGTH_KEY).toInt())
            }
    }

    private fun attachUserChatRoomsListener() {
        if (userChatRoomsListner == null) {
            userChatRoomsListner = dbRefUsers.document(mUser?.userId!!).collection("chatRooms")
                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    firebaseFirestoreException?.let { return@addSnapshotListener }
                    val chatRoomList = querySnapshot?.toObjects(Chat::class.java)
                    chatRoomList?.let {
                        onChatRoomList?.invoke(loadChatRooms(chatRoomList))
                    }
                }
        }
    }

    private fun loadChatRooms(chatRoomList: List<Chat>): List<Chat> {
        for (chat in chatRoomList) {
            val isReceiver = chat.receiverId != mUser?.userId
            val receiverId = if (isReceiver) chat.receiverId else chat.senderId
            receiverId?.let { chat.userLr = getReceiver(chat, receiverId) }
            chat.chatId?.let { chat.msgLr = setSingleMsgListener(chat) }
        }
        return chatRoomList
    }

    private fun getReceiver(chat: Chat, receiverId: String): ListenerRegistration {
        return dbRefUsers.document(receiverId)
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                firebaseFirestoreException?.let { return@addSnapshotListener }
                val user = querySnapshot?.toObject(User::class.java)
                chat.setUser(user)
            }
    }

    private fun setSingleMsgListener(chat: Chat): ListenerRegistration {
        return dbRefChatRooms.document(chat.chatId!!).collection("chatRoom")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                firebaseFirestoreException?.let { return@addSnapshotListener }
                val msgList = querySnapshot?.toObjects(Message::class.java)
                msgList?.let {
                    for (lastMsg in msgList) {
                        chat.setMessage(lastMsg)
                        onChatRoomListSort?.invoke()
                    }
                }
            }
    }

    private fun attachChatRoomListener() {
        if (chatRoomListner == null) {
            // TODO editable messages
            chatRoomListner = dbRefChatRooms.document(chatRoomId).collection("chatRoom")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                // .limit(1)
                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    querySnapshot ?: return@addSnapshotListener
                    firebaseFirestoreException?.let { return@addSnapshotListener }
                    // querySnapshot?.let { if (it.metadata.isFromCache) return@addSnapshotListener }

                    val msgList = mutableListOf<Message>()

                    for (documentSnapshot in querySnapshot.documents) {
                        val msg = documentSnapshot.toObject(Message::class.java) ?: continue
                        msg.setMessageId()
                        msg.isOwner = msg.senderId!! == mUser?.userId.toString()
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
                    msgList.let { onMsgList?.invoke(msgList) }
                }
        }
    }

    private fun detachDatabaseListeners() {
        detachChatRoomListener()
        userChatRoomsListner?.remove()
        userChatRoomsListner = null
    }

    private fun detachChatRoomListener() {
        chatRoomListner?.remove()
        chatRoomListner = null
    }

    fun pushMsg(
        msg: String? = null,
        photoUrl: String? = null,
        audioUrl: String? = null,
        audioFile: String? = null,
        audioDuration: Long? = null,
        callBack: ((usernameStatus: NetworkState) -> Unit)? = null
    ) {
        val friendlyMessage = Message(
            senderId = mUser?.userId,
            receiverId = receiverId,
            name = mUser?.username,
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

    private fun addUser(callBack: (usernameStatus: NetworkState) -> Unit) {
        dbRefUsers.document(mUser?.userId!!).set(mUser!!)
            .addOnSuccessListener {
                callBack(NetworkState.LOADED)
            }
            .addOnFailureListener {
                callBack(NetworkState.FAILED)
            }
    }

    fun addUsername(username: String, callBack: (usernameStatus: NetworkState) -> Unit) {
        callBack(NetworkState.LOADING)
        val usernameLowerCase = username.toLowerCase()
        mUser?.username = usernameLowerCase
        mUser?.usernameList = User.nameToArray(usernameLowerCase)
        mUser?.fcmTokenList = arrayListOf()
        dbRefUsernames.document(usernameLowerCase).set(mUser!!)
            .addOnSuccessListener {
                addUser { callBack(it) }
            }
            .addOnFailureListener {
                callBack(NetworkState.FAILED)
            }
    }

    private suspend fun getUser(userId: String): User? {
        var user = User(userId)
        try {
            val documentSnapshot = dbRefUsers.document(userId).get().await()
            user = documentSnapshot.toObject(User::class.java)!!
        } catch (e: FirebaseFirestoreException) {
            onFragmentStateChanged?.invoke(FragmentState.USERNAME)
        }
        return user
    }

    private suspend fun updateUser(userId: String, isOnline: Boolean) {
        val map = mutableMapOf<String, Any>()
        updateFcmToken(map, isOnline)
        map["isOnline"] = isOnline
        map["lastSeenTimestamp"] = FieldValue.serverTimestamp()
        dbRefUsers.document(userId).update(map)
    }

    private suspend fun updateFcmToken(map: MutableMap<String, Any>, isOnline: Boolean) {
        FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener {
            val refreshedToken = it.result?.token ?: return@addOnCompleteListener
            val fcmTokenList = mUser?.fcmTokenList ?: arrayListOf()
            if (isOnline) fcmTokenList.add(refreshedToken) else fcmTokenList.remove(refreshedToken)
            fcmTokenList.distinct()
            map["fcmToken"] = fcmTokenList
        }.await()
    }

    private fun addSenderChatRoom() {
        val chatSender = Chat(chatRoomId, mUser?.userId, receiverId)
        dbRefUsers.document(mUser?.userId!!).collection("chatRooms").document(chatRoomId)
            .set(chatSender)
            .addOnSuccessListener {
                isNewSenderChatRoom = false
            }
            .addOnFailureListener {
            }
    }

    private fun addReceiverChatRoom() {
        val chatReceiver = Chat(chatRoomId, receiverId, mUser?.userId)
        dbRefUsers.document(receiverId!!).collection("chatRooms").document(chatRoomId)
            .set(chatReceiver)
            .addOnSuccessListener {
                isNewReceiverChatRoom = false
            }
            .addOnFailureListener {
            }
    }

    private fun getUserChatRoom() {
        dbRefUsers.document(mUser?.userId!!).collection("chatRooms").document(chatRoomId).get()
            .addOnSuccessListener { documentSnapshot ->
                isNewSenderChatRoom = !documentSnapshot.exists()
            }
            .addOnFailureListener {
            }

        dbRefUsers.document(receiverId!!).collection("chatRooms").document(chatRoomId).get()
            .addOnSuccessListener { documentSnapshot ->
                isNewReceiverChatRoom = !documentSnapshot.exists()
            }
            .addOnFailureListener {
            }
    }

    suspend fun searchForUser(
        username: String,
        callBack: (networkState: NetworkState, userList: MutableList<User>) -> Unit
    ) {
        try {
            callBack(NetworkState.LOADING, mutableListOf())
            val querySnapshot =
                dbRefUsers.whereArrayContains("usernameList", username).get().await()
            val list = querySnapshot.toObjects(User::class.java)
            val networkState = if (list.isNotEmpty()) NetworkState.LOADED else NetworkState.FAILED
            callBack(networkState, list)
        } catch (e: FirebaseFirestoreException) {
            callBack(NetworkState.FAILED, mutableListOf())
        }
    }

    suspend fun isUsernameAvailable(
        username: String,
        callBack: (networkState: NetworkState) -> Unit
    ) {
        try {
            callBack(NetworkState.LOADING)
            val document = dbRefUsernames.document(username.toLowerCase()).get().await()
            callBack(if (document.exists()) NetworkState.FAILED else NetworkState.LOADED)
        } catch (e: FirebaseFirestoreException) {
            callBack(NetworkState.FAILED)
        }
    }

    fun setChatRoomId(receiverId: String) {
        this.receiverId = receiverId
        val userId = mUser?.userId ?: return
        App.receiverId = receiverId
        chatRoomId = if (receiverId > userId) "${receiverId}_$userId" else "${userId}_$receiverId"
        detachChatRoomListener()
        attachChatRoomListener()
        isNewSenderChatRoom = null
        getUserChatRoom()
    }

    fun stopListening() {
        detachDatabaseListeners()
    }
}