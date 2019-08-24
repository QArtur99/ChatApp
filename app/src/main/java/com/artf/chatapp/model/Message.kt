package com.artf.chatapp.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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

    private val _audioDownloaded = MutableLiveData<Boolean>()
    @get:Exclude
    val audioDownloaded: LiveData<Boolean> = _audioDownloaded

    fun setAudioDownloaded(audioDownloaded: Boolean) {
        _audioDownloaded.value = audioDownloaded
    }

    fun setMessageId() {
        val timestamp = this.timestamp
        if (timestamp is Timestamp) {
            id = senderId + "_" + timestamp.toDate().time
        }
    }
}