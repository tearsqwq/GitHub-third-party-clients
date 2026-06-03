package com.kun.github.ui.screens.notifications

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import coil.compose.AsyncImage
import com.kun.github.data.model.GithubNotification
import com.kun.github.data.repository.user.UserRepository
import com.kun.github.presentation.settings.strings
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

@Composable
fun NotificationsScreen(
    modifier: Modifier = Modifier,
    token: String,
    userRepository: UserRepository,
    onBack: () -> Unit,
    onUnreadCountChange: (Int) -> Unit = {},
    onNavigateToUrl: (String) -> Unit = {}
) {
    val s = strings()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var notifications by remember { mutableStateOf<List<GithubNotification>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isMarkingAllRead by remember { mutableStateOf(false) }

    // 分页
    var currentPage by remember { mutableIntStateOf(1) }
    var hasMore by remember { mutableStateOf(true) }
    val perPage = 30

    // Tab 切换：0=未读, 1=全部
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabIndex by animateFloatAsState(
        targetValue = selectedTab.toFloat(),
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "tabIndex"
    )

    fun loadNotifications(refresh: Boolean = false) {
        if (refresh) {
            currentPage = 1
            hasMore = true
        }
        if (currentPage == 1) {
            isLoading = true
        }
        errorMessage = null

        scope.launch {
            val all = selectedTab == 1
            userRepository.getNotifications(token, all, currentPage, perPage).fold(
                onSuccess = { list ->
                    if (refresh || currentPage == 1) {
                        notifications = list
                    } else {
                        notifications = notifications + list
                    }
                    hasMore = list.size >= perPage
                    val unreadCount = if (selectedTab == 0) list.size else notifications.count { it.unread }
                    onUnreadCountChange(unreadCount)
                },
                onFailure = { error ->
                    errorMessage = error.message
                }
            )
            isLoading = false
            isRefreshing = false
            isLoadingMore = false
        }
    }

    fun loadMore() {
        if (!isLoadingMore && hasMore && !isLoading) {
            isLoadingMore = true
            currentPage++
            loadNotifications()
        }
    }

    fun markAllAsRead() {
        scope.launch {
            isMarkingAllRead = true
            userRepository.markAllNotificationsAsRead(token).fold(
                onSuccess = {
                    notifications = notifications.map { it.copy(unread = false) }
                    onUnreadCountChange(0)
                    Toast.makeText(context, s.notificationsMarkedRead, Toast.LENGTH_SHORT).show()
                },
                onFailure = { error ->
                    Toast.makeText(context, error.message ?: "操作失败", Toast.LENGTH_SHORT).show()
                }
            )
            isMarkingAllRead = false
        }
    }

    // Tab 切换时重新加载
    LaunchedEffect(selectedTab) {
        loadNotifications(refresh = true)
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            loadNotifications(refresh = true)
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 顶部导航栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = s.notificationsTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // 全部已读按钮
                if (selectedTab == 0 && notifications.any { it.unread }) {
                    IconButton(
                        onClick = { markAllAsRead() },
                        enabled = !isMarkingAllRead
                    ) {
                        if (isMarkingAllRead) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.DoneAll,
                                contentDescription = "全部已读",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Tab 切换 - 带动画
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        // 未读
                        Box(
                            modifier = Modifier
                                .width(46.dp)
                                .height(28.dp)
                                .clickable {
                                    if (selectedTab != 0) selectedTab = 0
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "未读",
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Medium else FontWeight.Normal,
                                color = if (selectedTab == 0) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // 全部
                        Box(
                            modifier = Modifier
                                .width(46.dp)
                                .height(28.dp)
                                .clickable {
                                    if (selectedTab != 1) selectedTab = 1
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "全部",
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Medium else FontWeight.Normal,
                                color = if (selectedTab == 1) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // 数量统计
                Text(
                    text = "${notifications.size} 条",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )

            // 内容区域 - 带动画
            // 使用 weight(1f) 确保 AnimatedContent 占据剩余空间，避免内部 LazyColumn 无限高度约束
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width } + fadeIn(animationSpec = tween(200)))
                            .togetherWith(slideOutHorizontally { width -> -width } + fadeOut(animationSpec = tween(200)))
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn(animationSpec = tween(200)))
                            .togetherWith(slideOutHorizontally { width -> width } + fadeOut(animationSpec = tween(200)))
                    }
                },
                modifier = Modifier.weight(1f),
                label = "tabContent"
            ) { tab ->
                when (tab) {
                    0 -> NotificationListContent(
                        notifications = notifications,
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        isLoadingMore = isLoadingMore,
                        hasMore = hasMore,
                        onLoadMore = { loadMore() },
                        onRetry = { loadNotifications(refresh = true) },
                        onMarkRead = { notification ->
                            scope.launch {
                                userRepository.markNotificationAsRead(token, notification.id).fold(
                                    onSuccess = {
                                        notifications = notifications.map {
                                            if (it.id == notification.id) it.copy(unread = false) else it
                                        }
                                        val newUnreadCount = notifications.count { it.unread }
                                        onUnreadCountChange(newUnreadCount)
                                    },
                                    onFailure = {}
                                )
                            }
                        },
                        onNavigateToUrl = onNavigateToUrl,
                        emptyMessage = s.notificationsEmpty
                    )
                    1 -> NotificationListContent(
                        notifications = notifications,
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        isLoadingMore = isLoadingMore,
                        hasMore = hasMore,
                        onLoadMore = { loadMore() },
                        onRetry = { loadNotifications(refresh = true) },
                        onMarkRead = { notification ->
                            scope.launch {
                                userRepository.markNotificationAsRead(token, notification.id).fold(
                                    onSuccess = {
                                        notifications = notifications.map {
                                            if (it.id == notification.id) it.copy(unread = false) else it
                                        }
                                    },
                                    onFailure = {}
                                )
                            }
                        },
                        onNavigateToUrl = onNavigateToUrl,
                        emptyMessage = "暂无通知记录"
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationListContent(
    notifications: List<GithubNotification>,
    isLoading: Boolean,
    errorMessage: String?,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onMarkRead: (GithubNotification) -> Unit,
    onNavigateToUrl: (String) -> Unit,
    emptyMessage: String
) {
    when {
        isLoading && notifications.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        errorMessage != null && notifications.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "点击重试",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { onRetry() }
                    )
                }
            }
        }
        notifications.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = emptyMessage,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            }
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
            ) {
                items(notifications, key = { it.id }) { notification ->
                    NotificationItem(
                        notification = notification,
                        onClick = {
                            // 标记已读
                            onMarkRead(notification)
                            // 跳转到对应页面
                            val url = notification.subject.url?.replace("https://api.github.com/repos/", "https://github.com/")
                                ?.replace("/pulls/", "/pull/")
                                ?.replace("/issues/", "/issues/")
                            if (url != null) {
                                onNavigateToUrl(url)
                            }
                        }
                    )
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                }

                // 加载更多
                if (hasMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                                .clickable(enabled = !isLoadingMore) { onLoadMore() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoadingMore) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text(
                                    "加载更多",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(
    notification: GithubNotification,
    onClick: () -> Unit
) {
    val typeLabel = getNotificationTypeLabel(notification.subject.type)
    val relativeTime = formatRelativeTime(notification.updatedAt)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 仓库头像 - 从 fullName 解析 owner
        val ownerLogin = notification.repository.fullName.split("/").firstOrNull() ?: ""
        AsyncImage(
            model = "https://avatars.githubusercontent.com/$ownerLogin?size=88",
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            // 标题
            Text(
                text = notification.subject.title,
                fontSize = 14.sp,
                fontWeight = if (notification.unread) FontWeight.SemiBold else FontWeight.Normal,
                color = if (notification.unread) {
                    MaterialTheme.colorScheme.onBackground
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 仓库名 + 类型 + 时间
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = notification.repository.fullName,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = "·",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
                Text(
                    text = typeLabel,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = "·",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
                Text(
                    text = relativeTime,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        // 未读圆点
        if (notification.unread) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

private fun getNotificationTypeLabel(type: String): String {
    return when (type.lowercase()) {
        "issue" -> "Issue"
        "pullrequest" -> "PR"
        "commit" -> "Commit"
        "release" -> "Release"
        "discussion" -> "Discussion"
        else -> type
    }
}

private fun formatRelativeTime(isoTime: String): String {
    return try {
        val instant = Instant.parse(isoTime)
        val now = Instant.now()
        val duration = Duration.between(instant, now)
        val minutes = duration.toMinutes()
        val hours = duration.toHours()
        val days = duration.toDays()

        when {
            minutes < 1 -> "刚刚"
            minutes < 60 -> "${minutes}分钟前"
            hours < 24 -> "${hours}小时前"
            days < 7 -> "${days}天前"
            days < 30 -> "${days / 7}周前"
            days < 365 -> "${days / 30}个月前"
            else -> "${days / 365}年前"
        }
    } catch (_: Exception) {
        isoTime
    }
}
