package com.example.moviereviewapp.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TMDBService {
    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "en-US"
    ): MovieResponse

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: String,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
        @Query("append_to_response") appendToResponse: String = "credits"
    ): MovieDetailsResponse
}

data class MovieResponse(
    val page: Int,
    val results: List<MovieDTO>,
    val total_pages: Int,
    val total_results: Int
)

data class MovieDTO(
    val id: Int,
    val title: String,
    val overview: String,
    val poster_path: String,
    val release_date: String,
    val vote_average: Double
)

data class MovieDetailsResponse(
    val id: Int,
    val title: String,
    val overview: String,
    val poster_path: String,
    val release_date: String,
    val vote_average: Double,
    val runtime: Int,
    val genres: List<Genre>,
    val credits: Credits
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
    val profile_path: String?
)

data class CrewMember(
    val id: Int,
    val name: String,
    val job: String,
    val profile_path: String?
) 