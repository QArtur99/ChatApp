package com.artf.chatapp.utils

import com.google.common.reflect.TypeToken
import com.google.gson.Gson

inline fun <reified T> convertToStringGson(gameData: T): String {
    val type = object : TypeToken<T>() {}.type
    return Gson().toJson(gameData, type)
}

inline fun <reified T> convertFromStringGson(item: String): T {
    val type = object : TypeToken<T>() {}.type
    return Gson().fromJson<T>(item, type)
}