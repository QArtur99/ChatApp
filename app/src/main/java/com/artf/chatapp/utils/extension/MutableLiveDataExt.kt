package com.artf.chatapp.utils.extension

import androidx.lifecycle.MutableLiveData

fun <T> MutableLiveData<List<T>>.add(item: T) {
    val updatedItems = this.value as MutableList
    updatedItems.add(item)
    this.value = updatedItems
}

fun <T> MutableLiveData<List<T>>.remove(item: T) {
    val updatedItems = this.value as MutableList
    updatedItems.remove(item)
    this.value = updatedItems
}

fun <T> MutableLiveData<List<T>>.clear() {
    this.value ?: return
    val updatedItems = this.value as MutableList
    updatedItems.clear()
    this.value = updatedItems
}

fun <T> MutableLiveData<List<T>>.get(index: Int): T {
    val updatedItems = this.value as MutableList
    return updatedItems[index]
}

fun <T> MutableLiveData<List<T>>.count(): Int {
    val updatedItems = this.value as MutableList
    return updatedItems.size
}