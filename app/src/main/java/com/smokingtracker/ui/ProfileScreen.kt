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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.CheckCircle
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
import com.smokingtracker.StatisticsManager
import com.smokingtracker.StatisticsData
import com.smokingtracker.ui.theme.AppTheme
import java.util.Locale
import java.util.Date
import java.text.SimpleDateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalScreen(
    viewModel: MainViewModel, 
    onNavigateToAbout: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToAppearance: () -> Unit
) {
    val themePreference by viewModel.themePreference.collectAsState()
    val dailyLimit by viewModel.dailyLimit.collectAsState()
    val fontPreset by viewModel.fontPreset.collectAsState()

    PersonalScreenContent(
        themePreference = themePreference,
        dailyLimit = dailyLimit,
        fontPreset = fontPreset,
        onThemeChange = viewModel::updateThemePreference,
        onSetDailyLimit = viewModel::setDailyLimit,
        onFontPresetChange = viewModel::updateFontPreset,
        onBackupData = { uri, onSuccess, onError -> viewModel.backupData(uri, onSuccess, onError) },
        onRestoreData = { uri, onSuccess, onError -> viewModel.restoreData(uri, onSuccess, onError) },
        onNavigateToAbout = onNavigateToAbout,
        onNavigateToAchievements = onNavigateToAchievements,
        onNavigateToStatistics = onNavigateToStatistics,
        onNavigateToAppearance = onNavigateToAppearance
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalScreenContent(
    themePreference: ThemePreference,
    dailyLimit: Int,
    fontPreset: String,
    onThemeChange: (ThemePreference) -> Unit,
    onSetDailyLimit: (Int) -> Unit,
    onFontPresetChange: (String) -> Unit,
    onBackupData: (android.net.Uri, () -> Unit, () -> Unit) -> Unit,
    onRestoreData: (android.net.Uri, () -> Unit, () -> Unit) -> Unit,
    onNavigateToAbout: () -> Unit = {},
    onNavigateToAchievements: () -> Unit = {},
    onNavigateToStatistics: () -> Unit = {},
    onNavigateToAppearance: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.personal_title),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                ),
            )
        }
    ) { paddingValues ->
        SettingsTab(
            modifier = Modifier.padding(paddingValues),
            currentTheme = themePreference,
            dailyLimit = dailyLimit,
            currentFontPreset = fontPreset,
            onThemeChange = onThemeChange,
            onSetDailyLimit = onSetDailyLimit,
            onFontPresetChange = onFontPresetChange,
            onBackupData = onBackupData,
            onRestoreData = onRestoreData,
            onNavigateToAbout = onNavigateToAbout,
            onNavigateToAchievements = onNavigateToAchievements,
            onNavigateToStatistics = onNavigateToStatistics,
            onNavigateToAppearance = onNavigateToAppearance
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab(
    modifier: Modifier = Modifier,
    currentTheme: ThemePreference,
    dailyLimit: Int,
    currentFontPreset: String,
    onThemeChange: (ThemePreference) -> Unit,
    onSetDailyLimit: (Int) -> Unit,
    onFontPresetChange: (String) -> Unit,
    onBackupData: (android.net.Uri, () -> Unit, () -> Unit) -> Unit,
    onRestoreData: (android.net.Uri, () -> Unit, () -> Unit) -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToAppearance: () -> Unit
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
        ModalBottomSheet(
            onDismissRequest = { showLanguageDialog = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 48.dp, top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.language_dialog_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Surface(
                    onClick = {
                        changeLanguage(context, "en")
                        showLanguageDialog = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
                    color = if (currentLocale == "en") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "English",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.weight(1f),
                            color = if (currentLocale == "en") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                        if (currentLocale == "en") {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    onClick = {
                        changeLanguage(context, "ru")
                        showLanguageDialog = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
                    color = if (currentLocale == "ru") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Русский",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.weight(1f),
                            color = if (currentLocale == "ru") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                        if (currentLocale == "ru") {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
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
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.settings_general),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp, start = 8.dp)
            )
        }
        item {
            SettingItem(
                icon = Icons.Filled.Brightness4,
                title = stringResource(R.string.settings_appearance),
                subtitle = stringResource(R.string.settings_appearance_desc),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
                onClick = onNavigateToAppearance
            )
        }
        item {
            SettingItem(
                icon = Icons.Filled.Language, 
                title = stringResource(R.string.settings_language), 
                subtitle = if (currentLocale == "ru") "Русский" else "English",
                shape = RoundedCornerShape(8.dp),
                onClick = { showLanguageDialog = true }
            )
        }
        item {
            SettingItem(
                icon = Icons.Filled.EmojiEvents,
                title = stringResource(R.string.settings_achievements),
                subtitle = stringResource(R.string.achievements_desc),
                shape = RoundedCornerShape(8.dp),
                onClick = onNavigateToAchievements
            )
        }
        item {
            SettingItem(
                icon = Icons.Filled.Warning, 
                title = stringResource(R.string.settings_daily_limit), 
                subtitle = if (dailyLimit > 0) dailyLimit.toString() else stringResource(R.string.no_limit),
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
                onClick = { showLimitDialog = true }
            )
        }

        item {
            Text(
                text = stringResource(R.string.settings_data),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 8.dp)
            )
        }
        item {
            SettingItem(
                icon = Icons.Filled.Backup, 
                title = stringResource(R.string.settings_backup), 
                subtitle = stringResource(R.string.backup_desc),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
                onClick = { backupLauncher.launch("smoking_tracker_backup.json") }
            )
        }
        item {
            SettingItem(
                icon = Icons.Filled.Restore, 
                title = stringResource(R.string.settings_restore), 
                subtitle = stringResource(R.string.restore_desc),
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
                onClick = { restoreLauncher.launch(arrayOf("application/json")) }
            )
        }

        item {
            Text(
                text = stringResource(R.string.settings_info),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 8.dp)
            )
        }
        item {
            SettingItem(
                icon = Icons.Filled.Info, 
                title = stringResource(R.string.about_app), 
                subtitle = stringResource(R.string.about_app_desc),
                shape = RoundedCornerShape(24.dp),
                onClick = onNavigateToAbout
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
fun SettingItemContent(
    icon: ImageVector, 
    title: String, 
    subtitle: String, 
    onClick: (() -> Unit)? = null
) {
    val modifier = Modifier
        .fillMaxWidth()
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(horizontal = 20.dp, vertical = 18.dp)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title, 
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), 
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle, 
                style = MaterialTheme.typography.bodyMedium, 
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        SettingItemContent(icon = icon, title = title, subtitle = subtitle, onClick = onClick)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val entries by viewModel.smokingEntries.collectAsState()
    val stats = remember(entries) { StatisticsManager.calculateStats(entries) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_statistics),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                ),
            )
        }
    ) { paddingValues ->
        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.stats_no_data), style = MaterialTheme.typography.titleMedium)
            }
        } else {
            StatisticsList(modifier = Modifier.padding(paddingValues), stats = stats)
        }
    }
}

