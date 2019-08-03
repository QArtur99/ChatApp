package com.artf.chatapp.model

class Message(
    var id: String? = null,
    var text: String? = null,
    var name: String? = null,
    var photoUrl: String? = null,
    var isOwner: Boolean? = null
)