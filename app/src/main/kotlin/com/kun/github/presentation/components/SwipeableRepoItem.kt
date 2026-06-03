package com.kun.github.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kun.github.data.model.GithubRepo
import kotlinx.coroutines.delay

/**
 * 可滑动的仓库列表项
 * 左滑：置顶/取消置顶
 * 右滑：删除
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableRepoItem(
    repo: GithubRepo,
    isPinned: Boolean,
    onPin: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isRemoved by remember { mutableStateOf(false) }
    
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    // 右滑 - 删除
                    isRemoved = true
                    true
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    // 左滑 - 置顶
                    onPin()
                    false // 不删除，只是触发置顶
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )
    
    // 处理删除动画完成后的回调
    LaunchedEffect(isRemoved) {
        if (isRemoved) {
            delay(300) // 等待动画完成
            onDelete()
        }
    }
    
    AnimatedVisibility(
        visible = !isRemoved,
        exit = shrinkVertically(
            animationSpec = tween(durationMillis = 300),
            shrinkTowards = Alignment.Top
        ) + fadeOut()
    ) {
        SwipeToDismissBox(
            state = dismissState,
            modifier = modifier,
            backgroundContent = {
                val direction = dismissState.dismissDirection
                val scale by animateFloatAsState(
                    targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) 0.8f else 1.2f,
                    label = "scale"
                )
                
                when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> {
                        // 右滑背景 - 删除（红色）
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFE53935))
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "删除",
                                tint = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.scale(scale)
                            )
                        }
                    }
                    SwipeToDismissBoxValue.EndToStart -> {
                        // 左滑背景 - 置顶（蓝色/橙色）
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (isPinned) Color(0xFF757575) else Color(0xFFFFB800)
                                )
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PushPin,
                                contentDescription = if (isPinned) "取消置顶" else "置顶",
                                tint = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.scale(scale)
                            )
                        }
                    }
                    SwipeToDismissBoxValue.Settled -> {}
                }
            },
            content = {
                RepoItem(
                    repo = repo,
                    onClick = onClick,
                    onLongClick = onLongClick
                )
            }
        )
    }
}
