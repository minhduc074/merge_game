package com.bepngon.merge.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface BepNgonApi {
    @GET("api/v1/config/current")
    suspend fun currentConfig(@Query("sinceVersion") sinceVersion: Int? = null): GameConfigResponse

    @POST("api/v1/players/bootstrap")
    suspend fun bootstrap(@Body body: BootstrapRequest): BootstrapResponse

    @PATCH("api/v1/players/me/nickname")
    suspend fun updateNickname(
        @Header("Authorization") authorization: String,
        @Body body: NicknameRequest,
    ): NicknameResponse

    @GET("api/v1/leaderboards/{board}")
    suspend fun leaderboard(
        @Path("board") board: String,
        @Query("limit") limit: Int = 100,
    ): LeaderboardResponse

    @GET("api/v1/leaderboards/{board}/around-me")
    suspend fun aroundMe(
        @Path("board") board: String,
        @Header("Authorization") authorization: String,
        @Query("range") range: Int = 10,
    ): LeaderboardResponse

    @POST("api/v1/leaderboards/{board}/submit")
    suspend fun submitScore(
        @Path("board") board: String,
        @Header("Authorization") authorization: String,
        @Body body: SubmitScoreRequest,
    ): SubmitScoreResponse
}
