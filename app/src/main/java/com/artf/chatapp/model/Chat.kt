package com.artf.chatapp.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.ListenerRegistration

data class Chat(
    var chatId: String? = null,
    var senderId: String? = null,
    var receiverId: String? = null
) {
    var userLr: ListenerRegistration? = null
    var msgLr: ListenerRegistration? = null

    private val _user = MutableLiveData<User>()
    val user: LiveData<User> = _user
    fun setUser(user: User?) {
        _user.value = user
    }

    private val _message = MutableLiveData<Message>()
    val message: LiveData<Message> = _message
    fun setMessage(message: Message?) {
        _message.value = message
    }
}