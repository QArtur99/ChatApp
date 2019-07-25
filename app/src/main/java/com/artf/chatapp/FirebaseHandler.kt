package com.artf.chatapp

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.firebase.ui.auth.AuthUI
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.storage.FirebaseStorage

class FirebaseHandler(private val activity: Activity) {

    companion object {
        const val RC_SIGN_IN = 1
        const val RC_PHOTO_PICKER = 2
        const val ANONYMOUS = "anonymous"
        const val DEFAULT_MSG_LENGTH_LIMIT = 5
        const val MSG_LENGTH_KEY = "friendly_msg_length"
    }

    private val TAG = FirebaseHandler::class.java.simpleName
    private var mUsername: String? = null

    private val firebaseDatabase by lazy { FirebaseDatabase.getInstance() }
    private val firebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firebaseStorage by lazy { FirebaseStorage.getInstance() }
    val firebaseRemoteConfig by lazy { FirebaseRemoteConfig.getInstance() }

    val usersReference by lazy { firebaseDatabase.reference.child("users") }
    val databaseReference by lazy { firebaseDatabase.reference.child("messages") }
    val storageReference by lazy { firebaseStorage.reference.child("chat_photos") }

    private var childEventListener: ChildEventListener? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    private val _msgData = MutableLiveData<List<FriendlyMessage>>()
    val msgData: LiveData<List<FriendlyMessage>> = _msgData

    private val _msgLength = MutableLiveData<Int>()
    val msgLength: LiveData<Int> = _msgLength

    init {
        _msgData.value = arrayListOf()
        mUsername = ANONYMOUS
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
                    val friendlyMessage = dataSnapshot.getValue(FriendlyMessage::class.java)
                    _msgData.add(friendlyMessage!!)
                }

                override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
                    val friendlyMessage = dataSnapshot.getValue(FriendlyMessage::class.java)
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
        mUsername = username
        attachDatabaseReadListener()
    }

    private fun onSignedOutCleanup() {
        mUsername = ANONYMOUS
        _msgData.clear()
        detachDatabaseReadListener()
    }

    fun pushMsg(msg: String, photoUrl: String?) {
        val friendlyMessage = FriendlyMessage(msg, mUsername!!, photoUrl)
        databaseReference.push().setValue(friendlyMessage)
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
                        val friendlyMessage = FriendlyMessage(null, mUsername!!, uri.toString())
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