@Composable
fun StatisticsList(modifier: Modifier = Modifier, stats: StatisticsData) {
    val dateFormat = SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.getDefault())
    val trackingSinceStr = stats.trackingSince?.let { dateFormat.format(Date(it)) } ?: stringResource(R.string.stats_no_data)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            StatCard(
                title = stringResource(R.string.stats_total_count),
                value = stats.totalCount.toString(),
                icon = Icons.Filled.BarChart,
                color = MaterialTheme.colorScheme.primaryContainer
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.stats_max_per_day),
                    value = stats.maxPerDay.toString(),
                    icon = Icons.Filled.Warning,
                    color = MaterialTheme.colorScheme.errorContainer
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.stats_min_per_day),
                    value = stats.minPerDay.toString(),
                    icon = Icons.Filled.Language, // Just a placeholder icon
                    color = MaterialTheme.colorScheme.secondaryContainer
                )
            }
        }
        item {
            StatCard(
                title = stringResource(R.string.stats_avg_per_day),
                value = stats.avgPerDay.toString(),
                icon = Icons.Filled.BarChart,
                color = MaterialTheme.colorScheme.tertiaryContainer
            )
        }
        item {
            StatCard(
                title = stringResource(R.string.stats_max_smoke_free_streak),
                value = stringResource(R.string.stats_days_unit, stats.longestStreakDays),
                icon = Icons.Filled.EmojiEvents,
                color = MaterialTheme.colorScheme.primaryContainer
            )
        }
        item {
            StatCard(
                title = stringResource(R.string.stats_tracking_since),
                value = trackingSinceStr,
                icon = Icons.Filled.Info,
                color = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.2f),
                contentColor = color,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title, 
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), 
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                Text(
                    text = value, 
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val entries by viewModel.smokingEntries.collectAsState()
    val unlockedAchievements by viewModel.unlockedAchievements.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.my_achievements),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                ),
            )
        }
    ) { paddingValues ->
        AchievementsTab(
            modifier = Modifier.padding(paddingValues),
            entries = entries, 
            unlockedAchievements = unlockedAchievements
        )
    }
}

@Composable
fun AchievementsTab(
    modifier: Modifier = Modifier,
    entries: List<Long>, 
    unlockedAchievements: Set<String>
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
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

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        achievements.forEach { achievement ->
                            val isUnlocked = unlockedAchievements.contains(achievement.id)

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isUnlocked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceContainerLowest,
                                border = if (isUnlocked) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                        contentColor = if (isUnlocked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (isUnlocked) Icons.Filled.CheckCircle else Icons.Filled.Lock,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(16.dp))
                                    
                                    Column {
                                        Text(
                                            text = stringResource(achievement.titleResId),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (isUnlocked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = stringResource(achievement.descResId),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isUnlocked) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
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
            dailyLimit = 10,
            fontPreset = "WIDE",
            onThemeChange = {},
            onSetDailyLimit = {},
            onFontPresetChange = {},
            onBackupData = { _, _, _ -> },
            onRestoreData = { _, _, _ -> },
            onNavigateToAbout = {},
            onNavigateToAchievements = {},
            onNavigateToStatistics = {},
            onNavigateToAppearance = {}
        )
    }
}
