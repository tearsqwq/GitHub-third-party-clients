package com.kun.github.presentation.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kun.github.data.local.preferences.AccountsManager
import com.kun.github.data.local.preferences.GitHubAccount
import com.kun.github.data.model.GithubUser
import com.kun.github.data.remote.client.GithubApiClient
import com.kun.github.data.repository.auth.AuthRepository
import com.kun.github.data.repository.user.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 认证 ViewModel
 * 
 * 职责：
 * - 管理 OAuth 认证流程
 * - 管理登录状态
 * - 支持多账号管理
 * 
 * 优化点：
 * - 使用 StateFlow 管理状态
 * - 分离用户信息获取逻辑
 * - 支持账号切换
 */
class AuthViewModel(
    private val authRepository: AuthRepository,
    private val context: Context
) : ViewModel() {

    // ==================== 状态流 ====================
    
    /** 认证状态 */
    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state.asStateFlow()
    
    /** 用户信息（用于显示当前登录用户） */
    private val _userInfo = MutableStateFlow<GithubUser?>(null)
    val userInfo: StateFlow<GithubUser?> = _userInfo.asStateFlow()
    
    /** Token 流（来自 AuthRepository） */
    val tokenFlow = authRepository.tokenFlow

    // ==================== OAuth 认证 ====================
    
    /**
     * 使用授权码交换 Token
     * 
     * 流程：
     * 1. 调用 GitHub API 交换 Token
     * 2. 获取用户信息
     * 3. 更新认证状态
     * 
     * @param code OAuth 授权码
     */
    fun exchangeCode(code: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            
            val result = authRepository.exchangeCode(code)
            
            result.fold(
                onSuccess = { token ->
                    // 获取用户信息
                    val user = fetchUserInfo(token)
                    _state.value = AuthState.Success(token, user)
                },
                onFailure = { error ->
                    _state.value = AuthState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }
    
    /**
     * 获取用户信息
     * 
     * @param token GitHub 访问令牌
     * @return 用户信息，失败返回 null
     */
    private suspend fun fetchUserInfo(token: String): GithubUser? {
        return try {
            val userRepository = UserRepository(context.cacheDir)
            userRepository.getUser(token).getOrNull()
        } catch (e: Exception) {
            null
        }
    }

    // ==================== 多账号管理 ====================
    
    /**
     * 保存当前账号到多账号管理器
     * 
     * @param token GitHub 访问令牌
     * @param user 用户信息
     */
    suspend fun saveCurrentAccount(token: String, user: GithubUser?) {
        val accountsManager = AccountsManager(context)
        val account = GitHubAccount(
            id = System.currentTimeMillis().toString(),
            username = user?.login ?: "User",
            avatarUrl = user?.avatarUrl ?: "",
            token = token,
            isCurrent = true
        )
        accountsManager.addAccount(account)
    }

    // ==================== 登出 ====================
    
    /**
     * 退出登录
     * 
     * 流程：
     * 1. 清除 Token
     * 2. 清除缓存
     * 3. 重置状态
     */
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _state.value = AuthState.Idle
            _userInfo.value = null
        }
    }

    // ==================== 状态重置 ====================
    
    /**
     * 重置认证状态
     * 用于清除错误信息或重置到初始状态
     */
    fun resetState() {
        _state.value = AuthState.Idle
    }
}

// ==================== 认证状态定义 ====================

/**
 * 认证状态
 * 
 * 状态流转：
 * Idle -> Loading -> Success/Error
 */
sealed class AuthState {
    /** 初始状态 */
    object Idle : AuthState()
    
    /** 加载中 */
    object Loading : AuthState()
    
    /** 认证成功 */
    data class Success(
        val token: String,
        val user: GithubUser? = null
    ) : AuthState()
    
    /** 认证失败 */
    data class Error(val message: String) : AuthState()
}
