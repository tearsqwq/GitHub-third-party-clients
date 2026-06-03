package com.kun.github.data.remote.api

import com.kun.github.data.model.GithubRepo
import com.kun.github.data.model.GithubUser
import com.kun.github.data.model.OAuthResponse
import com.kun.github.data.model.RepoContent
import com.kun.github.data.model.GithubEvent
import com.kun.github.data.model.GithubNotification
import com.kun.github.data.model.SearchRepositoriesResponse
import com.kun.github.data.model.SearchUsersResponse
import com.kun.github.data.remote.dto.CreateFileRequest
import com.kun.github.data.remote.dto.CreateRepoRequest
import com.kun.github.data.remote.dto.PagesDeploymentRequest
import com.kun.github.data.remote.dto.TokenRequest
import com.kun.github.data.remote.dto.UpdateFileRequest
import com.kun.github.data.remote.dto.UpdateRepoRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.HTTP
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface GithubApi {

    @POST("login/oauth/access_token")
    @Headers("Accept: application/json")
    suspend fun exchangeToken(@Body request: TokenRequest): OAuthResponse

    @GET("user")
    suspend fun getUser(): GithubUser

    @GET("users/{username}")
    suspend fun getUser(@Path("username") username: String): GithubUser

    @GET("user/repos?per_page=30&sort=updated")
    suspend fun getRepos(): List<GithubRepo>

    @GET("users/{username}/repos?per_page=30&sort=updated")
    suspend fun getUserRepos(@Path("username") username: String): List<GithubRepo>

    @GET("user/starred?per_page=30")
    suspend fun getStarredRepos(): List<GithubRepo>

    // 星标仓库
    @PUT("user/starred/{owner}/{repo}")
    suspend fun starRepo(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<Unit>

    @DELETE("user/starred/{owner}/{repo}")
    suspend fun unstarRepo(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<Unit>

    @GET("user/starred/{owner}/{repo}")
    suspend fun checkIfStarred(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<Unit>

    @POST("user/repos")
    @Headers("Accept: application/json")
    suspend fun createRepo(@Body request: CreateRepoRequest): GithubRepo

    @PATCH("repos/{owner}/{repo}")
    @Headers("Accept: application/json")
    suspend fun updateRepo(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: UpdateRepoRequest
    ): GithubRepo

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getRepoContents(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String
    ): List<RepoContent>

    @GET("repos/{owner}/{repo}/contents")
    suspend fun getRepoRootContents(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): List<RepoContent>

    @GET("repos/{owner}/{repo}/readme")
    @Streaming
    @Headers("Accept: application/vnd.github.html+json")
    suspend fun getReadme(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): ResponseBody

    @GET("repos/{owner}/{repo}/contents/{path}")
    @Headers("Accept: application/vnd.github.v3+json")
    suspend fun getFileContent(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Query("ref") ref: String? = null
    ): RepoContent

    @GET("repos/{owner}/{repo}/contents/{path}")
    @Streaming
    @Headers("Accept: application/vnd.github.raw+json")
    suspend fun getFileRawContent(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Query("ref") ref: String? = null
    ): ResponseBody

    @PUT("repos/{owner}/{repo}/contents/{path}")
    @Headers("Accept: application/json")
    suspend fun createFile(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Body request: CreateFileRequest
    ): ResponseBody

    @PUT("repos/{owner}/{repo}/contents/{path}")
    @Headers("Accept: application/json")
    suspend fun updateFile(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Body request: UpdateFileRequest
    ): ResponseBody

    // ============ GitHub Pages API ============

    @GET("repos/{owner}/{repo}/pages")
    @Headers("Accept: application/vnd.github+json")
    suspend fun getPagesInfo(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): ResponseBody

    @POST("repos/{owner}/{repo}/pages")
    @Headers("Accept: application/vnd.github+json")
    suspend fun enablePages(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: PagesDeploymentRequest
    ): ResponseBody

    @POST("repos/{owner}/{repo}/pages/builds")
    @Headers("Accept: application/vnd.github+json")
    suspend fun requestPagesBuild(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): ResponseBody

    // Fork 仓库
    @POST("repos/{owner}/{repo}/forks")
    @Headers("Accept: application/json")
    suspend fun forkRepo(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): ResponseBody

    // 获取仓库分支列表
    @GET("repos/{owner}/{repo}/branches")
    suspend fun getBranches(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): ResponseBody

    // 获取仓库 tags/releases
    @GET("repos/{owner}/{repo}/tags")
    suspend fun getTags(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): ResponseBody

    // 删除仓库（GitHub API 返回 204 No Content）
    @DELETE("repos/{owner}/{repo}")
    suspend fun deleteRepo(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<Unit>

    // 删除文件（使用 @HTTP 替代 @DELETE，因为 GitHub API 要求 DELETE 请求带 body）
    @HTTP(method = "DELETE", path = "repos/{owner}/{repo}/contents/{path}", hasBody = true)
    suspend fun deleteFile(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Body request: com.kun.github.data.remote.dto.DeleteFileRequest
    ): ResponseBody

    // ============ GitHub Events API ============

    /**
     * 获取用户的公开事件列表
     * 用于统计用户的贡献活动
     * @param username GitHub 用户名
     * @param page 页码
     * @param perPage 每页数量 (最大 100)
     */
    @GET("users/{username}/events/public")
    suspend fun getUserEvents(
        @Path("username") username: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 100
    ): List<com.kun.github.data.model.GithubEvent>

    /**
     * 获取当前认证用户的事件列表
     * 包含私有仓库的事件
     */
    @GET("users/{username}/events")
    suspend fun getUserEventsAuthenticated(
        @Path("username") username: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 100
    ): List<com.kun.github.data.model.GithubEvent>

    // ============ Trending Repositories (Search API) ============

    /**
     * 搜索最近30天内创建的最多 star 的仓库（模拟 Trending）
     * @param query 搜索条件，格式如 "created:>2026-04-26"
     * @param sort 排序字段，默认 stars
     * @param order 排序方向，默认 desc
     * @param perPage 每页数量，默认 30
     */
    @GET("search/repositories")
    suspend fun searchRepositories(
        @Query("q") query: String,
        @Query("sort") sort: String = "stars",
        @Query("order") order: String = "desc",
        @Query("per_page") perPage: Int = 30
    ): SearchRepositoriesResponse

    // ============ GitHub Notifications API ============

    /**
     * 获取当前认证用户的通知列表
     * @param all 如果为 true，返回所有通知包括已读。默认为 false，只返回未读通知
     * @param page 页码
     * @param perPage 每页数量，默认 30
     */
    @GET("notifications")
    suspend fun getNotifications(
        @Query("all") all: Boolean = false,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): List<GithubNotification>

    /**
     * 标记通知线程为已读
     * @param threadId 通知线程 ID
     */
    @PATCH("notifications/threads/{thread_id}")
    suspend fun markNotificationAsRead(
        @Path("thread_id") threadId: String
    ): Response<Unit>

    /**
     * 标记所有通知为已读
     */
    @PUT("notifications")
    suspend fun markAllNotificationsAsRead(): Response<Unit>

    // ============ GitHub Search API ============

    @GET("search/repositories")
    suspend fun searchRepositories(
        @Query("q") query: String,
        @Query("per_page") perPage: Int = 30
    ): SearchRepositoriesResponse

    @GET("search/users")
    suspend fun searchUsers(
        @Query("q") query: String,
        @Query("per_page") perPage: Int = 30
    ): SearchUsersResponse

    // ============ Issues API ============

    @GET("repos/{owner}/{repo}/issues")
    suspend fun getIssues(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "open",
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): List<com.kun.github.data.model.GithubIssue>

    @POST("repos/{owner}/{repo}/issues")
    suspend fun createIssue(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: CreateIssueRequest
    ): com.kun.github.data.model.GithubIssue

    // ============ Pull Requests API ============

    @GET("repos/{owner}/{repo}/pulls")
    suspend fun getPullRequests(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "open",
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): List<com.kun.github.data.model.GithubPullRequest>

    // ============ Releases API ============

    @GET("repos/{owner}/{repo}/releases")
    suspend fun getReleases(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): List<com.kun.github.data.model.GithubRelease>

    // ============ Contributors API ============

    @GET("repos/{owner}/{repo}/contributors")
    suspend fun getContributors(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): List<com.kun.github.data.model.GithubContributor>
}

@kotlinx.serialization.Serializable
data class CreateIssueRequest(
    val title: String,
    val body: String? = null
)
