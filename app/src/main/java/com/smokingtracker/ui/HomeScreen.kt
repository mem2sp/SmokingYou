package com.smokingtracker.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smokingtracker.MainViewModel
import com.smokingtracker.R
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Preview(showBackground = true)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(viewModel: MainViewModel? = null) {
    val cookieShape = MaterialShapes.Cookie12Sided.toShape()
    val entries by (viewModel?.smokingEntries ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState()
    
    val initialDailyLimit = if (LocalInspectionMode.current) 10 else 0
    val dailyLimit by (viewModel?.dailyLimit ?: kotlinx.coroutines.flow.MutableStateFlow(initialDailyLimit)).collectAsState()

    var currentDate by remember { mutableStateOf(Calendar.getInstance()) }

    var timePassedText by remember { mutableStateOf("") }
    val calculatingText = stringResource(R.string.time_calculating)
    val invalidEntryText = stringResource(R.string.invalid_future_entry)
    val startTrackingText = stringResource(R.string.start_tracking)
    
    val formatHm = stringResource(R.string.duration_hm)
    val formatHms = stringResource(R.string.duration_hms)
    val formatMs = stringResource(R.string.duration_ms)
    
    var showLimitWarning by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(entries, formatHm, formatHms, formatMs) {
        if (timePassedText.isEmpty()) timePassedText = calculatingText
        while (true) {
            val lastEntry = entries.maxOrNull()
            if (lastEntry != null) {
                val now = System.currentTimeMillis()
                val diff = now - lastEntry
                
              if (diff < 0) {
                    timePassedText = invalidEntryText
                } else {
                    val hours = TimeUnit.MILLISECONDS.toHours(diff)
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60
                    
                    timePassedText = when {
                        hours >= 24 -> String.format(Locale.getDefault(), formatHm, hours, minutes)
                        hours > 0 -> String.format(Locale.getDefault(), formatHms, hours, minutes, seconds)
                        else -> String.format(Locale.getDefault(), formatMs, minutes, seconds)
                    }
                }
            } else {
                timePassedText = startTrackingText
            }
            delay(1000)
        }
    }

    val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
    val selectedDateStr = dateFormat.format(currentDate.time)
    
    val selectedDateEntries = entries.filter {
        val entryDate = Calendar.getInstance().apply { timeInMillis = it }
        entryDate.get(Calendar.YEAR) == currentDate.get(Calendar.YEAR) &&
        entryDate.get(Calendar.DAY_OF_YEAR) == currentDate.get(Calendar.DAY_OF_YEAR)
    }.sortedDescending()

    val today = Calendar.getInstance()
    val isToday = currentDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                  currentDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
                  
    if (showLimitWarning) {
        AlertDialog(
            onDismissRequest = { showLimitWarning = false },
            title = { Text(stringResource(R.string.limit_warning_title)) },
            text = { Text(stringResource(R.string.limit_warning_message)) },
            confirmButton = {
                TextButton(onClick = {
                    val now = Calendar.getInstance()
                    val entryDate = currentDate.clone() as Calendar
                    entryDate.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY))
                    entryDate.set(Calendar.MINUTE, now.get(Calendar.MINUTE))
                    entryDate.set(Calendar.SECOND, now.get(Calendar.SECOND))
                    entryDate.set(Calendar.MILLISECOND, now.get(Calendar.MILLISECOND))
                    if (entryDate.timeInMillis <= now.timeInMillis) {
                        viewModel?.addSmokingEntry(entryDate.timeInMillis)
                    }
                    showLimitWarning = false
                }) {
                    Text(stringResource(R.string.add_anyway))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLimitWarning = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

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
                            text = stringResource(R.string.home_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                ),
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = timePassedText,
                            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.time_past_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }

                if (dailyLimit > 0) {
                    val progress = (selectedDateEntries.size.toFloat() / dailyLimit.toFloat()).coerceIn(0f, 1f)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    LinearWavyProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        color = if (progress >= 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        stroke = WavyProgressIndicatorDefaults.linearIndicatorStroke,
                        trackStroke = WavyProgressIndicatorDefaults.linearTrackStroke
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatItem(stringResource(R.string.stat_daily), selectedDateEntries.size.toString(), Modifier.weight(1f))
                    StatItem(stringResource(R.string.stat_weekly), getWeeklyCount(entries, currentDate).toString(), Modifier.weight(1f)) 
                    StatItem(stringResource(R.string.stat_monthly), getMonthlyCount(entries, currentDate).toString(), Modifier.weight(1f)) 
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Surface(
                        onClick = {
                            val newDate = currentDate.clone() as Calendar
                            newDate.add(Calendar.DAY_OF_YEAR, -1)
                            currentDate = newDate
                        },
                        shape = cookieShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.previous_day))
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ) {
                        Text(
                            text = selectedDateStr,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                    }

                    Surface(
                        onClick = {
                            val newDate = currentDate.clone() as Calendar
                            newDate.add(Calendar.DAY_OF_YEAR, 1)
                            currentDate = newDate
                        },
                        enabled = !isToday,
                        shape = cookieShape,
                        color = if (!isToday) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (!isToday) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.next_day))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    itemsIndexed(selectedDateEntries) { index, entryTime ->
                        EntryItem(
                            entryTime = entryTime,
                            index = index,
                            onDelete = { viewModel?.removeSmokingEntry(entryTime) },
                            onEdit = { newTime ->
                                viewModel?.removeSmokingEntry(entryTime)
                                viewModel?.addSmokingEntry(newTime)
                            }
                        )
                    }
                }
            }

            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()

            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.85f else 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "fab_bounce_animation"
            )

            ExtendedFloatingActionButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 120.dp, end = 16.dp)
                    .scale(scale), 
                interactionSource = interactionSource, 
                onClick = {
                    if (dailyLimit in 1..selectedDateEntries.size) {
                        showLimitWarning = true
                    } else {
                        val now = Calendar.getInstance()
                        val entryDate = currentDate.clone() as Calendar
                        entryDate.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY))
                        entryDate.set(Calendar.MINUTE, now.get(Calendar.MINUTE))
                        entryDate.set(Calendar.SECOND, now.get(Calendar.SECOND))
                        entryDate.set(Calendar.MILLISECOND, now.get(Calendar.MILLISECOND))

                        if (entryDate.timeInMillis <= now.timeInMillis) {
                            viewModel?.addSmokingEntry(entryDate.timeInMillis)
                        }
                    }
                },
                text = { Text(stringResource(R.string.add_entry), fontWeight = FontWeight.Bold) },
                icon = { },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

