package com.kun.github

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kun.github.data.local.preferences.AppPreferences
import com.kun.github.data.local.preferences.AuthPreferences
import com.kun.github.data.remote.client.GithubApiClient
import com.kun.github.data.model.GithubRepo
import com.kun.github.service.KeepAliveService
import com.kun.github.di.AuthViewModelFactory
import com.kun.github.di.UserViewModelFactory
import com.kun.github.presentation.auth.AuthState
import com.kun.github.presentation.auth.AuthViewModel
import com.kun.github.presentation.settings.LanguageManager
import com.kun.github.presentation.settings.LocalStrings
import com.kun.github.presentation.settings.getAppStrings
import com.kun.github.presentation.theme.GitHubTheme
import com.kun.github.presentation.user.UserState
import com.kun.github.presentation.user.UserViewModel
import com.kun.github.ui.screens.login.LoginScreen
import com.kun.github.ui.screens.splash.SplashScreen
import com.kun.github.ui.screens.terms.TermsWebViewScreen
import com.kun.github.utils.oauth.OAuthHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

enum class AppScreen {
    SPLASH,           // 启动页（包含协议同意）
    TERMS_WEB_VIEW,   // 服务协议 WebView
    PRIVACY_WEB_VIEW, // 隐私政策 WebView
    LOGIN,            // 登录页
    MAIN              // 主页面
}

class MainActivity : ComponentActivity() {

    private lateinit var authViewModelFactory: AuthViewModelFactory
    private lateinit var userViewModelFactory: UserViewModelFactory
    private lateinit var appPreferences: AppPreferences
    private lateinit var authPreferences: AuthPreferences

    override fun attachBaseContext(newBase: android.content.Context) {
        val context = LanguageManager.applyLanguage(newBase)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        // 启用全屏显示，设置透明导航栏
        enableEdgeToEdge(
    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
)
        // 禁用导航栏对比度强制执行（Android 10+），实现完全透明
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        // 初始化
        authViewModelFactory = AuthViewModelFactory(applicationContext)
        userViewModelFactory = UserViewModelFactory(applicationContext)
        appPreferences = AppPreferences(applicationContext)
        authPreferences = AuthPreferences(applicationContext)

        // 启动保活服务
        KeepAliveService.start(this)

        handleIntent(intent)

        setContent {
            val appPrefs = AppPreferences(applicationContext)
            var themeMode by remember {
                mutableStateOf(
                    when (appPrefs.getThemeMode()) {
                        "LIGHT" -> ThemeMode.LIGHT
                        "DARK" -> ThemeMode.DARK
                        else -> ThemeMode.SYSTEM
                    }
                )
            }
            LaunchedEffect(themeMode) {
                appPrefs.saveThemeMode(themeMode.name)
            }
            val isSystemDark = isSystemInDarkTheme()
            val isDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemDark
            }
            
            // 多语言支持
            val currentLanguage = LanguageManager.getCurrentLanguage(LocalContext.current)
            val appStrings = remember(currentLanguage) { getAppStrings(currentLanguage) }

            // 应用状态
            var currentScreen by remember { mutableStateOf(AppScreen.SPLASH) }
            var hasAcceptedTerms by remember { mutableStateOf(false) }
            var hasToken by remember { mutableStateOf<Boolean?>(null) }
            var isLoading by remember { mutableStateOf(true) }

            // 初始化检查 - 一次性检查所有状态
            LaunchedEffect(Unit) {
                hasAcceptedTerms = appPreferences.hasAcceptedTermsFlow.first()
                val token = authPreferences.tokenFlow.first()
                hasToken = token != null
                isLoading = false

                // 决定初始页面
                currentScreen = when {
                    !hasAcceptedTerms -> AppScreen.SPLASH
                    token != null -> AppScreen.MAIN
                    else -> AppScreen.LOGIN
                }
            }

            CompositionLocalProvider(LocalStrings provides appStrings) {
            GitHubTheme(darkTheme = isDarkTheme) {
                when (currentScreen) {
                    AppScreen.SPLASH -> {
                        SplashScreen(
                            hasAcceptedTerms = hasAcceptedTerms,
                            onSplashComplete = {
                                // 协议已同意，根据token状态跳转
                                lifecycleScope.launch {
                                    val token = authPreferences.tokenFlow.first()
                                    currentScreen = if (token != null) {
                                        AppScreen.MAIN
                                    } else {
                                        AppScreen.LOGIN
                                    }
                                }
                            },
                            onTermsAccepted = {
                                lifecycleScope.launch {
                                    appPreferences.setTermsAccepted(true)
                                    hasAcceptedTerms = true
                                    // 同意协议后跳转到登录页
                                    currentScreen = AppScreen.LOGIN
                                }
                            },
                            onExitApp = {
                                finish()
                            },
                            onViewTerms = {
                                currentScreen = AppScreen.TERMS_WEB_VIEW
                            },
                            onViewPrivacy = {
                                currentScreen = AppScreen.PRIVACY_WEB_VIEW
                            }
                        )
                    }

                    AppScreen.TERMS_WEB_VIEW -> {
                        TermsWebViewScreen(
                            title = "用户服务协议",
                            assetFileName = "terms_of_service.html",
                            onBack = {
                                currentScreen = AppScreen.SPLASH
                            }
                        )
                    }

                    AppScreen.PRIVACY_WEB_VIEW -> {
                        TermsWebViewScreen(
                            title = "隐私政策",
                            assetFileName = "privacy_policy.html",
                            onBack = {
                                currentScreen = AppScreen.SPLASH
                            }
                        )
                    }

                    AppScreen.LOGIN, AppScreen.MAIN -> {
                        MainAppContent(
                            themeMode = themeMode,
                            isDarkTheme = isDarkTheme,
                            onThemeModeChange = { themeMode = it },
                            authViewModelFactory = authViewModelFactory,
                            userViewModelFactory = userViewModelFactory,
                            onNavigateToLogin = { currentScreen = AppScreen.LOGIN },
                            onNavigateToMain = { currentScreen = AppScreen.MAIN },
                            onOAuthLogin = { launchOAuth() },
                            onViewTerms = { currentScreen = AppScreen.TERMS_WEB_VIEW },
                            onViewPrivacy = { currentScreen = AppScreen.PRIVACY_WEB_VIEW }
                        )
                    }
                }
            }
            } // CompositionLocalProvider
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.data?.let { uri ->
            if (uri.scheme == "github" && uri.host == "oauth" && uri.path == "/callback") {
                OAuthHelper.parseCallback(uri)?.let { (code, _) ->
                    val viewModel = ViewModelProvider(this, authViewModelFactory)[AuthViewModel::class.java]
                    viewModel.exchangeCode(code)
                }
            }
        }
    }

