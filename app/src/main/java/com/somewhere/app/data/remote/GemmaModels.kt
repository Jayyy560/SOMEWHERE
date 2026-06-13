package com.somewhere.app.data.remote

data class SimpleDropDto(
    val id: String,
    val text: String
)

data class PlaceSummaryRequest(
    val drops: List<SimpleDropDto>
)

data class PlaceSummaryResponse(
    val summary: String
)

data class CuratorRequest(
    val drops: List<SimpleDropDto>
)

data class CuratorResponse(
    val short_intro: String,
    val selected_drop_text: String
)
