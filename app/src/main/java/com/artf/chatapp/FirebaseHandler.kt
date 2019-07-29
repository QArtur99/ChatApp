package com.artf.chatapp

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.artf.chatapp.model.Message
import com.artf.chatapp.model.User
import com.artf.chatapp.utils.*
import com.firebase.ui.auth.AuthUI
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

class FirebaseHandler(private val activity: AppCompatActivity) {

    companion object {
        const val RC_SIGN_IN = 1
        const val RC_PHOTO_PICKER = 2
        const val ANONYMOUS = "anonymous"
        const val DEFAULT_MSG_LENGTH_LIMIT = 5
        const val MSG_LENGTH_KEY = "friendly_msg_length"
    }

    private val TAG = FirebaseHandler::class.java.simpleName
    private var mUser: User? = null
    private var usernameDialog: UsernameDialog? = null

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

    private val _msgData = MutableLiveData<List<Message>>()
    val msgData: LiveData<List<Message>> = _msgData

    private val _msgLength = MutableLiveData<Int>()
    val msgLength: LiveData<Int> = _msgLength

    private val _usernameStatus = MutableLiveData<NetworkState>()
    val usernameStatus: LiveData<NetworkState> = _usernameStatus

    init {
        _msgData.value = arrayListOf()
        this.mUser = null
        FirebaseApp.initializeApp(activity)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        authStateListener = getAuthStateListener()
        fetchConfig()
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

                val providers =
                    mutableListOf(
                        AuthUI.IdpConfig.EmailBuilder().build(),
                        AuthUI.IdpConfig.FacebookBuilder().build(),
                        AuthUI.IdpConfig.GoogleBuilder().build(),
                        AuthUI.IdpConfig.PhoneBuilder().build()
                    )

                activity.startActivityForResult(
                    AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setIsSmartLockEnabled(false)
                        .setAvailableProviders(providers)
                        .setLogo(R.mipmap.ic_launcher)
                        .build(), RC_SIGN_IN
                )
            }
        }
    }

    private fun fetchConfig() {
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600)
            .build()
        firebaseRemoteConfig.setConfigSettingsAsync(configSettings)

        val defaultConfigMap = HashMap<String, Any>()
        defaultConfigMap[MSG_LENGTH_KEY] = DEFAULT_MSG_LENGTH_LIMIT
        firebaseRemoteConfig.setDefaults(defaultConfigMap)

        firebaseRemoteConfig.fetchAndActivate()
            .addOnSuccessListener {
                firebaseRemoteConfig.activate()
                _msgLength.value = firebaseRemoteConfig.getLong(MSG_LENGTH_KEY).toInt()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error fetching config", e)
                _msgLength.value = firebaseRemoteConfig.getLong(MSG_LENGTH_KEY).toInt()
            }
    }

    private fun attachDatabaseReadListener() {
        if (childEventListener == null) {
            childEventListener = object : ChildEventListener {
                override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                    val friendlyMessage = dataSnapshot.getValue(Message::class.java)
                    _msgData.add(friendlyMessage!!)
                }

                override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
                    val friendlyMessage = dataSnapshot.getValue(Message::class.java)
                    _msgData.remove(_msgData.get(_msgData.count() - 1))
                    _msgData.add(friendlyMessage!!)
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
        _msgData.clear()
        detachDatabaseReadListener()
    }

    fun pushMsg(msg: String, photoUrl: String?) {
        val friendlyMessage = Message(msg, this.mUser!!.userName, photoUrl)
        databaseReference.push().setValue(friendlyMessage)
    }

    fun addUser() {
        usersReference.document(mUser!!.userId!!).set(mUser!!)
            .addOnSuccessListener {
                usernameDialog!!.dismiss()
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
            //usersReference.whereEqualTo("userId", userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) this.mUser = document.toObject(User::class.java)
                if (mUser!!.userName == null) startUsernameDialog()
            }
            .addOnFailureListener {
                startUsernameDialog()
            }
    }

    private fun isUsernameAvailable(username: String) {
        _usernameStatus.value = NetworkState.LOADING
        usernamesReference.document(username.toLowerCase()).get()
            .addOnSuccessListener { document ->
                _usernameStatus.value = if(document.exists()) NetworkState.FAILED else NetworkState.LOADED
            }
            .addOnFailureListener {
                _usernameStatus.value = NetworkState.FAILED
            }
    }

    private fun startUsernameDialog() {
        usernameDialog = UsernameDialog()
        usernameDialog!!.clickListener = { addUsername(it) }
        usernameDialog!!.isUserNameAvailable = { isUsernameAvailable(it) }
        usernameDialog!!.usernameStatus = usernameStatus
        usernameDialog!!.show(activity.supportFragmentManager, UsernameDialog::class.simpleName)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this.activity, "Signed in!", Toast.LENGTH_SHORT).show()
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(activity, "Sign in canceled", Toast.LENGTH_SHORT).show()
                activity.finish()
            }
        } else if (requestCode == RC_PHOTO_PICKER && resultCode == Activity.RESULT_OK) {
            val selectedImageUri = data!!.data
            val photoRef = storageReference.child(selectedImageUri!!.lastPathSegment!!)
            photoRef.putFile(selectedImageUri)
                .addOnSuccessListener(activity) { taskSnapshot ->
                    val urlTask = taskSnapshot.storage.downloadUrl
                    urlTask.addOnSuccessListener { uri ->
                        val friendlyMessage =
                            Message(null, this.mUser!!.userName, uri.toString())
                        databaseReference.push().setValue(friendlyMessage)
                    }
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
        _msgData.clear()
        detachDatabaseReadListener()
    }

}