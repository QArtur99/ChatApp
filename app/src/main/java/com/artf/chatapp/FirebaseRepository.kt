package com.artf.chatapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.artf.chatapp.model.Message
import com.artf.chatapp.model.User
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
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

    private var childEventListener: ChildEventListener? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null
    private var firebaseCallback: FirebaseCallback? = null

    init {
        this.mUser = null
        FirebaseApp.initializeApp(activity)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        authStateListener = getAuthStateListener()
    }

    private fun getAuthStateListener(): FirebaseAuth.AuthStateListener {
        return FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                val userName = if (user.displayName.isNullOrEmpty()) user.phoneNumber else user.displayName
                onSignedInInitialize(user.uid, userName)
            } else {
                // User is signed out
                onSignedOutCleanup()
                firebaseCallback?.startSignInActivity()
            }
        }
    }

    fun fetchConfig(callBack: (msgLengh: Int) -> Unit) {
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600)
            .build()
        firebaseRemoteConfig.setConfigSettingsAsync(configSettings)

        val defaultConfigMap = HashMap<String, Any>()
        defaultConfigMap[MSG_LENGTH_KEY] = DEFAULT_MSG_LENGTH_LIMIT
        firebaseRemoteConfig.setDefaults(defaultConfigMap)

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
            childEventListener = object : ChildEventListener {
                override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                    val friendlyMessage = dataSnapshot.getValue(Message::class.java)
                    firebaseCallback?.onChildAdded(friendlyMessage!!)
                }

                override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
                    val friendlyMessage = dataSnapshot.getValue(Message::class.java)
                    firebaseCallback?.onChildChanged(friendlyMessage!!)
                }

                override fun onChildRemoved(dataSnapshot: DataSnapshot) {}

                override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}

                override fun onCancelled(databaseError: DatabaseError) {}
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

    private fun onSignedInInitialize(userId: String, username: String?) {
        mUser = User(userId)
        getUser(userId)
        attachDatabaseReadListener()
    }

    private fun onSignedOutCleanup() {
        this.mUser = null
        firebaseCallback?.onSignedOut()
        detachDatabaseReadListener()
    }

    fun pushMsg(msg: String, photoUrl: String?) {
        val friendlyMessage = Message(msg, this.mUser!!.userName, photoUrl)
        databaseReference.push().setValue(friendlyMessage)
    }

    fun addUser() {
        usersReference.document(mUser!!.userId!!).set(mUser!!)
            .addOnSuccessListener {
                firebaseCallback?.startMainFragment()

            }
            .addOnFailureListener {

            }
    }

    fun addUsername(username: String) {
        mUser?.let { user ->
            user.userName = username.toLowerCase()
            usernamesReference.document(user.userName!!).set(user)
                .addOnSuccessListener {
                    addUser()
                }
                .addOnFailureListener {

                }
        }
    }

    private fun getUser(userId: String) {
        usersReference.document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) this.mUser = document.toObject(User::class.java)
            }
            .addOnFailureListener {
                firebaseCallback?.startUsernameFragment()
            }
    }

    fun isUsernameAvailable(username: String, callBack: (usernameStatus: NetworkState) -> Unit) {
        usernamesReference.document(username.toLowerCase()).get()
            .addOnSuccessListener { document ->
                callBack(if (document.exists()) NetworkState.FAILED else NetworkState.LOADED)
            }
            .addOnFailureListener {
                callBack(NetworkState.FAILED)
            }
    }

    fun putPicture(data: Intent?){
        val selectedImageUri = data!!.data
        storageReference.child(selectedImageUri!!.lastPathSegment!!).putFile(selectedImageUri)
            .addOnSuccessListener { taskSnapshot ->
                val urlTask = taskSnapshot.storage.downloadUrl
                urlTask.addOnSuccessListener { uri ->
                    val friendlyMessage = Message(null, this.mUser!!.userName, uri.toString())
                    databaseReference.push().setValue(friendlyMessage)
                }
            }
    }

    fun onResume() {
        firebaseAuth.addAuthStateListener(authStateListener!!)
    }

    fun onPause() {
        if (authStateListener != null) {
            firebaseAuth.removeAuthStateListener(authStateListener!!)
        }
        firebaseCallback?.onSignedOut()
        detachDatabaseReadListener()
    }

    fun addFirebaseCallback(firebaseCallback: FirebaseCallback){
        this.firebaseCallback = firebaseCallback
    }

    interface FirebaseCallback {
        fun startSignInActivity()

        fun onSignedOut()

        fun onChildAdded(message:Message)

        fun onChildChanged(message:Message)

        fun startUsernameFragment()

        fun startMainFragment()
    }
}