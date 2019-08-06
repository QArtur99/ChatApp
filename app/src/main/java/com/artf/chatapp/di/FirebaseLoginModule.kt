package com.artf.chatapp.di

import com.artf.chatapp.repository.FirebaseLogin
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class FirebaseLoginModule {

    @Provides
    @Singleton
    fun provideFirebaseLoginModule() : FirebaseLogin {
        return FirebaseLogin()
    }
}