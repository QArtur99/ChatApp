package com.artf.chatapp

import android.app.Application
import com.artf.chatapp.di.AppComponent
import com.artf.chatapp.di.AppModule
import com.artf.chatapp.di.DaggerAppComponent
import com.artf.chatapp.di.FirebaseRepositoryModule
import com.google.firebase.database.FirebaseDatabase


class App : Application() {

    companion object {
        lateinit var app: App
        val component: AppComponent by lazy {
            DaggerAppComponent.builder()
                .appModule(AppModule(app))
                .firebaseRepositoryModule(FirebaseRepositoryModule())
                .build()
        }
    }

    override fun onCreate() {
        super.onCreate()
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        App.app = this
        component.inject(this)
    }
}