package com.kun.github

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.navigationBarsPadding
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.Icon
import com.kun.github.presentation.settings.strings
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import com.kun.github.R
import com.kun.github.data.model.GithubRepo
import com.kun.github.data.repository.user.UserRepository
import com.kun.github.presentation.auth.AuthViewModel
import com.kun.github.presentation.navigation.LiquidBottomTab
import com.kun.github.presentation.navigation.LiquidBottomTabs
import com.kun.github.presentation.user.ReposState
import com.kun.github.presentation.user.UserViewModel
import com.kun.github.ui.screens.accounts.AccountsScreen
import com.kun.github.ui.screens.deploy.DeployDetailScreen
import com.kun.github.ui.screens.deploy.DeployScreen
import com.kun.github.ui.screens.detail.RepoDetailScreen
import com.kun.github.ui.screens.detail.IssuesScreen
import com.kun.github.ui.screens.detail.PullRequestsScreen
import com.kun.github.ui.screens.detail.ReleasesScreen
import com.kun.github.ui.screens.detail.ContributorsScreen
import com.kun.github.ui.screens.fileeditor.FileEditorScreen
import com.kun.github.ui.screens.home.HomeScreen
import com.kun.github.ui.screens.imagepreview.ImagePreviewScreen
import com.kun.github.ui.screens.notifications.NotificationsScreen
import com.kun.github.ui.screens.userprofile.UserProfileScreen
import com.kun.github.ui.screens.profile.ProfileScreen
import com.kun.github.ui.screens.about.AboutScreen
import com.kun.github.ui.screens.settings.SettingsScreen
import com.kun.github.ui.screens.stars.StarsScreen
import com.kun.github.ui.screens.search.SearchScreen
import com.kun.github.ui.screens.terms.TermsWebViewScreen
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import java.io.File

sealed class Screen {
    abstract val key: Any
    
    data object Main : Screen() {
        override val key = "Main"
    }
    data object Settings : Screen() {
        override val key = "Settings"
    }
    data object About : Screen() {
        override val key = "About"
    }
    data object Accounts : Screen() {
        override val key = "Accounts"
    }
    data class RepoDetail(val repo: GithubRepo) : Screen() {
        override val key = "RepoDetail-${repo.id}"
    }
    data class RepoIssues(val repo: GithubRepo) : Screen() {
        override val key = "RepoIssues-${repo.id}"
    }
    data class RepoPullRequests(val repo: GithubRepo) : Screen() {
        override val key = "RepoPullRequests-${repo.id}"
    }
    data class RepoReleases(val repo: GithubRepo) : Screen() {
        override val key = "RepoReleases-${repo.id}"
    }
    data class RepoContributors(val repo: GithubRepo) : Screen() {
        override val key = "RepoContributors-${repo.id}"
    }
    data class FilePreview(val repo: GithubRepo, val filePath: String, val isOwnRepo: Boolean) : Screen() {
        override val key = "FilePreview-${repo.id}-$filePath"
    }
    data class ImagePreview(val repo: GithubRepo, val imagePath: String) : Screen() {
        override val key = "ImagePreview-${repo.id}-$imagePath"
    }
    data class DeployDetail(val repo: GithubRepo) : Screen() {
        override val key = "DeployDetail-${repo.id}"
    }
    data object Search : Screen() {
        override val key = "Search"
    }
    data object Notifications : Screen() {
        override val key = "Notifications"
    }
    data class UserProfile(val username: String) : Screen() {
        override val key = "UserProfile-$username"
    }
    data class WebViewPage(val title: String, val assetFileName: String) : Screen() {
        override val key = "WebViewPage-$assetFileName"
    }
}

