package com.artf.chatapp.di

import android.app.Application
import com.artf.chatapp.repository.FirebaseBaseRepository
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class, FirebaseLoginModule::class])
interface AppComponent {
    fun inject(app: Application)
    fun inject(firebaseBaseRepository: FirebaseBaseRepository)
}