package com.artf.chatapp.model

import com.google.firebase.Timestamp

/**
 * Set messageId for recycler view adapter
 * Firebase timestamp is created after init method.
 */

data class Message(
    var id: String? = null,
    var authorId: String? = null,
    var isOwner: Boolean? = null,
    var name: String? = null,
    var photoUrl: String? = null,
    var audioUrl: String? = null,
    var audioFile: String? = null,
    var audioDuration: Long? = null,
    var audioDownloaded: Boolean? = null,
    var text: String? = null,
    var timestamp: Any? = null
) {
    fun setMessageId() {
        val timestamp = this.timestamp
        if (timestamp is Timestamp) {
            id = authorId + "_" + timestamp.toDate().time
        }
    }
}