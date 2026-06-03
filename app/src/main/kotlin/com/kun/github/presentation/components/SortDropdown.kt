package com.kun.github.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.ForkLeft
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kun.github.data.model.GithubRepo
import com.kun.github.presentation.settings.strings

/**
 * 排序方式枚举
 */
enum class RepoSortOption(val key: String, val icon: ImageVector) {
    UPDATED("updated", Icons.Outlined.DateRange),
    STARS("stars", Icons.Outlined.Star),
    FORKS("forks", Icons.Outlined.ForkLeft),
    NAME("name", Icons.Outlined.TextFields)
}

/**
 * 排序下拉菜单组件
 */
@Composable
fun RepoSortDropdown(
    currentSort: RepoSortOption,
    onSortSelected: (RepoSortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = strings()
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        TextButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = currentSort.icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = when (currentSort) {
                    RepoSortOption.UPDATED -> s.sortUpdated
                    RepoSortOption.STARS -> s.sortStars
                    RepoSortOption.FORKS -> s.sortForks
                    RepoSortOption.NAME -> s.sortName
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                imageVector = Icons.Outlined.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 0.dp
        ) {
            RepoSortOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (option == currentSort) 
                                    MaterialTheme.colorScheme.primary 
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = when (option) {
                                    RepoSortOption.UPDATED -> s.sortUpdated
                                    RepoSortOption.STARS -> s.sortStars
                                    RepoSortOption.FORKS -> s.sortForks
                                    RepoSortOption.NAME -> s.sortName
                                },
                                fontSize = 14.sp,
                                color = if (option == currentSort) 
                                    MaterialTheme.colorScheme.primary 
                                else
                                    MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (option == currentSort) 
                                    FontWeight.SemiBold 
                                else 
                                    FontWeight.Normal
                            )
                        }
                    },
                    onClick = {
                        onSortSelected(option)
                        expanded = false
                    },
                    modifier = Modifier.height(44.dp)
                )
            }
        }
    }
}

/**
 * 根据排序选项对仓库列表进行排序
 */
fun sortRepos(
    repos: List<GithubRepo>,
    sortOption: RepoSortOption,
    pinnedIds: Set<String> = emptySet()
): List<GithubRepo> {
    return when (sortOption) {
        RepoSortOption.UPDATED -> {
            repos.sortedWith(
                compareByDescending<GithubRepo> { pinnedIds.contains(it.id.toString()) }
                    .thenByDescending { it.pushedAt ?: it.updatedAt ?: "" }
            )
        }
        RepoSortOption.STARS -> {
            repos.sortedWith(
                compareByDescending<GithubRepo> { pinnedIds.contains(it.id.toString()) }
                    .thenByDescending { it.stars }
            )
        }
        RepoSortOption.FORKS -> {
            repos.sortedWith(
                compareByDescending<GithubRepo> { pinnedIds.contains(it.id.toString()) }
                    .thenByDescending { it.forks }
            )
        }
        RepoSortOption.NAME -> {
            repos.sortedWith(
                compareByDescending<GithubRepo> { pinnedIds.contains(it.id.toString()) }
                    .thenBy { it.name.lowercase() }
            )
        }
    }
}
