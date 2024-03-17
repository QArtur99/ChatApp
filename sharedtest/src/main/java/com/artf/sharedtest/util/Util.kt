package com.artf.sharedtest.util

import kotlinx.coroutines.delay

suspend fun unlockThread() {
    delay(10)
}