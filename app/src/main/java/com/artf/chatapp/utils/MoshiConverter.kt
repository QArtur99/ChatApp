package com.artf.chatapp.utils

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

inline fun <reified T> convertToString(item: T): String {
    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    val jsonAdapter = moshi.adapter<T>(T::class.java)
    return jsonAdapter.toJson(item)
}

inline fun <reified T> convertFromString(item: String): T {
    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    val jsonAdapter = moshi.adapter<T>(T::class.java)
    return jsonAdapter.fromJson(item)!!
}