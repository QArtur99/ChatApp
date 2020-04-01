package com.artf.chatapp.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class User(
    var userId: String? = null,
    var username: String? = null,
    @field:JvmField var isOnline: Boolean? = null,
    var lastSeenTimestamp: Any? = null,
    var photoUrl: String? = null,
    var usernameList: List<String>? = null,
    var fcmTokenList: MutableList<String>? = null
) {
    companion object {
        fun nameToArray(username: String): List<String> {
            val usernameList = arrayListOf<String>()
            for (x in 1..username.length) usernameList.add(username.take(x))
            return usernameList
        }
    }
}