@Composable
fun MainContent(
    currentToken: String,
    selectedRepo: GithubRepo?,
    currentUserLogin: String?,
    themeMode: ThemeMode,
    isDarkTheme: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    onRepoSelected: (GithubRepo) -> Unit,
    onBack: () -> Unit,
    authViewModel: AuthViewModel,
    userViewModel: UserViewModel,
    cacheDir: File? = null,
    onOAuthLogin: (() -> Unit)? = null
) {
    val userRepository = remember { UserRepository(cacheDir) }
    val s = strings()

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Main) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var isFromSearch by remember { mutableStateOf(false) }
    var unreadNotificationCount by remember { mutableStateOf(0) }

    if (selectedRepo != null && currentScreen !is Screen.RepoDetail && currentScreen !is Screen.RepoIssues && currentScreen !is Screen.RepoPullRequests && currentScreen !is Screen.RepoReleases && currentScreen !is Screen.RepoContributors && currentScreen !is Screen.FilePreview && currentScreen !is Screen.ImagePreview && currentScreen !is Screen.DeployDetail && currentScreen !is Screen.Settings && currentScreen !is Screen.About && currentScreen !is Screen.Search && currentScreen !is Screen.Notifications && currentScreen !is Screen.UserProfile) {
        currentScreen = Screen.RepoDetail(selectedRepo)
    }

    Crossfade(
        targetState = currentScreen.key,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "screenTransition"
    ) { _ ->
        val screen = currentScreen
        when (screen) {
            is Screen.Main -> {
                val backdrop = rememberLayerBackdrop()
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    Box(modifier = Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                        when (selectedTab) {
                            0 -> HomeScreen(
                                modifier = Modifier.statusBarsPadding(),
                                token = currentToken,
                                viewModel = userViewModel,
                                userRepository = userRepository,
                                onRepoClick = {
                                    onRepoSelected(it)
                                },
                                onSearchClick = {
                                    currentScreen = Screen.Search
                                },
                                onOwnerClick = { username ->
                                    currentScreen = Screen.UserProfile(username)
                                },
                                cacheDir = cacheDir
                            )
                            1 -> {
                                val reposState by userViewModel.reposState.collectAsState()
                                val repos = when (val state = reposState) {
                                    is ReposState.Success -> state.repos
                                    else -> emptyList()
                                }
                                DeployScreen(
                                    modifier = Modifier.statusBarsPadding(),
                                    token = currentToken,
                                    repos = repos,
                                    userLogin = currentUserLogin,
                                    userRepository = userRepository,
                                    onRepoClick = { repo ->
                                        currentScreen = Screen.DeployDetail(repo)
                                    }
                                )
                            }
                            2 -> StarsScreen(
                                modifier = Modifier.statusBarsPadding(),
                                token = currentToken,
                                viewModel = userViewModel,
                                userRepository = userRepository,
                                onRepoClick = {
                                    onRepoSelected(it)
                                },
                                onOwnerClick = { username ->
                                    currentScreen = Screen.UserProfile(username)
                                },
                                cacheDir = cacheDir
                            )
                            3 -> ProfileScreen(
                                modifier = Modifier.statusBarsPadding(),
                                token = currentToken,
                                themeMode = themeMode,
                                isDarkTheme = isDarkTheme,
                                onThemeModeChange = onThemeModeChange,
                                authViewModel = authViewModel,
                                userViewModel = userViewModel,
                                cacheDir = cacheDir,
                                onNavigateToSettings = {
                                    currentScreen = Screen.Settings
                                },
                                onNavigateToNotifications = {
                                    currentScreen = Screen.Notifications
                                },
                                unreadNotificationCount = unreadNotificationCount
                            )
                        }

                    }
                    // 导航栏图标颜色：深色模式用白色，浅色模式用黑色
                    val navIconTint = if (isDarkTheme) Color.White else Color.Black
                    
                    LiquidBottomTabs(
                        selectedTabIndex = { selectedTab },
                        onTabSelected = { selectedTab = it },
                        backdrop = backdrop,
                        tabsCount = 4,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = 32.dp)
                    ) {
                        // Tab 0: 首页
                        val isHomeSelected = selectedTab == 0
                        LiquidBottomTab(onClick = { selectedTab = 0 }) {
                            Icon(
                                painter = painterResource(
                                    id = if (isHomeSelected) R.drawable.ic_nav_home_filled else R.drawable.ic_nav_home_outline
                                ),
                                contentDescription = s.navHome,
                                tint = navIconTint
                            )
                        }
                        // Tab 1: 部署
                        val isDeploySelected = selectedTab == 1
                        LiquidBottomTab(onClick = { selectedTab = 1 }) {
                            Icon(
                                painter = painterResource(
                                    id = if (isDeploySelected) R.drawable.ic_nav_cloud_filled else R.drawable.ic_nav_cloud_outline
                                ),
                                contentDescription = s.navDeploy,
                                tint = navIconTint
                            )
                        }
                        // Tab 2: 星标
                        val isStarsSelected = selectedTab == 2
                        LiquidBottomTab(onClick = { selectedTab = 2 }) {
                            Icon(
                                painter = painterResource(
                                    id = if (isStarsSelected) R.drawable.ic_nav_star_filled else R.drawable.ic_nav_star_outline
                                ),
                                contentDescription = s.navStars,
                                tint = navIconTint
                            )
                        }
                        // Tab 3: 个人
                        val isProfileSelected = selectedTab == 3
                        LiquidBottomTab(onClick = { selectedTab = 3 }) {
                            Icon(
                                painter = painterResource(
                                    id = if (isProfileSelected) R.drawable.ic_nav_profile_filled else R.drawable.ic_nav_profile_outline
                                ),
                                contentDescription = s.navProfile,
                                tint = navIconTint
                            )
                        }
                    }
                }
            }
            is Screen.RepoDetail -> {
                BackHandler {
                    if (isFromSearch) {
                        isFromSearch = false
                        currentScreen = Screen.Search
                    } else {
                        currentScreen = Screen.Main
                        onBack()
                    }
                }
                val isOwnRepo = currentUserLogin != null && screen.repo.owner.login.equals(currentUserLogin, ignoreCase = true)
                RepoDetailScreen(
                    repo = screen.repo,
                    token = currentToken,
                    currentUserLogin = currentUserLogin,
                    onBack = {
                        if (isFromSearch) {
                            isFromSearch = false
                            currentScreen = Screen.Search
                        } else {
                            currentScreen = Screen.Main
                            onBack()
                        }
                    },
                    userRepository = userRepository,
                    onPreviewFile = { filePath ->
                        currentScreen = Screen.FilePreview(screen.repo, filePath, isOwnRepo)
                    },
                    onPreviewImage = { imagePath ->
                        currentScreen = Screen.ImagePreview(screen.repo, imagePath)
                    },
                    onOwnerClick = { username ->
                        currentScreen = Screen.UserProfile(username)
                    },
                    onNavigateToIssues = {
                        currentScreen = Screen.RepoIssues(screen.repo)
                    },
                    onNavigateToPullRequests = {
                        currentScreen = Screen.RepoPullRequests(screen.repo)
                    },
                    onNavigateToReleases = {
                        currentScreen = Screen.RepoReleases(screen.repo)
                    },
                    onNavigateToContributors = {
                        currentScreen = Screen.RepoContributors(screen.repo)
                    },
                    cacheDir = cacheDir
                )
            }
            is Screen.RepoIssues -> {
                BackHandler {
                    currentScreen = Screen.RepoDetail(screen.repo)
                }
                IssuesScreen(
                    owner = screen.repo.owner.login,
                    repo = screen.repo.name,
                    token = currentToken,
                    userRepository = userRepository,
                    onBack = {
                        currentScreen = Screen.RepoDetail(screen.repo)
                    }
                )
            }
            is Screen.RepoPullRequests -> {
                BackHandler {
                    currentScreen = Screen.RepoDetail(screen.repo)
                }
                PullRequestsScreen(
                    owner = screen.repo.owner.login,
                    repo = screen.repo.name,
                    token = currentToken,
                    userRepository = userRepository,
                    onBack = {
                        currentScreen = Screen.RepoDetail(screen.repo)
                    }
                )
            }
            is Screen.RepoReleases -> {
                BackHandler {
                    currentScreen = Screen.RepoDetail(screen.repo)
                }
                ReleasesScreen(
                    owner = screen.repo.owner.login,
                    repo = screen.repo.name,
                    token = currentToken,
                    userRepository = userRepository,
                    onBack = {
                        currentScreen = Screen.RepoDetail(screen.repo)
                    }
                )
            }
            is Screen.RepoContributors -> {
                BackHandler {
                    currentScreen = Screen.RepoDetail(screen.repo)
                }
                ContributorsScreen(
                    owner = screen.repo.owner.login,
                    repo = screen.repo.name,
                    token = currentToken,
                    userRepository = userRepository,
                    onBack = {
                        currentScreen = Screen.RepoDetail(screen.repo)
                    },
                    onUserClick = { username ->
                        currentScreen = Screen.UserProfile(username)
                    }
                )
            }
            is Screen.FilePreview -> {
                BackHandler {
                    currentScreen = Screen.RepoDetail(screen.repo)
                }
                FileEditorScreen(
                    repo = screen.repo,
                    filePath = screen.filePath,
                    token = currentToken,
                    isEditable = screen.isOwnRepo,
                    onBack = {
                        currentScreen = Screen.RepoDetail(screen.repo)
                    },
                    userRepository = userRepository
                )
            }
            is Screen.ImagePreview -> {
                BackHandler {
                    currentScreen = Screen.RepoDetail(screen.repo)
                }
                ImagePreviewScreen(
                    repo = screen.repo,
                    imagePath = screen.imagePath,
                    token = currentToken,
                    onBack = {
                        currentScreen = Screen.RepoDetail(screen.repo)
                    }
                )
            }
            is Screen.DeployDetail -> {
                BackHandler {
                    selectedTab = 1
                    currentScreen = Screen.Main
                }
                DeployDetailScreen(
                    repo = screen.repo,
                    token = currentToken,
                    onBack = {
                        selectedTab = 1
                        currentScreen = Screen.Main
                    },
                    userRepository = userRepository
                )
            }
            is Screen.Search -> {
                BackHandler {
                    currentScreen = Screen.Main
                }
                SearchScreen(
                    modifier = Modifier.statusBarsPadding(),
                    token = currentToken,
                    userRepository = userRepository,
                    onBack = {
                        currentScreen = Screen.Main
                    },
                    onRepoClick = { repo ->
                        isFromSearch = true
                        currentScreen = Screen.RepoDetail(repo)
                    },
                    onUserClick = { username ->
                        currentScreen = Screen.UserProfile(username)
                    }
                )
            }
            is Screen.Notifications -> {
                val context = LocalContext.current
                BackHandler {
                    currentScreen = Screen.Main
                }
                NotificationsScreen(
                    modifier = Modifier.statusBarsPadding(),
                    token = currentToken,
                    userRepository = userRepository,
                    onBack = {
                        currentScreen = Screen.Main
                    },
                    onUnreadCountChange = { count ->
                        unreadNotificationCount = count
                    },
                    onNavigateToUrl = { url ->
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }
                )
            }
            is Screen.UserProfile -> {
                BackHandler {
                    currentScreen = Screen.Main
                }
                UserProfileScreen(
                    modifier = Modifier.statusBarsPadding(),
                    username = screen.username,
                    token = currentToken,
                    userRepository = userRepository,
                    onBack = {
                        currentScreen = Screen.Main
                    },
                    onRepoClick = { repo ->
                        currentScreen = Screen.RepoDetail(repo)
                    }
                )
            }
            is Screen.Settings -> {
                val activity = LocalContext.current as? android.app.Activity
                BackHandler {
                    currentScreen = Screen.Main
                }
                SettingsScreen(
                    themeMode = themeMode,
                    isDarkTheme = isDarkTheme,
                    onThemeModeChange = onThemeModeChange,
                    authViewModel = authViewModel,
                    userViewModel = userViewModel,
                    cacheDir = cacheDir,
                    onBack = {
                        currentScreen = Screen.Main
                    },
                    onNavigateToAbout = {
                        currentScreen = Screen.About
                    },
                    onNavigateToAccounts = {
                        currentScreen = Screen.Accounts
                    },
                    onLanguageChange = {
                        // 用 Intent 重启 Activity，避免 recreate() 导致的状态栏/导航栏残留
                        activity?.let { act ->
                            act.finish()
                            val intent = android.content.Intent(act, MainActivity::class.java)
                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            act.startActivity(intent)
                        }
                    }
                )
            }
            is Screen.Accounts -> {
                val activity = LocalContext.current as? android.app.Activity
                val context = LocalContext.current
                BackHandler {
                    currentScreen = Screen.Settings
                }
                AccountsScreen(
                    accountsManager = com.kun.github.data.local.preferences.AccountsManager(context),
                    onBack = {
                        currentScreen = Screen.Settings
                    },
                    onAddAccount = {
                        onOAuthLogin?.invoke()
                    },
                    onSwitchAccount = { account ->
                        // 用新 token 初始化 API Client
                        com.kun.github.data.remote.client.GithubApiClient.refreshClient(
                            account.token,
                            context.cacheDir
                        )
                        // 清除缓存，强制重新加载
                        userViewModel.resetState()
                        userViewModel.invalidateCache()
                        // 回到首页
                        currentScreen = Screen.Main
                    }
                )
            }
            is Screen.About -> {
                BackHandler {
                    currentScreen = Screen.Settings
                }
                AboutScreen(
                    onBack = {
                        currentScreen = Screen.Settings
                    },
                    onNavigateToTerms = { title, assetFileName ->
                        currentScreen = Screen.WebViewPage(title, assetFileName)
                    }
                )
            }
            is Screen.WebViewPage -> {
                BackHandler {
                    currentScreen = Screen.About
                }
                TermsWebViewScreen(
                    title = screen.title,
                    assetFileName = screen.assetFileName,
                    onBack = {
                        currentScreen = Screen.About
                    }
                )
            }
        }
    }
}
