package com.smokingtracker.ui

import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smokingtracker.MainViewModel
import com.smokingtracker.R
import com.smokingtracker.data.ThemePreference
import com.smokingtracker.AchievementsManager
import com.smokingtracker.ui.theme.AppTheme
import com.smokingtracker.AchievementCategory
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalScreen(viewModel: MainViewModel, onNavigateToAbout: () -> Unit) {
    val themePreference by viewModel.themePreference.collectAsState()
    val entries by viewModel.smokingEntries.collectAsState()
    val unlockedAchievements by viewModel.unlockedAchievements.collectAsState()
    val dailyLimit by viewModel.dailyLimit.collectAsState()

    PersonalScreenContent(
        themePreference = themePreference,
        entries = entries,
        unlockedAchievements = unlockedAchievements,
        dailyLimit = dailyLimit,
        onThemeChange = viewModel::updateThemePreference,
        onSetDailyLimit = viewModel::setDailyLimit,
        onBackupData = { uri, onSuccess, onError -> viewModel.backupData(uri, onSuccess, onError) },
        onRestoreData = { uri, onSuccess, onError -> viewModel.restoreData(uri, onSuccess, onError) },
        onNavigateToAbout = onNavigateToAbout
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalScreenContent(
    themePreference: ThemePreference,
    entries: List<Long>,
    unlockedAchievements: Set<String>,
    dailyLimit: Int,
    onThemeChange: (ThemePreference) -> Unit,
    onSetDailyLimit: (Int) -> Unit,
    onBackupData: (android.net.Uri, () -> Unit, () -> Unit) -> Unit,
    onRestoreData: (android.net.Uri, () -> Unit, () -> Unit) -> Unit,
    onNavigateToAbout: () -> Unit = {}
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(stringResource(R.string.tab_settings), stringResource(R.string.tab_achievements))

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Text(
                            text = stringResource(R.string.personal_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                ),
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                space = (-8).dp
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTabIndex == index
                    val cornerRadius by animateDpAsState(
                        targetValue = if (isSelected) 28.dp else 12.dp,
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
                        label = "tab_corner"
                    )
                    SegmentedButton(
                        selected = isSelected,
                        onClick = { selectedTabIndex = index },
                        shape = RoundedCornerShape(cornerRadius),
                        border = BorderStroke(0.dp, Color.Transparent),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primary,
                            activeContentColor = MaterialTheme.colorScheme.onPrimary,
                            inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            activeBorderColor = Color.Transparent,
                            inactiveBorderColor = Color.Transparent
                        ),
                        label = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (selectedTabIndex) {
                0 -> SettingsTab(
                    currentTheme = themePreference,
                    dailyLimit = dailyLimit,
                    onThemeChange = onThemeChange,
                    onSetDailyLimit = onSetDailyLimit,
                    onBackupData = onBackupData,
                    onRestoreData = onRestoreData,
                    onNavigateToAbout = onNavigateToAbout
                )
                1 -> AchievementsTab(entries = entries, unlockedAchievements = unlockedAchievements)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab(
    currentTheme: ThemePreference,
    dailyLimit: Int,
    onThemeChange: (ThemePreference) -> Unit,
    onSetDailyLimit: (Int) -> Unit,
    onBackupData: (android.net.Uri, () -> Unit, () -> Unit) -> Unit,
    onRestoreData: (android.net.Uri, () -> Unit, () -> Unit) -> Unit,
    onNavigateToAbout: () -> Unit
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showLimitDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    
    val currentLocale = LocalConfiguration.current.locales.get(0)?.language ?: Locale.getDefault().language

    val backupSuccessStr = stringResource(R.string.backup_success)
    val backupErrorStr = stringResource(R.string.backup_error)
    val restoreSuccessStr = stringResource(R.string.restore_success)
    val restoreErrorStr = stringResource(R.string.restore_error)

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            onBackupData(
                it,
                { Toast.makeText(context, backupSuccessStr, Toast.LENGTH_SHORT).show() },
                { Toast.makeText(context, backupErrorStr, Toast.LENGTH_SHORT).show() }
            )
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            onRestoreData(
                it,
                { Toast.makeText(context, restoreSuccessStr, Toast.LENGTH_SHORT).show() },
                { Toast.makeText(context, restoreErrorStr, Toast.LENGTH_SHORT).show() }
            )
        }
    }

    if (showLanguageDialog) {
        BasicAlertDialog(onDismissRequest = { showLanguageDialog = false }) {
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        stringResource(R.string.language_dialog_title), 
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            changeLanguage(context, "en")
                            showLanguageDialog = false
                        }
                        .padding(16.dp)) {
                        Text("English", style = MaterialTheme.typography.bodyLarge)
                    }
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            changeLanguage(context, "ru")
                            showLanguageDialog = false
                        }
                        .padding(16.dp)) {
                        Text("Русский", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }

    if (showLimitDialog) {
        var newLimit by remember { mutableStateOf(dailyLimit.toString()) }
        BasicAlertDialog(onDismissRequest = { showLimitDialog = false }) {
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        stringResource(R.string.set_limit_title), 
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newLimit,
                        onValueChange = { newLimit = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text (stringResource(R.string.no_limit) ) },
                        shape = RoundedCornerShape(16.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { showLimitDialog = false }) {
                            Text(stringResource(R.string.dialog_cancel), fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            onSetDailyLimit(newLimit.toIntOrNull() ?: 0)
                            showLimitDialog = false
                        }) {
                            Text(stringResource(R.string.dialog_ok), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.settings_appearance),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
            )
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp), // MD3E rounding
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.elevatedCardElevation(0.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Brightness4, contentDescription = null)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            stringResource(R.string.settings_theme), 
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    ThemeSegmentedButton(
                        currentTheme = currentTheme,
                        onThemeChange = onThemeChange
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
        
        item {
            Text(
                text = stringResource(R.string.settings_general),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SettingItem(
                    icon = Icons.Filled.Language, 
                    title = stringResource(R.string.settings_language), 
                    subtitle = if (currentLocale == "ru") "Русский" else "English",
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                    onClick = { showLanguageDialog = true }
                )
                SettingItem(
                    icon = Icons.Filled.Warning, 
                    title = stringResource(R.string.settings_daily_limit), 
                    subtitle = if (dailyLimit > 0) dailyLimit.toString() else stringResource(R.string.no_limit),
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 28.dp, bottomEnd = 28.dp),
                    onClick = { showLimitDialog = true }
                )
            }
        }

        item {
            Text(
                text = stringResource(R.string.settings_data),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SettingItem(
                    icon = Icons.Filled.Backup, 
                    title = stringResource(R.string.settings_backup), 
                    subtitle = stringResource(R.string.backup_desc),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                    onClick = { backupLauncher.launch("smoking_tracker_backup.json") }
                )
                SettingItem(
                    icon = Icons.Filled.Restore, 
                    title = stringResource(R.string.settings_restore), 
                    subtitle = stringResource(R.string.restore_desc),
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 28.dp, bottomEnd = 28.dp),
                    onClick = { restoreLauncher.launch(arrayOf("application/json")) }
                )
            }
        }

        item {
            Text(
                text = stringResource(R.string.settings_info),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
            )
            SettingItem(
                icon = Icons.Filled.Info, 
                title = stringResource(R.string.about_app), 
                subtitle = stringResource(R.string.about_app_desc),
                shape = RoundedCornerShape(28.dp),
                onClick = onNavigateToAbout
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSegmentedButton(
    currentTheme: ThemePreference,
    onThemeChange: (ThemePreference) -> Unit
) {
    val options = listOf(
        ThemePreference.LIGHT to stringResource(R.string.theme_light),
        ThemePreference.SYSTEM to stringResource(R.string.theme_system),
        ThemePreference.DARK to stringResource(R.string.theme_dark)
    )

    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        space = (-8).dp
    ) {
        options.forEachIndexed { index, (theme, title) ->
            val isSelected = currentTheme == theme
            val cornerRadius by animateDpAsState(
                targetValue = if (isSelected) 28.dp else 12.dp,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
                label = "corner"
            )
            SegmentedButton(
                selected = isSelected,
                onClick = { onThemeChange(theme) },
                shape = RoundedCornerShape(cornerRadius),
                border = BorderStroke(0.dp, Color.Transparent),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primary,
                    activeContentColor = MaterialTheme.colorScheme.onPrimary,
                    inactiveContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    inactiveContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    activeBorderColor = Color.Transparent,
                    inactiveBorderColor = Color.Transparent
                ),
                label = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            )
        }
    }
}

fun changeLanguage(context: android.content.Context, languageTag: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(languageTag)
    } else {
        val locale = Locale(languageTag)
        Locale.setDefault(locale)
        val config = android.content.res.Configuration()
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
        if (context is android.app.Activity) {
            context.recreate()
        }
    }
}

@Composable
fun SettingItem(
    icon: ImageVector, 
    title: String, 
    subtitle: String, 
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(28.dp),
    onClick: (() -> Unit)? = null
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        val modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 20.dp)

        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun AchievementsTab(entries: List<Long>, unlockedAchievements: Set<String>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        val groupedAchievements = AchievementsManager.achievementsList.groupBy { it.category }
        
        groupedAchievements.forEach { (category, achievements) ->
            item {
                Text(
                    text = stringResource(category.titleResId),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
                )

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.elevatedCardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        achievements.forEach { achievement ->
                            val isUnlocked = unlockedAchievements.contains(achievement.id)

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isUnlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(12.dp)
                                    ) {}
                                    
                                    Spacer(modifier = Modifier.width(16.dp))
                                    
                                    Column {
                                        Text(
                                            text = stringResource(achievement.titleResId),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if(isUnlocked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = stringResource(achievement.descResId),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if(isUnlocked) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha=0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PersonalScreenPreview() {
    AppTheme {
        PersonalScreenContent(
            themePreference = ThemePreference.SYSTEM,
            entries = emptyList(),
            unlockedAchievements = setOf("first_smoke"),
            dailyLimit = 10,
            onThemeChange = {},
            onSetDailyLimit = {},
            onBackupData = { _, _, _ -> },
            onRestoreData = { _, _, _ -> },
            onNavigateToAbout = {}
        )
    }
}
