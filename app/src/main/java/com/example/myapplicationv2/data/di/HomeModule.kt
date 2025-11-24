package com.example.myapplicationv2.data.di

import android.content.Context
import com.example.myapplicationv2.data.local.prefs.HomePrefs
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HomeModule {
    @Provides @Singleton
    fun provideHomePrefs(@ApplicationContext ctx: Context): HomePrefs = HomePrefs(ctx)
}
