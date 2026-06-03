package com.kun.github.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DeleteFileRequest(
    val message: String,
    val sha: String
)
