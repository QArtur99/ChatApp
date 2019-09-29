package com.artf.chatapp.di

import com.artf.chatapp.di.viewModel.ViewModelModule
import dagger.Module

@Module(includes = [(ViewModelModule::class)])
object AppModule
