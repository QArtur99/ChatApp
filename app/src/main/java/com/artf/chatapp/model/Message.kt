package com.artf.chatapp.model

data class Message(
    var id: String? = null,
    var authorId: String? = null,
    var isOwner: Boolean? = null,
    var name: String? = null,
    var photoUrl: String? = null,
    var audioUrl: String? = null,
    var audioFile: String? = null,
    var text: String? = null,
    var timestamp: Long? = null
) {
    init {
        id = authorId + "_" + timestamp
    }
}