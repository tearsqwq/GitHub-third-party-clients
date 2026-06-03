package com.kun.github.ui.screens.userprofile

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kun.github.R
import com.kun.github.data.model.GithubRepo
import com.kun.github.data.model.GithubUser
import com.kun.github.data.repository.user.UserRepository
import com.kun.github.presentation.components.RepoItem
import kotlinx.coroutines.launch

@Composable
fun UserProfileScreen(
    modifier: Modifier = Modifier,
    username: String,
    token: String,
    userRepository: UserRepository,
    onBack: () -> Unit,
    onRepoClick: (GithubRepo) -> Unit = {}
) {
    var user by remember { mutableStateOf<GithubUser?>(null) }
    var repos by remember { mutableStateOf<List<GithubRepo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // 入场动画
    var titleVisible by remember { mutableStateOf(false) }
    val titleAlpha by animateFloatAsState(
        targetValue = if (titleVisible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMediumLow),
        label = "titleAlpha"
    )

    LaunchedEffect(username) {
        isLoading = true
        errorMessage = null
        titleVisible = false

        // 并行加载用户信息和仓库
        launch {
            userRepository.getUser(token, username).fold(
                onSuccess = { user = it },
                onFailure = { errorMessage = it.message }
            )
        }
        launch {
            userRepository.getUserRepos(token, username).fold(
                onSuccess = { repos = it },
                onFailure = {}
            )
        }.invokeOnCompletion {
            isLoading = false
            titleVisible = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = modifier.fillMaxSize()
        ) {
            // 顶部导航栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_left),
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = username,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage ?: "加载失败",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(
                        bottom = 100.dp,
                        top = 8.dp
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                        .alpha(titleAlpha)
                ) {
                    // 用户信息卡片
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // 头像
                                AsyncImage(
                                    model = user?.avatarUrl,
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // 用户名
                                Text(
                                    text = user?.name ?: username,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                // @用户名
                                Text(
                                    text = "@${user?.login ?: username}",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )

                                // bio
                                val bio = user?.bio
                                if (!bio.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = bio,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 18.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // 统计信息
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    StatItem(
                                        value = repos.size.toString(),
                                        label = "仓库"
                                    )
                                    StatItem(
                                        value = formatCount(user?.followers ?: 0),
                                        label = "粉丝"
                                    )
                                    StatItem(
                                        value = formatCount(user?.following ?: 0),
                                        label = "关注"
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 仓库标题
                        Text(
                            text = "仓库",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // 仓库列表
                    items(repos, key = { it.id }) { repo ->
                        RepoItem(
                            repo = repo,
                            onClick = { onRepoClick(repo) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

/**
 * 格式化数字：超过1000显示 x.xk
 */
private fun formatCount(count: Int): String {
    return if (count >= 1000) {
        val value = count / 1000.0
        if (value == value.toLong().toDouble()) {
            "${value.toLong()}k"
        } else {
            String.format("%.1fk", value)
        }
    } else {
        count.toString()
    }
}
