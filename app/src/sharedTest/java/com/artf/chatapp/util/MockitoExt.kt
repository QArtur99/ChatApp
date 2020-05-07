package com.artf.chatapp.util

import org.mockito.ArgumentCaptor
import org.mockito.Mockito

inline fun <reified T> any(): T = Mockito.any()

inline fun <reified T> mock(): T = Mockito.mock(T::class.java)

inline fun <reified T> argumentCaptor(): ArgumentCaptor<T> = ArgumentCaptor.forClass(T::class.java)