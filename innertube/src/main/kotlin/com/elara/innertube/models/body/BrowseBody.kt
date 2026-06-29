package com.elara.innertube.models.body

import com.elara.innertube.models.Context
import com.elara.innertube.models.Continuation
import kotlinx.serialization.Serializable

@Serializable
data class BrowseBody(
    val context: Context,
    val browseId: String?,
    val params: String?,
    val continuation: String?
)
