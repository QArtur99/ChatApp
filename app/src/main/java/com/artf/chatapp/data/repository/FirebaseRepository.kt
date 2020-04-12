package com.artf.chatapp.data.repository

import com.artf.chatapp.data.model.User
import com.artf.chatapp.utils.states.NetworkState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject

class FirebaseRepository @Inject constructor(private val ioDispatcher: CoroutineDispatcher) {

    companion object {
        const val RC_SIGN_IN = 1
        const val RC_PHOTO_PICKER = 2
        const val ANONYMOUS = "anonymous"
        const val DEFAULT_MSG_LENGTH_LIMIT = 1000
        const val MSG_LENGTH_KEY = "friendly_msg_length"
    }

    private val firebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val firebaseRemoteConfig by lazy { FirebaseRemoteConfig.getInstance() }

    private val dbRefUsernames by lazy { firebaseFirestore.collection("usernames") }
    private val dbRefUsers by lazy { firebaseFirestore.collection("users") }

    init {
        fetchConfig()
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

    private fun addUser(user: User, callBack: (usernameStatus: NetworkState) -> Unit) {
        dbRefUsers.document(user.userId!!).set(user)
            .addOnSuccessListener {
                callBack(NetworkState.LOADED)
            }
            .addOnFailureListener {
                callBack(NetworkState.FAILED)
            }
    }

    fun addUsername(
        user: User,
        username: String,
        callBack: (usernameStatus: NetworkState) -> Unit
    ) {
        callBack(NetworkState.LOADING)
        val usernameLowerCase = username.toLowerCase(Locale.ROOT)
        user.username = usernameLowerCase
        user.usernameList = User.nameToArray(usernameLowerCase)
        user.fcmTokenList = arrayListOf()
        dbRefUsernames.document(usernameLowerCase).set(user)
            .addOnSuccessListener {
                addUser(user) { callBack(it) }
            }
            .addOnFailureListener {
                callBack(NetworkState.FAILED)
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
            val document = dbRefUsernames.document(username.toLowerCase(Locale.ROOT)).get().await()
            callBack(if (document.exists()) NetworkState.FAILED else NetworkState.LOADED)
        } catch (e: FirebaseFirestoreException) {
            callBack(NetworkState.FAILED)
        }
    }
}