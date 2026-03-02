package com.pharos.app.network.model

import com.google.gson.annotations.SerializedName

data class AnalysisResponse(
    @SerializedName("topics")
    val topics: List<String> = emptyList(),

    @SerializedName("project_suggestions")
    val projectSuggestions: List<String> = emptyList(),

    @SerializedName("summary")
    val summary: String = "",

    @SerializedName("action_items")
    val actionItems: List<String> = emptyList(),

    @SerializedName("confidence")
    val confidence: Double = 0.0
)
