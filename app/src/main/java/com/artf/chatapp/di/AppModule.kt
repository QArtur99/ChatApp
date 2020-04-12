package com.artf.chatapp.di

import com.artf.chatapp.data.repository.Repository
import com.artf.chatapp.data.repository.RepositoryImpl
import com.artf.chatapp.data.source.firebase.FirebaseDaoImpl
import com.artf.chatapp.di.viewModel.ViewModelModule
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module(includes = [(ViewModelModule::class)])
object AppModule {

    @JvmStatic
    @Singleton
    @Provides
    fun provideRepository(
        firebaseDaoImpl: FirebaseDaoImpl,
        ioDispatcher: CoroutineDispatcher
    ): Repository {
        return RepositoryImpl(firebaseDaoImpl, ioDispatcher)
    }

    @JvmStatic
    @Singleton
    @Provides
    fun provideFirebaseDaoImpl(): FirebaseDaoImpl {
        return FirebaseDaoImpl()
    }

    @JvmStatic
    @Singleton
    @Provides
    fun provideIoDispatcher() = Dispatchers.IO
}


