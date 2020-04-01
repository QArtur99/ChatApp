package com.artf.chatapp.utils.mapper

import com.artf.chatapp.data.model.Message
import com.artf.chatapp.utils.mapper.core.Mapper

object RemoteMessageMapper : Mapper<Map<String, String>, Message> {

    override fun map(input: Map<String, String>): Message {
        return Message(
            id = input["id"],
            senderId = input["senderId"],
            receiverId = input["receiverId"],
            isOwner = input["isOwner"].let { if (it.isNullOrEmpty()) false else it.toBoolean() },
            name = input["name"],
            photoUrl = input["photoUrl"],
            audioUrl = input["audioUrl"],
            audioFile = input["audioFile"],
            audioDuration = input["audioDuration"].let { if (it.isNullOrEmpty()) 0 else it.toLong() },
            text = input["text"],
            timestamp = input["timestamp"],
            readTimestamp = input["readTimestamp"]
        )
    }
}