package com.artf.chatapp

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase

class FirebaseHandler(context: Context) {

    init{
        FirebaseApp.initializeApp(context)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    }
}