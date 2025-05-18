package com.example.moviereviewapp.data

data class Movie(
    val id: String,
    val title: String,
    val overview: String,
    val posterPath: String,
    val releaseDate: String,
    val voteAverage: Double,
    val runtime: Int? = null,
    val genres: List<Genre> = emptyList(),
    val credits: Credits? = null,
    val reviews: List<Review> = emptyList()
)

data class Genre(
    val id: Int,
    val name: String
)

data class Credits(
    val cast: List<CastMember>,
    val crew: List<CrewMember>
)

data class CastMember(
    val id: Int,
    val name: String,
    val character: String,
    val profilePath: String?
)

data class CrewMember(
    val id: Int,
    val name: String,
    val job: String,
    val profilePath: String?
)

data class Review(
    val id: String,
    val author: String,
    val content: String,
    val rating: Int,
    val date: String
) 