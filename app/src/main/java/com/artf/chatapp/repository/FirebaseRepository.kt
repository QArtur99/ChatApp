package com.artf.chatapp.repository


import android.content.Intent
import com.artf.chatapp.model.Chat
import com.artf.chatapp.model.Message
import com.artf.chatapp.model.User
import com.artf.chatapp.utils.FragmentState
import com.artf.chatapp.utils.NetworkState
import com.artf.chatapp.utils.Utility
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class FirebaseRepository {

    companion object {
        const val RC_SIGN_IN = 1
        const val RC_PHOTO_PICKER = 2
        const val ANONYMOUS = "anonymous"
        const val DEFAULT_MSG_LENGTH_LIMIT = 1000
        const val MSG_LENGTH_KEY = "friendly_msg_length"
        val TAG = FirebaseRepository::class.java.simpleName
    }

    private var mUser: User? = null

    private val firebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val firebaseRemoteConfig by lazy { FirebaseRemoteConfig.getInstance() }
    private val firebaseStorage by lazy { FirebaseStorage.getInstance() }

    private val chatRoomsReference by lazy { firebaseFirestore.collection("chatRooms") }
    private val storageReference by lazy { firebaseStorage.reference.child("chat_photos") }
    private val usernamesReference by lazy { firebaseFirestore.collection("usernames") }
    private val usersReference by lazy { firebaseFirestore.collection("users") }

    private lateinit var chatRoomId: String

    private var authStateListener: FirebaseAuth.AuthStateListener? = null
    private var chatRoomListner: ListenerRegistration? = null
    private var userChatRoomsListner: ListenerRegistration? = null

    private var isNewSenderChatRoom: Boolean? = null
    private var isNewReceiverChatRoom: Boolean? = null
    private var receiverId: String? = null

    var onChatRoomListSort: (() -> Unit)? = null
    var onSignOut: (() -> Unit)? = null
    var onFragmentStateChanged: ((state: FragmentState) -> Unit)? = null
    var onChatRoomList: ((chatRoomList: List<Chat>) -> Unit)? = null
    var onMsgList: ((msgList: List<Message>) -> Unit)? = null
    var onChildAdded: ((message: Message) -> Unit)? = null
    var onChildChanged: ((message: Message) -> Unit)? = null


    fun startListening() {
        authStateListener = getAuthStateListener()
        firebaseAuth.addAuthStateListener(authStateListener!!)
        fetchConfig()
    }

    private fun getAuthStateListener(): FirebaseAuth.AuthStateListener {
        return FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) onSignedIn(user.uid) else onSignedOut()
        }
    }

    private fun onSignedIn(userId: String) {
        mUser = User(userId)
        getUser(mUser?.userId!!)
        attachUserChatRoomsListener()
    }

    private fun onSignedOut() {
        this.mUser = null
        detachDatabaseListeners()
        onSignOut?.invoke()
    }

    private fun fetchConfig() {
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600)
            .build()
        firebaseRemoteConfig.setConfigSettingsAsync(configSettings)

        val defaultConfigMap = HashMap<String, Any>()
        defaultConfigMap[MSG_LENGTH_KEY] =
            DEFAULT_MSG_LENGTH_LIMIT
        firebaseRemoteConfig.setDefaults(defaultConfigMap)
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
            userChatRoomsListner = usersReference.document(mUser?.userId!!).collection("chatRooms")
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
            val receiverId = (if (chat.receiverId != mUser?.userId) chat.receiverId else chat.senderId)
            receiverId?.let { chat.userLr = getReceiver(chat, receiverId) }
            chat.chatId?.let { chat.msgLr = setSingleMsgListener(chat) }
        }
        return chatRoomList
    }

    private fun getReceiver(chat: Chat, receiverId: String): ListenerRegistration {
        return usersReference.document(receiverId)
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                firebaseFirestoreException?.let { return@addSnapshotListener }
                val user = querySnapshot?.toObject(User::class.java)
                chat.setUser(user)
            }
    }

    private fun setSingleMsgListener(chat: Chat): ListenerRegistration {
        return chatRoomsReference.document(chat.chatId!!).collection("chatRoom")
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
            //TODO editable messages
            chatRoomListner = chatRoomsReference.document(chatRoomId).collection("chatRoom")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                //.limit(1)
                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    firebaseFirestoreException?.let { return@addSnapshotListener }
                    //querySnapshot?.let { if (it.metadata.isFromCache) return@addSnapshotListener }

                    val msgList = querySnapshot?.toObjects(Message::class.java)
                    msgList?.let {
                        for (msg in msgList) {
                            msg!!.isOwner = msg.authorId!! == mUser?.userId.toString()
                        }
                    }
                    msgList?.let { onMsgList?.invoke(msgList) }
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

    fun pushMsg(msg: String?, photoUrl: String?) {
        val friendlyMessage = Message(
            authorId = mUser?.userId,
            name = mUser?.username,
            photoUrl = photoUrl,
            text = msg,
            timestamp = Utility.getTimeStamp()
        )
        chatRoomsReference.document(chatRoomId).collection("chatRoom").document().set(friendlyMessage)
            .addOnSuccessListener {
                isNewSenderChatRoom?.let { if (it) addSenderChatRoom() }
                isNewReceiverChatRoom?.let { if (it) addReceiverChatRoom() }
            }
            .addOnFailureListener {

            }
    }

    private fun addUser(callBack: (usernameStatus: NetworkState) -> Unit) {
        usersReference.document(mUser?.userId!!).set(mUser!!)
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
        usernamesReference.document(usernameLowerCase).set(mUser!!)
            .addOnSuccessListener {
                addUser { callBack(it) }
            }
            .addOnFailureListener {
                callBack(NetworkState.FAILED)
            }
    }

    private fun getUser(userId: String) {
        usersReference.document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    this.mUser = document.toObject(User::class.java)
                    onFragmentStateChanged?.invoke(FragmentState.START)
                } else onFragmentStateChanged?.invoke(FragmentState.USERNAME)
            }
            .addOnFailureListener {
                onFragmentStateChanged?.invoke(FragmentState.USERNAME)
            }
    }


    private fun addSenderChatRoom() {
        val chatSender = Chat(chatRoomId, mUser?.userId, receiverId)
        usersReference.document(mUser?.userId!!).collection("chatRooms").document(chatRoomId).set(chatSender)
            .addOnSuccessListener {
                isNewSenderChatRoom = false
            }
            .addOnFailureListener {

            }
    }

    private fun addReceiverChatRoom() {
        val chatReceiver = Chat(chatRoomId, receiverId, mUser?.userId)
        usersReference.document(receiverId!!).collection("chatRooms").document(chatRoomId).set(chatReceiver)
            .addOnSuccessListener {
                isNewReceiverChatRoom = false
            }
            .addOnFailureListener {

            }
    }

    private fun getUserChatRoom() {
        usersReference.document(mUser?.userId!!).collection("chatRooms").document(chatRoomId).get()
            .addOnSuccessListener { documentSnapshot ->
                isNewSenderChatRoom = !documentSnapshot.exists()
            }
            .addOnFailureListener {

            }

        usersReference.document(receiverId!!).collection("chatRooms").document(chatRoomId).get()
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
            val querySnapshot =  usersReference.whereArrayContains("usernameList", username).get().await()
            val list = querySnapshot.toObjects(User::class.java)
            val networkState = if (list.isNotEmpty()) NetworkState.LOADED else NetworkState.FAILED
            callBack(networkState, list)
        } catch (e: FirebaseFirestoreException) {
            callBack(NetworkState.FAILED, mutableListOf())
        }
    }

    suspend fun isUsernameAvailable(username: String, callBack: (networkState: NetworkState) -> Unit) {
        try {
            callBack(NetworkState.LOADING)
            val document = usernamesReference.document(username.toLowerCase()).get().await()
            callBack(if (document.exists()) NetworkState.FAILED else NetworkState.LOADED)
        } catch (e: FirebaseFirestoreException) {
            callBack(NetworkState.FAILED)
        }
    }

    fun putPicture(data: Intent?) {
        val selectedImageUri = data!!.data
        storageReference.child(selectedImageUri!!.lastPathSegment!!).putFile(selectedImageUri)
            .addOnSuccessListener { taskSnapshot ->
                val urlTask = taskSnapshot.storage.downloadUrl
                urlTask.addOnSuccessListener { uri ->
                    pushMsg(null, uri.toString())
                }
            }
    }

    fun setChatRoomId(receiverId: String) {
        val userId = mUser?.userId!!
        this.receiverId = receiverId
        chatRoomId = if (receiverId > userId) "${receiverId}_$userId" else "${userId}_$receiverId"
        detachChatRoomListener()
        attachChatRoomListener()
        isNewSenderChatRoom = null
        getUserChatRoom()
    }

    fun stopListening() {
        if (authStateListener != null) firebaseAuth.removeAuthStateListener(authStateListener!!)
        detachDatabaseListeners()
    }

}