    private fun launchOAuth() {
        val (url, _) = OAuthHelper.buildAuthorizeUrl()
        CustomTabsIntent.Builder().build().launchUrl(this, url.toUri())
    }
}

@Composable
private fun MainAppContent(
    themeMode: ThemeMode,
    isDarkTheme: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    authViewModelFactory: AuthViewModelFactory,
    userViewModelFactory: UserViewModelFactory,
    onNavigateToLogin: () -> Unit,
    onNavigateToMain: () -> Unit,
    onOAuthLogin: () -> Unit,
    onViewTerms: () -> Unit,
    onViewPrivacy: () -> Unit
) {
    val authViewModel: AuthViewModel = viewModel(factory = authViewModelFactory)
    val userViewModel: UserViewModel = viewModel(factory = userViewModelFactory)
    val context = LocalContext.current

    val token by authViewModel.tokenFlow.collectAsState(initial = null)
    val authState by authViewModel.state.collectAsState()
    val userState by userViewModel.userState.collectAsState()

    val currentToken = (authState as? AuthState.Success)?.token ?: token
    val selectedRepo = remember { mutableStateOf<GithubRepo?>(null) }
    val currentUserLogin = (userState as? UserState.Success)?.user?.login

    // 当获取到 token 且用户状态为 Idle 时加载用户信息
    LaunchedEffect(currentToken, userState) {
        if (currentToken != null && userState is UserState.Idle) {
            userViewModel.loadUser(currentToken)
        }
    }

    // 登录成功后保存账号信息并刷新 API Client
    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            val success = authState as AuthState.Success
            // 刷新 API Client
            GithubApiClient.refreshClient(success.token, context.cacheDir)
            // 清除缓存重新加载
            userViewModel.resetState()
            userViewModel.invalidateCache()
            // 保存账号
            success.user?.let { user ->
                authViewModel.saveCurrentAccount(success.token, user)
            }
        }
    }

    // 根据 token 状态导航 - 只在token变化时触发
    LaunchedEffect(currentToken) {
        if (currentToken != null) {
            onNavigateToMain()
        } else {
            onNavigateToLogin()
        }
    }

    if (currentToken != null) {
        MainContent(
            currentToken = currentToken,
            selectedRepo = selectedRepo.value,
            currentUserLogin = currentUserLogin,
            themeMode = themeMode,
            isDarkTheme = isDarkTheme,
            onThemeModeChange = onThemeModeChange,
            onRepoSelected = { selectedRepo.value = it },
            onBack = { selectedRepo.value = null },
            authViewModel = authViewModel,
            userViewModel = userViewModel,
            cacheDir = androidx.compose.ui.platform.LocalContext.current.cacheDir,
            onOAuthLogin = onOAuthLogin
        )
    } else {
        LoginScreen(
            errorMessage = (authState as? AuthState.Error)?.message,
            onLoginClick = onOAuthLogin,
            onViewTerms = onViewTerms,
            onViewPrivacy = onViewPrivacy
        )
    }
}
