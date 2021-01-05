package com.artf.chatapp.di

import android.app.Application
import android.content.Context
import com.artf.chatapp.data.repository.Repository
import com.artf.chatapp.data.repository.RepositoryImpl
import com.artf.chatapp.data.source.firebase.FirebaseDaoImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class AppModule {

    @Provides
    @Singleton
    fun provideApplicationContext(application: Application): Context {
        return application
    }

    @Singleton
    @Provides
    fun provideRepository(
        firebaseDaoImpl: FirebaseDaoImpl,
        ioDispatcher: CoroutineDispatcher
    ): Repository {
        return RepositoryImpl(firebaseDaoImpl, ioDispatcher)
    }

    @Singleton
    @Provides
    fun provideFirebaseDaoImpl(): FirebaseDaoImpl {
        return FirebaseDaoImpl()
    }

    @Singleton
    @Provides
    fun provideIoDispatcher() = Dispatchers.IO
}
