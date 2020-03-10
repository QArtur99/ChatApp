package com.artf.chatapp.utils.mapper.core

interface ListMapperNonNullable<I, O> :
    Mapper<List<I>?, List<O>>