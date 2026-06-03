package com.kun.github.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateRepoRequest(
    val name: String,
    val description: String? = null,
    val private: Boolean = false,
    val homepage: String? = null,
    @SerialName("has_issues") val hasIssues: Boolean? = null,
    @SerialName("has_projects") val hasProjects: Boolean? = null,
    @SerialName("has_wiki") val hasWiki: Boolean? = null,
    @SerialName("has_discussions") val hasDiscussions: Boolean? = null,
    @SerialName("allow_squash_merge") val allowSquashMerge: Boolean? = null,
    @SerialName("allow_merge_commit") val allowMergeCommit: Boolean? = null,
    @SerialName("allow_rebase_merge") val allowRebaseMerge: Boolean? = null,
    @SerialName("delete_branch_on_merge") val deleteBranchOnMerge: Boolean? = null,
    val archived: Boolean? = null
)
