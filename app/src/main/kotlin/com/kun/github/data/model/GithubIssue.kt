package com.kun.github.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GitHub Issue 数据模型
 */
@Serializable
data class GithubIssue(
    val id: Long = 0,
    val number: Int = 0,
    val title: String = "",
    val state: String = "",
    val user: GithubUser? = null,
    val labels: List<GithubLabel> = emptyList(),
    val comments: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    @SerialName("closed_at") val closedAt: String? = null,
    val body: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null
)

@Serializable
data class GithubLabel(
    val id: Long = 0,
    val name: String = "",
    val color: String = "",
    val description: String? = null
)

/**
 * GitHub Pull Request 数据模型
 */
@Serializable
data class GithubPullRequest(
    val id: Long = 0,
    val number: Int = 0,
    val title: String = "",
    val state: String = "",
    val user: GithubUser? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    @SerialName("closed_at") val closedAt: String? = null,
    @SerialName("merged_at") val mergedAt: String? = null,
    val draft: Boolean = false,
    val body: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    val head: BranchRef? = null,
    val base: BranchRef? = null
)

@Serializable
data class BranchRef(
    val ref: String = "",
    val sha: String = "",
    val repo: GithubRepo? = null
)

/**
 * GitHub Release 数据模型
 */
@Serializable
data class GithubRelease(
    val id: Long = 0,
    val name: String? = null,
    @SerialName("tag_name") val tagName: String = "",
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("published_at") val publishedAt: String = "",
    val author: GithubUser? = null,
    val assets: List<ReleaseAsset> = emptyList(),
    @SerialName("html_url") val htmlUrl: String? = null
)

@Serializable
data class ReleaseAsset(
    val id: Long = 0,
    val name: String = "",
    val size: Long = 0,
    @SerialName("download_count") val downloadCount: Int = 0,
    @SerialName("browser_download_url") val browserDownloadUrl: String = ""
)

/**
 * GitHub Contributor 数据模型
 */
@Serializable
data class GithubContributor(
    val id: Long = 0,
    val login: String = "",
    @SerialName("avatar_url") val avatarUrl: String = "",
    val contributions: Int = 0,
    @SerialName("html_url") val htmlUrl: String? = null
)
