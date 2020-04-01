package com.artf.chatapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

/**
 * Set messageId for recycler view adapter
 * Firebase timestamp is created after init method.
 */

data class Message(
    var id: String? = null,
    var senderId: String? = null,
    var receiverId: String? = null,
    var isOwner: Boolean? = null,
    var name: String? = null,
    var photoUrl: String? = null,
    var audioUrl: String? = null,
    var audioFile: String? = null,
    var audioDuration: Long? = null,
    var text: String? = null,
    var timestamp: Any? = null,
    var readTimestamp: Any? = null
) {

    @get:Exclude
    var audioDownloaded = false

    fun setMessageId() {
        val timestamp = this.timestamp
        if (timestamp is Timestamp) {
            id = senderId + "_" + timestamp.toDate().time
        }
    }
}