package com.artf.chatapp.model

data class Chat(
    var chatId: String? = null,
    var senderId: String? = null,
    var receiverId: String? = null,
    var user: User? = null,
    var msg: Message? = null
)