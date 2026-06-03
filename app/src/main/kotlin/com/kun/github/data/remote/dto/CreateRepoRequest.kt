package com.kun.github.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateRepoRequest(
    val name: String,
    val description: String? = null,
    val private: Boolean = false,
    val auto_init: Boolean = false
)
