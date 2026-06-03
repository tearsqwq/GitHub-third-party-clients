package com.kun.github.presentation.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kun.github.data.model.GithubRepo
import com.kun.github.data.model.GithubUser
import com.kun.github.data.repository.user.UserRepository
import com.kun.github.presentation.components.ContributionDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 用户 ViewModel
 * 
 * 职责：
 * - 管理用户信息状态
 * - 管理仓库列表状态（我的仓库、星标仓库、热门仓库）
 * - 管理贡献日历数据
 * - 实现智能缓存策略
 * 
 * 优化点：
 * - 使用 StateFlow 替代 LiveData
 * - 实现缓存优先策略，提升用户体验
 * - 分离加载状态和刷新逻辑
 */
class UserViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    // ==================== 状态流 ====================
    
    // 用户信息状态
    private val _userState = MutableStateFlow<UserState>(UserState.Idle)
    val userState: StateFlow<UserState> = _userState.asStateFlow()
    
    // 我的仓库状态
    private val _reposState = MutableStateFlow<ReposState>(ReposState.Idle)
    val reposState: StateFlow<ReposState> = _reposState.asStateFlow()
    
    // 星标仓库状态
    private val _starredReposState = MutableStateFlow<ReposState>(ReposState.Idle)
    val starredReposState: StateFlow<ReposState> = _starredReposState.asStateFlow()
    
    // 热门仓库状态
    private val _trendingState = MutableStateFlow<ReposState>(ReposState.Idle)
    val trendingState: StateFlow<ReposState> = _trendingState.asStateFlow()
    
    // 创建仓库状态
    private val _createRepoState = MutableStateFlow<CreateRepoState>(CreateRepoState.Idle)
    val createRepoState: StateFlow<CreateRepoState> = _createRepoState.asStateFlow()
    
    // 贡献日历状态
    private val _contributionsState = MutableStateFlow<ContributionsState>(ContributionsState.Idle)
    val contributionsState: StateFlow<ContributionsState> = _contributionsState.asStateFlow()

    // ==================== 缓存 ====================
    
    /** 仓库列表缓存，避免切换页面重复加载 */
    private var cachedRepos: List<GithubRepo>? = null
    
    /** 星标仓库缓存 */
    private var cachedStarredRepos: List<GithubRepo>? = null
    
    /** 用户信息缓存 */
    private var cachedUser: GithubUser? = null

    // ==================== 用户信息 ====================
    
    /**
     * 加载用户信息
     * 
     * @param token GitHub 访问令牌
     */
    fun loadUser(token: String) {
        // 如果有缓存，先显示缓存
        cachedUser?.let { user ->
            _userState.value = UserState.Success(user)
        }
        
        viewModelScope.launch {
            if (cachedUser == null) {
                _userState.value = UserState.Loading
            }
            
            userRepository.getUser(token).fold(
                onSuccess = { user ->
                    cachedUser = user
                    _userState.value = UserState.Success(user)
                },
                onFailure = { error ->
                    if (cachedUser == null) {
                        _userState.value = UserState.Error(error.message ?: "Unknown error")
                    }
                }
            )
        }
    }

    // ==================== 仓库列表 ====================
    
    /**
     * 加载仓库列表
     * 
     * 策略：
     * - 有缓存时先显示缓存，后台静默刷新
     * - 无缓存时显示加载状态
     * 
     * @param token GitHub 访问令牌
     * @param forceRefresh 是否强制刷新
     */
    fun loadRepos(token: String, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!forceRefresh && cachedRepos != null) {
                // 先显示缓存
                _reposState.value = ReposState.Success(cachedRepos!!)
                // 后台静默刷新
                refreshReposInternal(token)
            } else {
                _reposState.value = ReposState.Loading
                refreshReposInternal(token)
            }
        }
    }
    
    private suspend fun refreshReposInternal(token: String) {
        userRepository.getRepos(token).fold(
            onSuccess = { repos ->
                cachedRepos = repos
                _reposState.value = ReposState.Success(repos)
            },
            onFailure = { error ->
                if (cachedRepos != null) {
                    // 有缓存时，显示缓存而不是错误
                    _reposState.value = ReposState.Success(cachedRepos!!)
                } else {
                    _reposState.value = ReposState.Error(error.message ?: "Unknown error")
                }
            }
        )
    }

    // ==================== 星标仓库 ====================
    
    /**
     * 加载星标仓库列表
     * 
     * @param token GitHub 访问令牌
     * @param forceRefresh 是否强制刷新
     */
    fun loadStarredRepos(token: String, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!forceRefresh && cachedStarredRepos != null) {
                _starredReposState.value = ReposState.Success(cachedStarredRepos!!)
                refreshStarredReposInternal(token)
            } else {
                _starredReposState.value = ReposState.Loading
                refreshStarredReposInternal(token)
            }
        }
    }
    
    private suspend fun refreshStarredReposInternal(token: String) {
        userRepository.getStarredRepos(token).fold(
            onSuccess = { repos ->
                cachedStarredRepos = repos
                _starredReposState.value = ReposState.Success(repos)
            },
            onFailure = { error ->
                if (cachedStarredRepos != null) {
                    _starredReposState.value = ReposState.Success(cachedStarredRepos!!)
                } else {
                    _starredReposState.value = ReposState.Error(error.message ?: "Unknown error")
                }
            }
        )
    }

    // ==================== 热门仓库 ====================
    
    /**
     * 加载热门仓库
     * 使用 GitHub Search API 模拟 Trending 功能
     * 
     * @param token GitHub 访问令牌
     */
    fun loadTrendingRepos(token: String) {
        viewModelScope.launch {
            _trendingState.value = ReposState.Loading
            _trendingState.value = userRepository.getTrendingRepos(token).fold(
                onSuccess = { repos -> ReposState.Success(repos) },
                onFailure = { error -> ReposState.Error(error.message ?: "Unknown error") }
            )
        }
    }

    // ==================== 仓库操作 ====================
    
    /**
     * 创建新仓库
     * 
     * @param token GitHub 访问令牌
     * @param name 仓库名称
     * @param description 仓库描述
     */
    fun createRepo(token: String, name: String, description: String?) {
        viewModelScope.launch {
            _createRepoState.value = CreateRepoState.Loading
            _createRepoState.value = userRepository.createRepo(token, name, description).fold(
                onSuccess = { repo -> 
                    // 创建成功后清除缓存
                    cachedRepos = null
                    CreateRepoState.Success(repo) 
                },
                onFailure = { CreateRepoState.Error(it.message ?: "Unknown error") }
            )
        }
    }
    
    fun resetCreateRepoState() {
        _createRepoState.value = CreateRepoState.Idle
    }

    // ==================== 贡献日历 ====================
    
    /**
     * 加载用户贡献数据
     * 
     * @param token GitHub 访问令牌
     * @param username GitHub 用户名
     */
    fun loadContributions(token: String, username: String) {
        viewModelScope.launch {
            _contributionsState.value = ContributionsState.Loading
            _contributionsState.value = userRepository.getUserContributions(token, username).fold(
                onSuccess = { ContributionsState.Success(it) },
                onFailure = { ContributionsState.Error(it.message ?: "获取贡献数据失败") }
            )
        }
    }
    
    /**
     * 刷新贡献数据
     */
    fun refreshContributions(token: String, username: String) {
        loadContributions(token, username)
    }

    // ==================== 状态重置 ====================
    
    /**
     * 重置所有状态
     * 用于退出登录或切换账号
     */
    fun resetState() {
        _userState.value = UserState.Idle
        _reposState.value = ReposState.Idle
        _starredReposState.value = ReposState.Idle
        _trendingState.value = ReposState.Idle
        _createRepoState.value = CreateRepoState.Idle
        _contributionsState.value = ContributionsState.Idle
        
        // 清除缓存
        clearCache()
    }
    
    /**
     * 清除所有缓存
     */
    fun clearCache() {
        cachedRepos = null
        cachedStarredRepos = null
        cachedUser = null
    }
    
    /**
     * 使仓库缓存失效
     * 用于删除仓库后强制刷新
     */
    fun invalidateReposCache() {
        cachedRepos = null
    }
    
    /**
     * 使所有缓存失效
     * 别名方法，语义更清晰
     */
    fun invalidateCache() {
        clearCache()
    }
}

// ==================== 状态定义 ====================

/** 贡献日历状态 */
sealed class ContributionsState {
    object Idle : ContributionsState()
    object Loading : ContributionsState()
    data class Success(val contributions: List<ContributionDay>) : ContributionsState()
    data class Error(val message: String) : ContributionsState()
}

/** 用户信息状态 */
sealed class UserState {
    object Idle : UserState()
    object Loading : UserState()
    data class Success(val user: GithubUser) : UserState()
    data class Error(val message: String) : UserState()
}

/** 仓库列表状态 */
sealed class ReposState {
    object Idle : ReposState()
    object Loading : ReposState()
    data class Success(val repos: List<GithubRepo>) : ReposState()
    data class Error(val message: String) : ReposState()
}

/** 创建仓库状态 */
sealed class CreateRepoState {
    object Idle : CreateRepoState()
    object Loading : CreateRepoState()
    data class Success(val repo: GithubRepo) : CreateRepoState()
    data class Error(val message: String) : CreateRepoState()
}
