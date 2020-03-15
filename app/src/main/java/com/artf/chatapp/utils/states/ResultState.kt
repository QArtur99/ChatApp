package com.artf.chatapp.utils.states

sealed class ResultState<out R>(private val tag: String) {

    private companion object {
        val successTag = Success::class.java.simpleName
        val errorTag = Error::class.java.simpleName
        val loadingTag = Loading::class.java.simpleName
    }

    data class Success<out T>(val data: T) : ResultState<T>(successTag)
    data class Error(val exception: Exception) : ResultState<Nothing>(errorTag)
    object Loading : ResultState<Nothing>(loadingTag)

    val succeeded: Boolean
        get() = this is Success<*> && data != null

    override fun toString(): String {
        return when (this) {
            is Success<*> -> "Success[data=$data]"
            is Error -> "Error[exception=$exception]"
            is Loading -> "Loading"
        }
    }
}
