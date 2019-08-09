package com.artf.chatapp.di

import com.artf.chatapp.repository.FirebaseRepository
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class FirebaseRepositoryModule {

    @Provides
    @Singleton
    fun provideFirebaseRepositoryModule() : FirebaseRepository {
        return FirebaseRepository()
    }
}