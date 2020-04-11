package com.artf.chatapp.data.model

import com.artf.chatapp.data.repository.MessageLiveData
import com.artf.chatapp.data.repository.ReceiverLiveData
import com.google.firebase.firestore.Exclude

data class Chat(
    var chatId: String? = null,
    var senderId: String? = null,
    var receiverId: String? = null,
    var isGroupChat: Boolean? = null
) {
    @get:Exclude
    var user: ReceiverLiveData? = null

    @get:Exclude
    var message: MessageLiveData? = null
}