package com.artf.chatapp.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

inline fun <reified T> convertToStringGson(gameData: T): String {
    val type = object : TypeToken<T>() {}.type
    return Gson().toJson(gameData, type)
}

inline fun <reified T> convertFromStringGson(item: String): T {
    val type = object : TypeToken<T>() {}.type
    return Gson().fromJson<T>(item, type)
}