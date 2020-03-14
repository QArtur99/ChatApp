package com.artf.chatapp.di

import com.artf.chatapp.di.viewModel.ViewModelModule
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module(includes = [(ViewModelModule::class)])
object AppModule {

    @JvmStatic
    @Singleton
    @Provides
    fun provideIoDispatcher() = Dispatchers.IO
}


