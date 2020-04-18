package com.artf.chatapp.data.source.firebase

import androidx.lifecycle.LiveData
import com.artf.chatapp.data.model.Message
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot

class MessageLiveData(private val query: Query) : LiveData<Message?>() {

    private var msgLr: ListenerRegistration? = null

    private val snapshotListener = object : EventListener<QuerySnapshot> {
        override fun onEvent(
            querySnapshot: QuerySnapshot?,
            firebaseFirestoreException: FirebaseFirestoreException?
        ) {
            firebaseFirestoreException?.let { return@onEvent }
            val msgList = querySnapshot?.toObjects(Message::class.java)
            msgList ?: return
            for (lastMsg in msgList) {
                value = lastMsg
            }
        }
    }

    override fun onActive() {
        msgLr = query.addSnapshotListener(snapshotListener)
    }

    override fun onInactive() {
        msgLr?.remove()
        msgLr = null
    }
}