package com.artf.chatapp.utils.mapper.core

class ListMapperNonNullableImpl<I, O>(private val mapper: Mapper<I, O>) :
    ListMapperNonNullable<I, O> {
    override fun map(input: List<I>?): List<O> {
        return input?.map { mapper.map(it) }.orEmpty()
    }
}