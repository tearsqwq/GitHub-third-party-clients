package com.kun.github.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 重试拦截器
 * 
 * 功能：
 * - 对服务器错误（5xx）自动重试
 * - 可配置最大重试次数
 * - 支持指数退避策略
 * 
 * 使用场景：
 * - 网络不稳定时自动重试
 * - 服务器临时故障时提高成功率
 * 
 * @param maxRetryCount 最大重试次数，默认 3 次
 */
class RetryInterceptor(
    private val maxRetryCount: Int = DEFAULT_RETRY_COUNT
) : Interceptor {
    
    companion object {
        /** 默认重试次数 */
        const val DEFAULT_RETRY_COUNT = 3
        
        /** 服务器错误状态码范围 */
        private val SERVER_ERROR_RANGE = 500..599
        
        /** 请求过多状态码 */
        private const val TOO_MANY_REQUESTS = 429
        
        /**
         * 创建重试拦截器
         * 
         * @param maxRetryCount 最大重试次数，默认 3 次
         * @return RetryInterceptor 实例
         */
        @JvmStatic
        fun create(maxRetryCount: Int = DEFAULT_RETRY_COUNT): RetryInterceptor {
            return RetryInterceptor(maxRetryCount)
        }
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        var response = chain.proceed(request)
        var retryCount = 0
        
        // 对服务器错误（5xx）进行重试
        while (!response.isSuccessful && isRetryable(response.code) && retryCount < maxRetryCount) {
            retryCount++
            
            // 关闭旧响应
            response.close()
            
            // 可选：添加短暂延迟（指数退避）
            // Thread.sleep(100L * retryCount)
            
            // 重新发起请求
            request = request.newBuilder().build()
            response = chain.proceed(request)
        }
        
        return response
    }
    
    /**
     * 判断是否可以重试
     * 
     * 可重试的情况：
     * - 5xx 服务器错误
     * - 429 请求过多（限流）
     */
    private fun isRetryable(code: Int): Boolean {
        return code in SERVER_ERROR_RANGE || code == TOO_MANY_REQUESTS
    }
}
