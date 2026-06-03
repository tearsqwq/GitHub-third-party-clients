package com.kun.github.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GitHub 通知数据模型
 * 用于获取用户的通知列表
 */
@Serializable
data class GithubNotification(
    val id: String,
    val subject: NotificationSubject,
    val repository: NotificationRepo,
    val reason: String,
    val unread: Boolean,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class NotificationSubject(
    val title: String,
    val url: String? = null,
    val type: String
)

@Serializable
data class NotificationRepo(
    @SerialName("full_name") val fullName: String
)
