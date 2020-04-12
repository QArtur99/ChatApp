package com.artf.chatapp.data.source.firebase

import androidx.lifecycle.LiveData
import com.artf.chatapp.data.model.User
import com.artf.chatapp.utils.states.AuthenticationState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserLiveData : LiveData<Pair<User?, AuthenticationState>>() {

    private var uiScope: CoroutineScope? = null

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val dbRefUsers by lazy { firebaseFirestore.collection("users") }

    var mUser: User? = null

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        firebaseAuth.currentUser?.uid?.let { userId ->
            uiScope?.launch {
                val user = getUser(userId) ?: User().apply { this.userId = userId }
                value = Pair(user, AuthenticationState.Authenticated(userId))
                updateUser(userId, true)
            }
        } ?: run {
            value = Pair(null, AuthenticationState.Unauthenticated)
        }
    }

    private suspend fun getUser(userId: String): User? {
        val user: User?
        try {
            val documentSnapshot = dbRefUsers.document(userId).get().await()
            user = documentSnapshot.toObject(User::class.java)
        } catch (e: FirebaseFirestoreException) {
            return null
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

    fun setNewUser(user: User) {
        user.userId?.let { userId ->
            value = Pair(user, AuthenticationState.Authenticated(userId))
            uiScope?.launch {
                updateUser(userId, true)
            }
        }
    }

    override fun onActive() {
        uiScope = CoroutineScope(Job() + Dispatchers.Main)
        firebaseAuth.addAuthStateListener(authStateListener)
    }

    override fun onInactive() {
        firebaseAuth.removeAuthStateListener(authStateListener)
        uiScope?.launch {
            mUser?.userId?.let { updateUser(it, false) }
            uiScope?.cancel()
        }
    }
}