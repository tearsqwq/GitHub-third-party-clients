package com.kun.github.ui.screens.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kun.github.R
import com.kun.github.ThemeMode
import com.kun.github.data.remote.client.GithubApiClient
import com.kun.github.presentation.auth.AuthViewModel
import com.kun.github.presentation.settings.LanguageManager
import com.kun.github.presentation.settings.AppStrings
import com.kun.github.presentation.settings.strings
import com.kun.github.presentation.user.UserViewModel
import java.io.File

/**
 * 设置页面 - 卡片式分组设计
 * 退出登录常驻底部，不随列表滚动
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    isDarkTheme: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    authViewModel: AuthViewModel,
    userViewModel: UserViewModel,
    cacheDir: File? = null,
    onBack: () -> Unit,
    onNavigateToAbout: () -> Unit = {},
    onNavigateToAccounts: () -> Unit = {},
    onLanguageChange: () -> Unit = {}
) {
    val context = LocalContext.current
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var currentLanguage by remember { mutableStateOf(LanguageManager.getCurrentLanguage(context)) }
    val s = strings()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 顶部导航栏
            TopAppBar(
                title = {
                    Text(
                        text = s.settingsTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_left),
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                windowInsets = WindowInsets.statusBars
            )

            // 可滚动内容区域（退出登录按钮除外）
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                // ====== 账号与安全 ======
                SectionHeader(title = "账号与安全")

                SettingsCard {
                    SettingsNavRow(
                        label = "账号管理",
                        onClick = { onNavigateToAccounts() }
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ====== 语言 ======
                SectionHeader(title = s.settingsLanguage)

                SettingsCard {
                    SettingsNavRow(
                        label = s.settingsLanguage,
                        value = when (currentLanguage) {
                            LanguageManager.AppLanguage.SYSTEM -> s.langSystem
                            LanguageManager.AppLanguage.CHINESE -> s.langChinese
                            LanguageManager.AppLanguage.ENGLISH -> s.langEnglish
                        },
                        onClick = { showLanguageDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ====== 外观 ======
                SectionHeader(title = s.settingsAppearance)

                SettingsCard {
                    // 深浅色模式三段式选择器
                    ThemeModeSelector(
                        currentMode = themeMode,
                        onModeSelected = onThemeModeChange,
                        s = s
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ====== 数据管理 ======
                SectionHeader(title = s.settingsDataManage)

                SettingsCard {
                    SettingsNavRow(
                        label = s.settingsClearCache,
                        onClick = {
                            GithubApiClient.clearAllCachedClients()
                            val prefs = context.getSharedPreferences("github_prefs", Context.MODE_PRIVATE)
                            prefs.edit().clear().apply()
                            GithubApiClient.clearHttpCache(cacheDir)
                            Toast.makeText(context, s.settingsCacheCleared, Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ====== 其他 ======
                SectionHeader(title = s.settingsOthers)

                SettingsCard {
                    SettingsNavRow(
                        label = s.settingsAbout,
                        onClick = { onNavigateToAbout() }
                    )
                }

                // 底部留白，避免被退出登录按钮遮挡
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // 退出登录按钮 - 常驻底部，不随列表滚动
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp)
        ) {
            LogoutButton(
                onClick = { showLogoutConfirm = true },
                s = s
            )
        }
    }

    // 语言选择弹窗
    if (showLanguageDialog) {
        LanguageSelectorDialog(
            currentLanguage = currentLanguage,
            onLanguageSelected = { language ->
                LanguageManager.setLanguage(context, language)
                currentLanguage = language
                showLanguageDialog = false
                onLanguageChange()
            },
            onDismiss = { showLanguageDialog = false },
            s = s
        )
    }

    // 退出登录确认弹窗
    if (showLogoutConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = s.logoutConfirm,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = s.logoutMessage,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showLogoutConfirm = false
                        authViewModel.logout()
                        userViewModel.resetState()
                    }
                ) {
                    Text(s.logout, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showLogoutConfirm = false }
                ) {
                    Text(s.cancel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

// ====== Segmented Button 风格主题模式选择器 ======

@Composable
private fun ThemeModeSelector(
    currentMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit,
    s: AppStrings
) {
    val options = listOf(
        Pair(ThemeMode.LIGHT, s.settingsLight),
        Pair(ThemeMode.SYSTEM, s.settingsSystem),
        Pair(ThemeMode.DARK, s.settingsDark)
    )

    val selectedIndex = when (currentMode) {
        ThemeMode.LIGHT -> 0
        ThemeMode.SYSTEM -> 1
        ThemeMode.DARK -> 2
    }

    Column(
        modifier = Modifier.padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = s.settingsDarkMode,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { index, (_, label) ->
                val isSelected = index == selectedIndex
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .then(
                            if (isSelected) Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                            else Modifier
                        )
                        .clickable { onModeSelected(options[index].first) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

// ====== 通用组件 ======

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 52.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    )
}

/**
 * 带右箭头的导航行
 */
@Composable
private fun SettingsNavRow(
    label: String,
    value: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        
        if (value != null) {
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }

        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * 语言选择对话框
 */
@Composable
private fun LanguageSelectorDialog(
    currentLanguage: LanguageManager.AppLanguage,
    onLanguageSelected: (LanguageManager.AppLanguage) -> Unit,
    onDismiss: () -> Unit,
    s: AppStrings
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = s.settingsSelectLanguage,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column {
                LanguageManager.AppLanguage.entries.forEach { language ->
                    val isSelected = language == currentLanguage
                    val displayName = when (language) {
                        LanguageManager.AppLanguage.SYSTEM -> s.langSystem
                        LanguageManager.AppLanguage.CHINESE -> s.langChinese
                        LanguageManager.AppLanguage.ENGLISH -> s.langEnglish
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLanguageSelected(language) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayName,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        
                        if (isSelected) {
                            androidx.compose.material3.RadioButton(
                                selected = true,
                                onClick = null,
                                colors = androidx.compose.material3.RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(s.cancel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
private fun LogoutButton(onClick: () -> Unit, s: AppStrings) {
    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
        ),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 15.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Logout,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = s.logout,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
