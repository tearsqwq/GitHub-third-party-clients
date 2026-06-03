package com.kun.github.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kun.github.data.local.preferences.AuthPreferences
import com.kun.github.data.repository.auth.AuthRepository
import com.kun.github.data.repository.user.UserRepository
import com.kun.github.presentation.auth.AuthViewModel
import com.kun.github.presentation.user.UserViewModel

class AuthViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    private val authPreferences = AuthPreferences(context.applicationContext)
    private val authRepository = AuthRepository(authPreferences)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(authRepository, context.applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

class UserViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    // 使用应用的缓存目录来存储 HTTP 缓存
    private val userRepository = UserRepository(context.applicationContext.cacheDir)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
            return UserViewModel(userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
