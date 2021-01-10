package com.artf.chatapp.utils

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

inline fun <reified T> convertToString(item: T): String {
    val jsonAdapter = moshi.adapter(T::class.java)
    return jsonAdapter.toJson(item)
}

inline fun <reified T> convertFromString(item: String): T {
    val jsonAdapter = moshi.adapter(T::class.java)
    return jsonAdapter.fromJson(item)!!
}