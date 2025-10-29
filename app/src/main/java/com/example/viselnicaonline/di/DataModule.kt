package com.example.viselnicaonline.di

import android.content.Context
import com.example.viselnicaonline.data.GameRepositoryImpl
import com.example.viselnicaonline.data.WordsProvider
import com.example.viselnicaonline.domain.GameRepository
import com.google.firebase.database.FirebaseDatabase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {

    @Binds
    fun bindGameRepository(impl: GameRepositoryImpl): GameRepository


    companion object {

        @Provides
        @ApplicationScope
        fun provideFirebaseDatabase(): FirebaseDatabase = FirebaseDatabase.getInstance()

        @Provides
        @ApplicationScope
        fun provideWordsProvider(context: Context): WordsProvider = WordsProvider(context)

    }
}