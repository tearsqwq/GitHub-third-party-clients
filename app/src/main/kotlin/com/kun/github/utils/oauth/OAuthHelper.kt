package com.kun.github.utils.oauth

import android.net.Uri
import com.kun.github.config.GitHubConfig
import java.util.UUID

/**
 * OAuth 辅助工具
 * 
 * 职责：
 * - 构建 GitHub OAuth 授权 URL
 * - 解析 OAuth 回调 URI
 * - 生成安全的 state 参数
 * 
 * 安全特性：
 * - 使用 UUID 生成随机 state 参数防止 CSRF 攻击
 * - 对 redirect_uri 和 scope 进行 URL 编码
 */
object OAuthHelper {

    // ==================== URL 构建 ====================
    
    /**
     * 构建 GitHub OAuth 授权 URL
     * 
     * 流程：
     * 1. 生成随机 state 参数
     * 2. 拼接授权 URL
     * 3. 对特殊参数进行 URL 编码
     * 
     * @return Pair<授权URL, state参数>
     */
    fun buildAuthorizeUrl(): Pair<String, String> {
        // 生成随机 state 参数，用于防止 CSRF 攻击
        val state = UUID.randomUUID().toString()
        
        val url = buildString {
            append(GitHubConfig.AUTHORIZE_URL)
            append("?client_id=${GitHubConfig.CLIENT_ID}")
            append("&redirect_uri=${Uri.encode(GitHubConfig.REDIRECT_URI)}")
            append("&scope=${Uri.encode(GitHubConfig.SCOPE)}")
            append("&state=$state")
        }
        
        return url to state
    }

    // ==================== 回调解析 ====================
    
    /**
     * 解析 OAuth 回调 URI
     * 
     * 验证：
     * - 检查 URI scheme 和 host
     * - 提取 code 和 state 参数
     * 
     * @param uri 回调 URI
     * @return Pair<授权码, state参数>，解析失败返回 null
     */
    fun parseCallback(uri: Uri): Pair<String, String>? {
        // 验证 URI 格式
        if (uri.scheme != "github" || uri.host != "oauth" || uri.path != "/callback") {
            return null
        }
        
        // 提取参数
        val code = uri.getQueryParameter("code") ?: return null
        val state = uri.getQueryParameter("state") ?: return null
        
        return code to state
    }
    
    /**
     * 验证 state 参数
     * 
     * 用于防止 CSRF 攻击
     * 
     * @param receivedState 收到的 state 参数
     * @param expectedState 期望的 state 参数
     * @return 是否匹配
     */
    fun validateState(receivedState: String?, expectedState: String): Boolean {
        return receivedState != null && receivedState == expectedState
    }
}
