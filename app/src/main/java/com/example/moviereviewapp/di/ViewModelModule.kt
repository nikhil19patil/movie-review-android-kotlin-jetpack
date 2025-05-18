package com.example.moviereviewapp.di

import android.content.Context
import com.example.moviereviewapp.data.MovieRepository
import com.example.moviereviewapp.data.MovieRepositoryImpl
import com.example.moviereviewapp.data.api.TMDBService
import com.example.moviereviewapp.data.local.MovieDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ViewModelModule {
    
    @Provides
    @Singleton
    fun provideMovieRepository(
        tmdbService: TMDBService,
        movieDao: MovieDao,
        apiKey: String,
        context: Context
    ): MovieRepository {
        return MovieRepositoryImpl(tmdbService, movieDao, apiKey, context)
    }
} 