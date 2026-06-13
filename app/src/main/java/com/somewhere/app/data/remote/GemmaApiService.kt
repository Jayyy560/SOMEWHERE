package com.somewhere.app.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

interface GemmaApiService {
    @POST("/place-summary")
    suspend fun getPlaceSummary(@Body request: PlaceSummaryRequest): PlaceSummaryResponse

    @POST("/curator")
    suspend fun getCuratorDrop(@Body request: CuratorRequest): CuratorResponse
}
