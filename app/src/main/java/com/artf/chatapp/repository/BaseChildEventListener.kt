package com.artf.chatapp.repository

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError

class BaseChildEventListener : ChildEventListener{

    var onChildAdded: ((p0: DataSnapshot, p1: String?) -> Unit)? = null
    var onChildChanged: ((p0: DataSnapshot, p1: String?) -> Unit)? = null

    override fun onCancelled(p0: DatabaseError) {}

    override fun onChildMoved(p0: DataSnapshot, p1: String?) {}

    override fun onChildAdded(p0: DataSnapshot, p1: String?) {
        onChildAdded?.invoke(p0, p1)
    }

    override fun onChildChanged(p0: DataSnapshot, p1: String?) {
        onChildChanged?.invoke(p0, p1)
    }

    override fun onChildRemoved(p0: DataSnapshot) {}

}