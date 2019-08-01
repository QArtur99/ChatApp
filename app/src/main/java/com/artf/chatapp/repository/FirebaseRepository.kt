package com.artf.chatapp.repository

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.artf.chatapp.model.Message
import com.artf.chatapp.model.User
import com.artf.chatapp.utils.NetworkState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.storage.FirebaseStorage

class FirebaseRepository(private val activity: AppCompatActivity) {

    companion object {
        const val RC_SIGN_IN = 1
        const val RC_PHOTO_PICKER = 2
        const val ANONYMOUS = "anonymous"
        const val DEFAULT_MSG_LENGTH_LIMIT = 5
        const val MSG_LENGTH_KEY = "friendly_msg_length"
    }

    private val TAG = FirebaseRepository::class.java.simpleName
    private var mUser: User? = null

    private val firebaseDatabase by lazy { FirebaseDatabase.getInstance() }
    private val firebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val firebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firebaseStorage by lazy { FirebaseStorage.getInstance() }
    private val firebaseRemoteConfig by lazy { FirebaseRemoteConfig.getInstance() }

    val usersReference by lazy { firebaseFirestore.collection("users") }
    val usernamesReference by lazy { firebaseFirestore.collection("usernames") }
    val databaseReference by lazy { firebaseDatabase.reference.child("messages") }
    val storageReference by lazy { firebaseStorage.reference.child("chat_photos") }

    private var childEventListener: BaseChildEventListener? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    var startSignInActivity: (() -> Unit)? = null
    var signOut: (() -> Unit)? = null
    var startUsernameFragment: (() -> Unit)? = null
    var startMainFragment: (() -> Unit)? = null
    var onChildAdded: ((message: Message) -> Unit)? = null
    var onChildChanged: ((message: Message) -> Unit)? = null

    init {
        authStateListener = getAuthStateListener()
        firebaseAuth.addAuthStateListener(authStateListener!!)
        fetchConfig()
    }

    private fun getAuthStateListener(): FirebaseAuth.AuthStateListener {
        return FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                onSignedInInitialize(user.uid)
            } else {
                onSignedOutCleanup()
                startSignInActivity?.invoke()
            }
        }
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
        if (childEventListener == null) {
            childEventListener = BaseChildEventListener()
            childEventListener?.onChildAdded = { dataSnapshot, s ->
                val friendlyMessage = dataSnapshot.getValue(Message::class.java)
                onChildAdded?.invoke(friendlyMessage!!)
            }
            childEventListener?.onChildChanged = { dataSnapshot, s ->
                val friendlyMessage = dataSnapshot.getValue(Message::class.java)
                onChildChanged?.invoke(friendlyMessage!!)
            }
            databaseReference.addChildEventListener(childEventListener!!)
        }
    }

    private fun detachDatabaseReadListener() {
        if (childEventListener != null) {
            databaseReference.removeEventListener(childEventListener!!)
            childEventListener = null
        }
    }

    private fun onSignedInInitialize(userId: String) {
        mUser = User(userId)
        getUser(userId)
        attachDatabaseReadListener()
    }

    private fun onSignedOutCleanup() {
        this.mUser = null
        signOut?.invoke()
        detachDatabaseReadListener()
    }

    fun pushMsg(msg: String?, photoUrl: String?) {
        val friendlyMessage = Message(getMsgId(), msg, this.mUser!!.userName, photoUrl)
        databaseReference.push().setValue(friendlyMessage)
    }

    fun addUser(callBack: (usernameStatus: NetworkState) -> Unit) {
        usersReference.document(mUser!!.userId!!).set(mUser!!)
            .addOnSuccessListener {
                callBack(NetworkState.LOADED)
                startMainFragment?.invoke()

            }
            .addOnFailureListener {
                callBack(NetworkState.FAILED)
            }
    }

    fun addUsername(username: String, callBack: (usernameStatus: NetworkState) -> Unit) {
        callBack(NetworkState.LOADING)
        mUser!!.userName = username.toLowerCase()
        usernamesReference.document(mUser!!.userName!!).set(mUser!!)
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
                    startMainFragment?.invoke()
                } else startUsernameFragment?.invoke()
            }
            .addOnFailureListener {
                startUsernameFragment?.invoke()
            }
    }

    fun isUsernameAvailable(username: String, callBack: (usernameStatus: NetworkState) -> Unit) {
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

    fun removeListener() {
        if (authStateListener != null) firebaseAuth.removeAuthStateListener(authStateListener!!)
        signOut?.invoke()
        detachDatabaseReadListener()
    }

    fun getMsgId(): String {
        return this.mUser!!.userId + "_" + System.currentTimeMillis()
    }
}