fun getWeeklyCount(entries: List<Long>, date: Calendar): Int {
    val weekStart = date.clone() as Calendar
    weekStart.set(Calendar.DAY_OF_WEEK, weekStart.firstDayOfWeek)
    weekStart.set(Calendar.HOUR_OF_DAY, 0)
    weekStart.set(Calendar.MINUTE, 0)
    weekStart.set(Calendar.SECOND, 0)
    weekStart.set(Calendar.MILLISECOND, 0)

    val weekEnd = weekStart.clone() as Calendar
    weekEnd.add(Calendar.DAY_OF_YEAR, 7)

    return entries.count { it >= weekStart.timeInMillis && it < weekEnd.timeInMillis }
}

fun getMonthlyCount(entries: List<Long>, date: Calendar): Int {
    val monthStart = date.clone() as Calendar
    monthStart.set(Calendar.DAY_OF_MONTH, 1)
    monthStart.set(Calendar.HOUR_OF_DAY, 0)
    monthStart.set(Calendar.MINUTE, 0)
    monthStart.set(Calendar.SECOND, 0)
    monthStart.set(Calendar.MILLISECOND, 0)

    val monthEnd = monthStart.clone() as Calendar
    monthEnd.add(Calendar.MONTH, 1)

    return entries.count { it >= monthStart.timeInMillis && it < monthEnd.timeInMillis }
}

@Composable
fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier.aspectRatio(1f),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    MaterialTheme {
        // Mock data logic or just a simplified version
        HomeScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun EntryItemPreview() {
    EntryItem()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EntryItem(
    entryTime: Long = System.currentTimeMillis(),
    index: Int = 0,
    onDelete: () -> Unit = {},
    onEdit: (Long) -> Unit = {}
) {
    val cookieShape = MaterialShapes.Cookie9Sided.toShape()
    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(entryTime))
    var showTimePicker by remember { mutableStateOf(false) }

    val cal = Calendar.getInstance().apply { timeInMillis = entryTime }
    val timePickerState = rememberTimePickerState(
        initialHour = cal.get(Calendar.HOUR_OF_DAY),
        initialMinute = cal.get(Calendar.MINUTE),
        is24Hour = true,
    )

    if (showTimePicker) {
        TimePickerDialog(
            title = stringResource(R.string.edit_entry),
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newCal = Calendar.getInstance().apply { timeInMillis = entryTime }
                        newCal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        newCal.set(Calendar.MINUTE, timePickerState.minute)
                        onEdit(newCal.timeInMillis)
                        showTimePicker = false
                    }
                ) { Text(stringResource(R.string.dialog_ok)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showTimePicker = false }
                ) { Text(stringResource(R.string.dialog_cancel)) }
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    val (cardColor, pillBgColor, pillTextColor) = when (index % 3) {
        0 -> Triple(colorScheme.tertiaryContainer, colorScheme.onTertiaryContainer, colorScheme.tertiaryContainer)
        1 -> Triple(colorScheme.secondaryContainer, colorScheme.onSecondaryContainer, colorScheme.secondaryContainer)
        else -> Triple(colorScheme.primaryContainer, colorScheme.onPrimaryContainer, colorScheme.primaryContainer)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor,
        ),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    onClick = { showTimePicker = true },
                    shape = cookieShape,
                    color = pillBgColor.copy(alpha = 0.15f),
                    contentColor = pillBgColor,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.edit_entry),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Surface(
                    onClick = onDelete,
                    shape = cookieShape,
                    color = colorScheme.errorContainer,
                    contentColor = colorScheme.onErrorContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.delete_entry),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Surface(
                shape = CircleShape,
                color = pillBgColor,
                contentColor = pillTextColor,
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
        }
    }
}
