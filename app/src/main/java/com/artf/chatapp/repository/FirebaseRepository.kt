package com.artf.chatapp.repository


import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.artf.chatapp.model.Chat
import com.artf.chatapp.model.Message
import com.artf.chatapp.model.User
import com.artf.chatapp.utils.FragmentState
import com.artf.chatapp.utils.NetworkState
import com.artf.chatapp.utils.Utility
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.storage.FirebaseStorage

class FirebaseRepository(val activity: AppCompatActivity) : FirebaseBaseRepository(activity) {

    companion object {
        const val RC_SIGN_IN = 1
        const val RC_PHOTO_PICKER = 2
        const val ANONYMOUS = "anonymous"
        const val DEFAULT_MSG_LENGTH_LIMIT = 1000
        const val MSG_LENGTH_KEY = "friendly_msg_length"
    }

    private val TAG = FirebaseRepository::class.java.simpleName

    private val firebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val firebaseStorage by lazy { FirebaseStorage.getInstance() }
    private val firebaseRemoteConfig by lazy { FirebaseRemoteConfig.getInstance() }

    val chatRoomsReference by lazy { firebaseFirestore.collection("chatRooms") }
    val usersReference by lazy { firebaseFirestore.collection("users") }
    val usernamesReference by lazy { firebaseFirestore.collection("usernames") }
    val storageReference by lazy { firebaseStorage.reference.child("chat_photos") }

    private lateinit var chatRoomId: String
    private var chatRoomListner: ListenerRegistration? = null

    var onSignOut: (() -> Unit)? = null
    var onFragmentStateChanged: ((state: FragmentState) -> Unit)? = null
    var onMsgList: ((message: List<Message>) -> Unit)? = null
    var onChildAdded: ((message: Message) -> Unit)? = null
    var onChildChanged: ((message: Message) -> Unit)? = null

    init {
        init()
        fetchConfig()
    }

    override fun onSignedIn() {
        getUser(firebaseLogin.mUser?.userId!!)
        //attachDatabaseReadListener()
    }

    override fun onSignedOut() {
        detachDatabaseReadListener()
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

    private fun attachDatabaseReadListener() {
        if (chatRoomListner == null) {
            chatRoomListner = chatRoomsReference.document(chatRoomId).collection("chatRoom")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    firebaseFirestoreException?.let { return@addSnapshotListener }
                    querySnapshot?.let { if (it.metadata.isFromCache) return@addSnapshotListener }

                    val msgList = querySnapshot?.toObjects(Message::class.java)
                    msgList?.let {
                        for (friendlyMessage in msgList) {
                            friendlyMessage!!.isOwner = friendlyMessage.authorId!! == getUser().userId.toString()
                            onChildAdded?.invoke(friendlyMessage)
                        }
                    }
                }
        }

    }

    private fun detachDatabaseReadListener() {
        chatRoomListner?.remove()
        chatRoomListner = null
    }

    fun pushMsg(msg: String?, photoUrl: String?) {
        val friendlyMessage = Message(
            authorId = getUser().userId,
            name = getUser().userName,
            photoUrl = photoUrl,
            text = msg,
            timestamp = Utility.getTimeStamp()
        )
        chatRoomsReference.document(chatRoomId).collection("chatRoom").document().set(friendlyMessage)
            .addOnFailureListener {

            }
    }

    private fun addUser(callBack: (usernameStatus: NetworkState) -> Unit) {
        usersReference.document(getUser().userId!!).set(getUser())
            .addOnSuccessListener {
                callBack(NetworkState.LOADED)
            }
            .addOnFailureListener {
                callBack(NetworkState.FAILED)
            }
    }

    fun addUsername(username: String, callBack: (usernameStatus: NetworkState) -> Unit) {
        callBack(NetworkState.LOADING)
        getUser().userName = username.toLowerCase()
        usernamesReference.document(getUser().userName!!).set(getUser())
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
                    this.firebaseLogin.mUser = document.toObject(User::class.java)
                    onFragmentStateChanged?.invoke(FragmentState.MAIN)
                } else onFragmentStateChanged?.invoke(FragmentState.USERNAME)
            }
            .addOnFailureListener {
                onFragmentStateChanged?.invoke(FragmentState.USERNAME)
            }
    }


    private fun addUserChats(userId: String) {
        val chat = Chat("555", "666", "777")
        firebaseFirestore.collection("userChats").document(userId).set(chat)
            .addOnSuccessListener {

            }
            .addOnFailureListener {

            }
    }

    private fun getUserChats(userId: String) {
        firebaseFirestore.collection("userChats/$userId").get()
            .addOnSuccessListener {

            }
            .addOnFailureListener {

            }
    }

    fun searchForUser(
        username: String,
        callBack: (networkState: NetworkState, userList: MutableList<User>) -> Unit
    ) {
        callBack(NetworkState.LOADING, mutableListOf())
        usersReference.whereEqualTo("userName", username).get()
            .addOnSuccessListener { querySnapshot ->
                val list = querySnapshot.toObjects(User::class.java)
                callBack(NetworkState.LOADED, list)
            }
            .addOnFailureListener {
                callBack(NetworkState.FAILED, mutableListOf())
            }
    }

    fun isUsernameAvailable(username: String, callBack: (networkState: NetworkState) -> Unit) {
        callBack(NetworkState.LOADING)
        usernamesReference.document(username.toLowerCase()).get()
            .addOnSuccessListener { document ->
                callBack(if (document.exists()) NetworkState.FAILED else NetworkState.LOADED)
            }
            .addOnFailureListener {
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
        val userId = getUser().userId!!
        chatRoomId = if (receiverId > userId) "$receiverId\\_$userId" else "$userId\\_$receiverId"
        detachDatabaseReadListener()
        attachDatabaseReadListener()
    }

    fun removeListener() {
        removeAuthListener()
        detachDatabaseReadListener()
    }

}