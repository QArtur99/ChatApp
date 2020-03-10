package com.artf.chatapp.utils.mapper.core

interface Mapper<I, O> {
    fun map(input: I): O
}