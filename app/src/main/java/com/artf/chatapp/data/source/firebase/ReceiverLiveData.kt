package com.artf.chatapp.data.source.firebase

import androidx.lifecycle.LiveData
import com.artf.chatapp.data.model.User
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration

class ReceiverLiveData(private val docRef: DocumentReference) : LiveData<User?>() {

    private var userLr: ListenerRegistration? = null

    private val snapshotListener = object : EventListener<DocumentSnapshot> {
        override fun onEvent(
            querySnapshot: DocumentSnapshot?,
            firebaseFirestoreException: FirebaseFirestoreException?
        ) {
            firebaseFirestoreException?.let { return@onEvent }
            val user = querySnapshot?.toObject(User::class.java)
            value = user
        }
    }

    override fun onActive() {
        userLr = docRef.addSnapshotListener(snapshotListener)
    }

    public override fun onInactive() {
        userLr?.remove()
        userLr = null
    }
}