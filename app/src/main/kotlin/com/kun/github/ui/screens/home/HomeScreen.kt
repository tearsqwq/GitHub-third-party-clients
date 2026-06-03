package com.kun.github.ui.screens.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ForkLeft
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kun.github.data.model.GithubRepo
import com.kun.github.data.remote.client.GithubApiClient
import com.kun.github.data.repository.user.UserRepository
import com.kun.github.presentation.components.CreateRepoDialog
import com.kun.github.presentation.components.EmptyReposState
import com.kun.github.presentation.components.NetworkErrorState
import com.kun.github.presentation.components.RepoItem
import com.kun.github.presentation.components.RepoListSkeleton
import com.kun.github.presentation.components.RepoSortDropdown
import com.kun.github.presentation.components.RepoSortOption
import com.kun.github.presentation.components.sortRepos
import com.kun.github.presentation.user.CreateRepoState
import com.kun.github.presentation.user.ReposState
import com.kun.github.presentation.user.UserViewModel
import com.kun.github.presentation.settings.strings
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    token: String,
    viewModel: UserViewModel,
    userRepository: UserRepository,
    onRepoClick: (GithubRepo) -> Unit,
    onSearchClick: () -> Unit = {},
    onOwnerClick: (String) -> Unit = {},
    cacheDir: File? = null
) {
    val reposState by viewModel.reposState.collectAsState()
    val trendingState by viewModel.trendingState.collectAsState()
    val createRepoState by viewModel.createRepoState.collectAsState()
    val s = strings()
    var showDialog by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 星标状态缓存
    val starredCache = remember { mutableStateOf(setOf<String>()) }

    // 检查仓库星标状态
    fun checkStarredStatus(repos: List<GithubRepo>) {
        scope.launch {
            repos.forEach { repo ->
                launch {
                    val key = "${repo.owner.login}/${repo.name}"
                    userRepository.checkIfStarred(token, repo.owner.login, repo.name).fold(
                        onSuccess = { isStarred ->
                            if (isStarred) {
                                starredCache.value = starredCache.value + key
                            }
                        },
                        onFailure = {}
                    )
                }
            }
        }
    }

    fun toggleStar(repo: GithubRepo) {
        val key = "${repo.owner.login}/${repo.name}"
        scope.launch {
            val isStarred = starredCache.value.contains(key)
            val result = if (isStarred) {
                userRepository.unstarRepo(token, repo.owner.login, repo.name)
            } else {
                userRepository.starRepo(token, repo.owner.login, repo.name)
            }
            result.fold(
                onSuccess = {
                    starredCache.value = if (isStarred) {
                        starredCache.value - key
                    } else {
                        starredCache.value + key
                    }
                    android.widget.Toast.makeText(
                        context,
                        if (isStarred) "已取消星标" else "已添加星标",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                },
                onFailure = {}
            )
        }
    }

    // 入场动画：标题淡入
    var titleVisible by remember { mutableStateOf(false) }
    val titleAlpha by animateFloatAsState(
        targetValue = if (titleVisible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMediumLow),
        label = "titleAlpha"
    )

    // 入场动画：列表淡入（延迟）
    var listVisible by remember { mutableStateOf(false) }
    val listAlpha by animateFloatAsState(
        targetValue = if (listVisible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMediumLow),
        label = "listAlpha"
    )

    // Context menu state for long press
    var contextMenuRepo by remember { mutableStateOf<GithubRepo?>(null) }

    // 当仓库数据加载完成时，结束刷新动画
    LaunchedEffect(reposState) {
        if (reposState is ReposState.Success || reposState is ReposState.Error) {
            isRefreshing = false
        }
    }

    // 当切换到热门仓库 tab 时加载数据
    LaunchedEffect(selectedTab) {
        if (selectedTab == 1) {
            viewModel.loadTrendingRepos(token)
        }
    }

    // SharedPreferences for pinned repos
    val prefs = remember {
        context.getSharedPreferences("github_prefs", Context.MODE_PRIVATE)
    }

    fun getPinnedRepoIds(): Set<String> {
        return prefs.getStringSet("pinned_repo_ids", emptySet()) ?: emptySet()
    }

    fun togglePinRepo(repoId: Long) {
        val pinnedIds = getPinnedRepoIds().toMutableSet()
        val idStr = repoId.toString()
        if (pinnedIds.contains(idStr)) {
            pinnedIds.remove(idStr)
        } else {
            pinnedIds.add(idStr)
        }
        prefs.edit().putStringSet("pinned_repo_ids", pinnedIds).apply()
    }

    fun isPinned(repoId: Long): Boolean {
        return getPinnedRepoIds().contains(repoId.toString())
    }

    // Branches/Tags dialog state
    var showBranchesDialog by remember { mutableStateOf(false) }
    var showTagsDialog by remember { mutableStateOf(false) }
    var branchesList by remember { mutableStateOf<List<String>>(emptyList()) }
    var tagsList by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoadingBranches by remember { mutableStateOf(false) }
    var isLoadingTags by remember { mutableStateOf(false) }
    var dialogRepoName by remember { mutableStateOf("") }
    var dialogRepoOwner by remember { mutableStateOf("") }

    // Delete repo dialog state
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteRepoItem by remember { mutableStateOf<GithubRepo?>(null) }
    var isDeletingRepo by remember { mutableStateOf(false) }

    // Repo settings dialog state
    var showRepoSettingsDialog by remember { mutableStateOf(false) }
    var repoSettingsItem by remember { mutableStateOf<GithubRepo?>(null) }
    var repoSettingsName by remember { mutableStateOf("") }
    var repoSettingsDesc by remember { mutableStateOf("") }
    var repoSettingsPrivate by remember { mutableStateOf(false) }
    var repoSettingsHomepage by remember { mutableStateOf("") }
    var repoSettingsHasIssues by remember { mutableStateOf(true) }
    var repoSettingsHasWiki by remember { mutableStateOf(true) }
    var repoSettingsHasDiscussions by remember { mutableStateOf(false) }
    var isUpdatingRepo by remember { mutableStateOf(false) }
    
    // 排序状态
    var currentSort by remember { mutableStateOf(RepoSortOption.UPDATED) }

    LaunchedEffect(Unit) {
        viewModel.loadRepos(token)
        titleVisible = true
    }

    LaunchedEffect(titleAlpha) {
        if (titleAlpha > 0.5f) {
            listVisible = true
        }
    }

    LaunchedEffect(createRepoState) {
        if (createRepoState is CreateRepoState.Success) {
            showDialog = false
            viewModel.resetCreateRepoState()
            // 清除缓存后重新加载，确保能看到新创建的仓库
            GithubApiClient.refreshClient(token, cacheDir)
            viewModel.loadRepos(token)
            Toast.makeText(context, s.toastRepoCreated, Toast.LENGTH_SHORT).show()
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            if (selectedTab == 0) {
                viewModel.loadRepos(token)
            } else {
                viewModel.loadTrendingRepos(token)
            }
        }
    ) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 标题行 + Tab 切换
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .alpha(titleAlpha),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                s.homeTitle,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            // 紧凑 Tab 切换 - 带动画
            val tabIndex by animateFloatAsState(
                targetValue = selectedTab.toFloat(),
                animationSpec = spring(
                    dampingRatio = 0.7f,
                    stiffness = 300f
                ),
                label = "tabIndex"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(2.dp)
            ) {
                // 动画背景指示器
                Box(
                    modifier = Modifier
                        .width(46.dp)
                        .height(28.dp)
                        .offset(x = (tabIndex * 50).dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 我的仓库
                    Box(
                        modifier = Modifier
                            .width(46.dp)
                            .height(28.dp)
                            .clickable {
                                if (selectedTab != 0) {
                                    selectedTab = 0
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "我的",
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Medium else FontWeight.Normal,
                            color = if (selectedTab == 0) MaterialTheme.colorScheme.onPrimary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // 热门
                    Box(
                        modifier = Modifier
                            .width(46.dp)
                            .height(28.dp)
                            .clickable {
                                if (selectedTab != 1) {
                                    selectedTab = 1
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "热门",
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Medium else FontWeight.Normal,
                            color = if (selectedTab == 1) MaterialTheme.colorScheme.onPrimary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 操作按钮行（仅我的仓库显示）
        if (selectedTab == 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 搜索按钮
                IconButton(onClick = onSearchClick) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "搜索",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
                // 排序下拉菜单
                RepoSortDropdown(
                    currentSort = currentSort,
                    onSortSelected = { currentSort = it }
                )
                Spacer(modifier = Modifier.weight(1f))
                // 新建仓库
                IconButton(onClick = { showDialog = true }) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "新建仓库",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // 根据 Tab 显示不同内容 - 带动画
        // 使用 weight(1f) 确保 AnimatedContent 占据剩余空间，避免 LazyColumn 无限高度约束
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                if (targetState > initialState) {
                    // 从右滑入
                    (slideInHorizontally { width -> width } + fadeIn(animationSpec = tween(200)))
                        .togetherWith(slideOutHorizontally { width -> -width } + fadeOut(animationSpec = tween(200)))
                } else {
                    // 从左滑入
                    (slideInHorizontally { width -> -width } + fadeIn(animationSpec = tween(200)))
                        .togetherWith(slideOutHorizontally { width -> width } + fadeOut(animationSpec = tween(200)))
                }
            },
            modifier = Modifier.weight(1f),
            label = "tabContent"
        ) { tab ->
            when (tab) {
                0 -> {
                    // 我的仓库
                    when (val state = reposState) {
                        is ReposState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp)
                            ) {
                                RepoListSkeleton(count = 5)
                            }
                        }
                        is ReposState.Error -> {
                            NetworkErrorState(
                                onRetryClick = { viewModel.loadRepos(token) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        is ReposState.Success -> {
                            val sortedRepos = remember(state.repos, currentSort) {
                                val pinnedIds = getPinnedRepoIds()
                                sortRepos(state.repos, currentSort, pinnedIds)
                            }

                            LaunchedEffect(state.repos) {
                                checkStarredStatus(state.repos)
                            }

                            if (sortedRepos.isEmpty()) {
                                EmptyReposState(
                                    onCreateClick = { showDialog = true },
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(bottom = 100.dp),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 24.dp)
                                        .alpha(listAlpha)
                                ) {
                                    items(sortedRepos, key = { it.id }) { repo ->
                                        val key = "${repo.owner.login}/${repo.name}"
                                        Box {
                                            RepoItem(
                                                repo = repo,
                                                onClick = { onRepoClick(repo) },
                                                onLongClick = { contextMenuRepo = repo },
                                                isStarred = starredCache.value.contains(key),
                                                onStarClick = { toggleStar(repo) },
                                                onOwnerClick = { onOwnerClick(repo.owner.login) },
                                                modifier = Modifier.animateItem()
                                            )
                                            CustomContextMenu(
                                                expanded = contextMenuRepo?.id == repo.id,
                                                onDismiss = { contextMenuRepo = null },
                                                offset = DpOffset(x = (-8).dp, y = 0.dp)
                                            ) {
                                                ContextMenuItem(
                                                    icon = Icons.Outlined.PushPin,
                                                    label = if (isPinned(repo.id)) s.repoUnpin else s.repoPin,
                                                    onClick = {
                                                        togglePinRepo(repo.id)
                                                        contextMenuRepo = null
                                                        viewModel.loadRepos(token)
                                                    }
                                                )
                                                ContextMenuItem(
                                                    icon = Icons.Outlined.Settings,
                                                    label = s.repoSettings,
                                                    onClick = {
                                                        repoSettingsItem = repo
                                                        repoSettingsName = repo.name
                                                        repoSettingsDesc = repo.description ?: ""
                                                        repoSettingsPrivate = repo.private
                                                        repoSettingsHomepage = repo.htmlUrl
                                                        repoSettingsHasIssues = true
                                                        repoSettingsHasWiki = true
                                                        repoSettingsHasDiscussions = false
                                                        showRepoSettingsDialog = true
                                                        contextMenuRepo = null
                                                    }
                                                )
                                                ContextMenuItem(
                                                    icon = Icons.Outlined.AccountTree,
                                                    label = s.repoBranches,
                                                    onClick = {
                                                        dialogRepoOwner = repo.owner.login
                                                        dialogRepoName = repo.name
                                                        isLoadingBranches = true
                                                        showBranchesDialog = true
                                                        contextMenuRepo = null
                                                        scope.launch {
                                                            val result = userRepository.getBranches(token, repo.owner.login, repo.name)
                                                            result.fold(
                                                                onSuccess = { branchesList = it },
                                                                onFailure = {
                                                                    Toast.makeText(context, s.toastNoBranches, Toast.LENGTH_SHORT).show()
                                                                }
                                                            )
                                                            isLoadingBranches = false
                                                        }
                                                    }
                                                )
                                                ContextMenuItem(
                                                    icon = Icons.Outlined.Tag,
                                                    label = s.repoTags,
                                                    onClick = {
                                                        dialogRepoOwner = repo.owner.login
                                                        dialogRepoName = repo.name
                                                        isLoadingTags = true
                                                        showTagsDialog = true
                                                        contextMenuRepo = null
                                                        scope.launch {
                                                            val result = userRepository.getTags(token, repo.owner.login, repo.name)
                                                            result.fold(
                                                                onSuccess = { tagsList = it },
                                                                onFailure = {
                                                                    Toast.makeText(context, s.toastNoTags, Toast.LENGTH_SHORT).show()
                                                                }
                                                            )
                                                            isLoadingTags = false
                                                        }
                                                    }
                                                )
                                                ContextMenuItem(
                                                    icon = Icons.Outlined.ContentCopy,
                                                    label = s.repoCopyLink,
                                                    onClick = {
                                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                        val clip = ClipData.newPlainText("repo_url", repo.htmlUrl)
                                                        clipboard.setPrimaryClip(clip)
                                                        Toast.makeText(context, s.toastLinkCopied, Toast.LENGTH_SHORT).show()
                                                        contextMenuRepo = null
                                                    }
                                                )
                                                ContextMenuItem(
                                                    icon = Icons.Outlined.Download,
                                                    label = s.repoDownloadZip,
                                                    onClick = {
                                                        val downloadUrl = userRepository.getZipDownloadUrl(repo.owner.login, repo.name)
                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                                                        context.startActivity(intent)
                                                        contextMenuRepo = null
                                                    }
                                                )
                                                ContextMenuItem(
                                                    icon = Icons.Outlined.Delete,
                                                    label = s.repoDelete,
                                                    onClick = {
                                                        deleteRepoItem = repo
                                                        showDeleteDialog = true
                                                        contextMenuRepo = null
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        else -> {}
                    }
                }
                1 -> {
                    // 热门仓库
                    when (val state = trendingState) {
                        is ReposState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp)
                            ) {
                                RepoListSkeleton(count = 5)
                            }
                        }
                        is ReposState.Error -> {
                            NetworkErrorState(
                                onRetryClick = { viewModel.loadTrendingRepos(token) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        is ReposState.Success -> {
                            LaunchedEffect(state.repos) {
                                checkStarredStatus(state.repos)
                            }

                            if (state.repos.isEmpty()) {
                                EmptyReposState(
                                    onCreateClick = { },
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(bottom = 100.dp),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 24.dp)
                                        .alpha(listAlpha)
                                ) {
                                    items(state.repos, key = { it.id }) { repo ->
                                        val key = "${repo.owner.login}/${repo.name}"
                                        RepoItem(
                                            repo = repo,
                                            onClick = { onRepoClick(repo) },
                                            isStarred = starredCache.value.contains(key),
                                            onStarClick = { toggleStar(repo) },
                                            onOwnerClick = { onOwnerClick(repo.owner.login) },
                                            modifier = Modifier.animateItem()
                                        )
                                    }
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
    }

    if (showDialog) {
        CreateRepoDialog(
            isLoading = createRepoState is CreateRepoState.Loading,
            errorMessage = (createRepoState as? CreateRepoState.Error)?.message,
            onDismiss = {
                showDialog = false
                viewModel.resetCreateRepoState()
            },
            onCreate = { name, description ->
                viewModel.createRepo(token, name, description)
            }
        )
    }

    // Branches Dialog
    if (showBranchesDialog) {
        AlertDialog(
            onDismissRequest = { showBranchesDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "${s.repoBranches} - $dialogRepoName",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                if (isLoadingBranches) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (branchesList.isEmpty()) {
                    Text(s.toastNoBranches, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Column {
                        branchesList.forEach { branch ->
                            TextButton(
                                onClick = {
                                    Toast.makeText(context, "当前分支: $branch", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(branch, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBranchesDialog = false }) {
                    Text(s.close, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }

    // Tags Dialog
    if (showTagsDialog) {
        AlertDialog(
            onDismissRequest = { showTagsDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "${s.repoTags} - $dialogRepoName",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                if (isLoadingTags) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (tagsList.isEmpty()) {
                    Text(s.toastNoTags, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Column {
                        tagsList.forEach { tag ->
                            TextButton(
                                onClick = {
                                    Toast.makeText(context, "当前标签: $tag", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(tag, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTagsDialog = false }) {
                    Text(s.close, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }

    // Delete Repo Confirmation Dialog
    if (showDeleteDialog && deleteRepoItem != null) {
        val repoToDelete = deleteRepoItem!!
        AlertDialog(
            onDismissRequest = { if (!isDeletingRepo) showDeleteDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    s.deleteRepoTitle,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    String.format(s.deleteRepoMessage, repoToDelete.fullName),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDeletingRepo = true
                        scope.launch {
                            val result = userRepository.deleteRepo(token, repoToDelete.owner.login, repoToDelete.name)
                            isDeletingRepo = false
                            result.fold(
                                onSuccess = {
                                    Toast.makeText(context, s.toastRepoDeleted, Toast.LENGTH_SHORT).show()
                                    showDeleteDialog = false
                                    deleteRepoItem = null
                                    GithubApiClient.refreshClient(token, cacheDir)
                                    viewModel.invalidateCache()
                                    viewModel.loadRepos(token)
                                },
                                onFailure = {
                                    Toast.makeText(context, "删除失败: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    },
                    enabled = !isDeletingRepo
                ) {
                    Text(s.delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    enabled = !isDeletingRepo
                ) {
                    Text(s.cancel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }

    // Repo Settings Dialog
    if (showRepoSettingsDialog && repoSettingsItem != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { if (!isUpdatingRepo) showRepoSettingsDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            androidx.compose.material3.Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    // 标题
                    Text(
                        s.repoSettingsTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // ====== 基本信息 ======
                    Text(
                        s.repoSettingsBasic,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )

                    // 仓库名称
                    OutlinedTextField(
                        value = repoSettingsName,
                        onValueChange = { repoSettingsName = it },
                        label = { Text(s.repoSettingsName, fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isUpdatingRepo,
                        shape = RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // 仓库描述
                    OutlinedTextField(
                        value = repoSettingsDesc,
                        onValueChange = { repoSettingsDesc = it },
                        label = { Text(s.repoSettingsDesc, fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isUpdatingRepo,
                        shape = RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // 主页 URL
                    OutlinedTextField(
                        value = repoSettingsHomepage,
                        onValueChange = { repoSettingsHomepage = it },
                        label = { Text(s.repoSettingsHomepage, fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isUpdatingRepo,
                        shape = RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // ====== 功能开关 ======
                    Text(
                        s.repoSettingsFeatures,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )

                    // 私有仓库
                    SettingsSwitchRow(
                        label = s.repoSettingsPrivate,
                        checked = repoSettingsPrivate,
                        onCheckedChange = { repoSettingsPrivate = it },
                        enabled = !isUpdatingRepo
                    )
                    // Issues
                    SettingsSwitchRow(
                        label = s.repoSettingsIssues,
                        checked = repoSettingsHasIssues,
                        onCheckedChange = { repoSettingsHasIssues = it },
                        enabled = !isUpdatingRepo
                    )
                    // Wiki
                    SettingsSwitchRow(
                        label = s.repoSettingsWiki,
                        checked = repoSettingsHasWiki,
                        onCheckedChange = { repoSettingsHasWiki = it },
                        enabled = !isUpdatingRepo
                    )
                    // Discussions
                    SettingsSwitchRow(
                        label = s.repoSettingsDiscussions,
                        checked = repoSettingsHasDiscussions,
                        onCheckedChange = { repoSettingsHasDiscussions = it },
                        enabled = !isUpdatingRepo
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 按钮行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showRepoSettingsDialog = false },
                            enabled = !isUpdatingRepo
                        ) {
                            Text(s.cancel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                isUpdatingRepo = true
                                val item = repoSettingsItem!!
                                val owner = item.fullName.substringBefore("/")
                                val request = com.kun.github.data.remote.dto.UpdateRepoRequest(
                                    name = repoSettingsName,
                                    description = repoSettingsDesc.ifBlank { null },
                                    private = repoSettingsPrivate,
                                    homepage = repoSettingsHomepage.ifBlank { null },
                                    hasIssues = repoSettingsHasIssues,
                                    hasWiki = repoSettingsHasWiki,
                                    hasDiscussions = repoSettingsHasDiscussions
                                )
                                scope.launch {
                                    val result = userRepository.updateRepo(token, owner, item.name, request)
                                    isUpdatingRepo = false
                                    if (result.isSuccess) {
                                        showRepoSettingsDialog = false
                                        viewModel.loadRepos(token)
                                        Toast.makeText(context, s.toastRepoUpdated, Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "保存失败: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = !isUpdatingRepo && repoSettingsName.isNotBlank()
                        ) {
                            if (isUpdatingRepo) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(s.save, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 15.sp,
            color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.weight(1f)
        )
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun CustomContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset.Zero,
    content: @Composable ColumnScope.() -> Unit
) {
    androidx.compose.material3.DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier,
        offset = offset,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 0.dp,
        border = null,
        content = content
    )
}

@Composable
private fun ColumnScope.ContextMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    androidx.compose.material3.DropdownMenuItem(
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        onClick = onClick,
        contentPadding = PaddingValues(
            start = 14.dp, end = 14.dp, top = 9.dp, bottom = 9.dp
        ),
        modifier = Modifier.height(36.dp)
    )
}
