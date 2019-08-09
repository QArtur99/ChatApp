package com.artf.chatapp.di

import android.app.Application
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class, FirebaseRepositoryModule::class])
interface AppComponent {
    fun inject(app: Application)
}