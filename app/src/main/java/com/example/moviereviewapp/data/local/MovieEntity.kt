package com.example.moviereviewapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.moviereviewapp.data.Movie

@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val overview: String,
    val posterPath: String,
    val releaseDate: String,
    val voteAverage: Double,
    val page: Int
) 