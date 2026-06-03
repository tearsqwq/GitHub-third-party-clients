package com.kun.github.ui.screens.detail

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.ForkLeft
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.kun.github.data.model.GithubRepo
import com.kun.github.data.model.RepoContent
import com.kun.github.data.repository.user.UserRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarRepoDetailScreen(
    repo: GithubRepo,
    token: String,
    onBack: () -> Unit,
    userRepository: UserRepository,
    onPreviewFile: (String) -> Unit = {},
    onPreviewImage: (String) -> Unit = {},
    onOwnerClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var contents by remember { mutableStateOf<List<RepoContent>?>(null) }
    var readmeHtml by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isReadmeLoading by remember { mutableStateOf(false) }
    var currentPath by remember { mutableStateOf("") }
    var pathHistory by rememberSaveable { mutableStateOf(listOf<String>()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()
    var isStarred by remember { mutableStateOf(false) }
    var starCount by remember { mutableStateOf(repo.stars) }

    // 检查星标状态
    LaunchedEffect(repo) {
        userRepository.checkIfStarred(token, repo.owner.login, repo.name).fold(
            onSuccess = { isStarred = it },
            onFailure = {}
        )
    }

    fun toggleStar() {
        scope.launch {
            val result = if (isStarred) {
                userRepository.unstarRepo(token, repo.owner.login, repo.name)
            } else {
                userRepository.starRepo(token, repo.owner.login, repo.name)
            }
            result.fold(
                onSuccess = {
                    isStarred = !isStarred
                    starCount = if (isStarred) starCount + 1 else starCount - 1
                    android.widget.Toast.makeText(
                        context,
                        if (isStarred) "已添加星标" else "已取消星标",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                },
                onFailure = {}
            )
        }
    }

    fun loadContents(path: String) {
        scope.launch {
            isLoading = true
            errorMessage = null
            val result = userRepository.getRepoContents(token, repo.owner.login, repo.name, path)
            result.fold(
                onSuccess = {
                    contents = it
                    isLoading = false
                },
                onFailure = { e ->
                    errorMessage = e.message
                    isLoading = false
                }
            )
        }
    }

    fun loadReadme() {
        scope.launch {
            isReadmeLoading = true
            val result = userRepository.getReadme(token, repo.owner.login, repo.name)
            result.fold(
                onSuccess = { html ->
                    readmeHtml = html
                    isReadmeLoading = false
                },
                onFailure = {
                    readmeHtml = null
                    isReadmeLoading = false
                }
            )
        }
    }

    LaunchedEffect(repo) {
        loadContents("")
        loadReadme()
    }

    fun refresh() {
        isRefreshing = true
        loadContents(currentPath)
        loadReadme()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = if (currentPath.isEmpty()) repo.name else currentPath.substringAfterLast("/"),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = {
                    if (currentPath.isNotEmpty() && pathHistory.isNotEmpty()) {
                        currentPath = pathHistory.last()
                        pathHistory = pathHistory.dropLast(1)
                        loadContents(currentPath)
                    } else {
                        onBack()
                    }
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "返回",
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            actions = {
                IconButton(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(repo.htmlUrl))
                    context.startActivity(intent)
                }) {
                    Icon(
                        imageVector = Icons.Outlined.Link,
                        contentDescription = "在浏览器中打开",
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { refresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    // 仓库信息卡片
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // 所有者行
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { onOwnerClick(repo.owner.login) }
                            ) {
                                AsyncImage(
                                    model = repo.owner.avatarUrl,
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    repo.owner.login,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            repo.description?.let {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    it,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 20.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 统计行
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                StarChip(
                                    isStarred = isStarred,
                                    count = starCount,
                                    onClick = { toggleStar() }
                                )
                                StatChip(
                                    icon = Icons.Outlined.ForkLeft,
                                    label = "Fork",
                                    count = repo.forks
                                )
                            }
                        }
                    }

                    // 面包屑导航
                    if (currentPath.isNotEmpty()) {
                        Breadcrumb(
                            currentPath = currentPath,
                            onRootClick = {
                                currentPath = ""
                                pathHistory = emptyList()
                                loadContents("")
                            },
                            onPathClick = { index ->
                                val paths = currentPath.split("/").filter { it.isNotEmpty() }
                                val newPath = paths.take(index + 1).joinToString("/")
                                currentPath = newPath
                                pathHistory = pathHistory.dropLast(paths.size - index - 1)
                                loadContents(newPath)
                            }
                        )
                    }

                    // 错误信息
                    errorMessage?.let { message ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    // 文件列表
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        contents?.let { items ->
                            if (items.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "空文件夹",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                items.sortedWith(compareBy({ it.type != "dir" }, { it.name }))
                                    .forEach { item ->
                                        FileListItem(
                                            item = item,
                                            onClick = {
                                                if (item.type == "dir") {
                                                    pathHistory = pathHistory + currentPath
                                                    currentPath = if (currentPath.isEmpty()) {
                                                        item.name
                                                    } else {
                                                        "$currentPath/${item.name}"
                                                    }
                                                    loadContents(currentPath)
                                                } else {
                                                    when {
                                                        item.name.endsWith(".jpg", true) ||
                                                        item.name.endsWith(".jpeg", true) ||
                                                        item.name.endsWith(".png", true) ||
                                                        item.name.endsWith(".gif", true) ||
                                                        item.name.endsWith(".webp", true) -> {
                                                            onPreviewImage(item.path)
                                                        }
                                                        else -> {
                                                            onPreviewFile(item.path)
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                    }
                            }
                        }
                    }

                    // README
                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isReadmeLoading || readmeHtml != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "README",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        when {
                            isReadmeLoading -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            readmeHtml != null -> {
                                val currentReadmeHtml = readmeHtml
                                if (currentReadmeHtml != null) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                    ) {
                                        AndroidView(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 100.dp)
                                                .padding(12.dp),
                                            factory = { ctx ->
                                                WebView(ctx).apply {
                                                    webViewClient = WebViewClient()
                                                    settings.apply {
                                                        javaScriptEnabled = false
                                                        loadWithOverviewMode = true
                                                        useWideViewPort = true
                                                    }
                                                    val styledHtml = wrapReadmeHtml(currentReadmeHtml, isDark)
                                                    loadDataWithBaseURL(
                                                        "https://github.com",
                                                        styledHtml,
                                                        "text/html",
                                                        "UTF-8",
                                                        null
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }
    }
}

// StarRepoDetailScreen 专用的 FileListItem（无长按功能）
@Composable
private fun FileListItem(
    item: RepoContent,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (item.type == "dir") Icons.Outlined.Folder else Icons.AutoMirrored.Outlined.InsertDriveFile,
            contentDescription = null,
            tint = if (item.type == "dir") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = item.name,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
