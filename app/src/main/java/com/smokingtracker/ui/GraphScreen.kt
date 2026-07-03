package com.smokingtracker.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smokingtracker.MainViewModel
import com.smokingtracker.R
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen(viewModel: MainViewModel, onNavigateToStatistics: () -> Unit) {
    val entries by viewModel.smokingEntries.collectAsState()
    GraphScreenContent(entries = entries, onNavigateToStatistics = onNavigateToStatistics)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreenContent(entries: List<Long>, onNavigateToStatistics: () -> Unit = {}) {
    var dailyDate by remember { mutableStateOf(Calendar.getInstance()) }
    var weeklyDate by remember { mutableStateOf(Calendar.getInstance()) }
    var monthlyDate by remember { mutableStateOf(Calendar.getInstance()) }
    var yearlyDate by remember { mutableStateOf(Calendar.getInstance()) }

    val dailyData = remember(entries, dailyDate) { generateDailyData(entries, dailyDate) }
    val weeklyData = remember(entries, weeklyDate) { generateWeeklyData(entries, weeklyDate) }
    val monthlyData = remember(entries, monthlyDate) { generateMonthlyData(entries, monthlyDate) }
    val yearlyData = remember(entries, yearlyDate) { generateYearlyData(entries, yearlyDate) }

    var activeDatePickerTarget by remember { mutableStateOf<String?>(null) }

    activeDatePickerTarget?.let { target ->
        val targetCalendar = when (target) {
            "daily" -> dailyDate
            "weekly" -> weeklyDate
            "monthly" -> monthlyDate
            else -> yearlyDate
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = targetCalendar.timeInMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= System.currentTimeMillis()
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { activeDatePickerTarget = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val newDate = Calendar.getInstance().apply { timeInMillis = millis }
                            when (target) {
                                "daily" -> dailyDate = newDate
                                "weekly" -> weeklyDate = newDate
                                "monthly" -> monthlyDate = newDate
                                "yearly" -> yearlyDate = newDate
                            }
                        }
                        activeDatePickerTarget = null
                    }
                ) {
                    Text(stringResource(R.string.dialog_ok), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { activeDatePickerTarget = null }) {
                    Text(stringResource(R.string.dialog_cancel), fontWeight = FontWeight.Bold)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.analytics_title),
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                    onClick = onNavigateToStatistics,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.BarChart, contentDescription = null, modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_statistics),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = stringResource(R.string.statistics_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            item {
                val dailyStr = remember(dailyDate) { dateFormat.format(dailyDate.time) }
                GraphSection(
                    title = stringResource(R.string.daily_overview),
                    totalCount = dailyData.sum(),
                    dateLabel = dailyStr,
                    dataPoints = dailyData,
                    onPrevious = { dailyDate = dailyDate.clone().apply { (this as Calendar).add(Calendar.DAY_OF_YEAR, -1) } as Calendar },
                    onNext = { dailyDate = dailyDate.clone().apply { (this as Calendar).add(Calendar.DAY_OF_YEAR, 1) } as Calendar },
                    onDateClick = { activeDatePickerTarget = "daily" }
                )
            }

            item {
                val weeklyStr = remember(weeklyDate) {
                    val weekStart = weeklyDate.clone() as Calendar
                    weekStart.set(Calendar.DAY_OF_WEEK, weekStart.firstDayOfWeek)
                    val weekEnd = weekStart.clone() as Calendar
                    weekEnd.add(Calendar.DAY_OF_YEAR, 6)
                    
                    val shortFormat = SimpleDateFormat("d MMM", Locale.getDefault())
                    "${shortFormat.format(weekStart.time)} - ${shortFormat.format(weekEnd.time)}"
                }
                GraphSection(
                    title = stringResource(R.string.weekly_overview),
                    totalCount = weeklyData.sum(),
                    dateLabel = weeklyStr,
                    dataPoints = weeklyData,
                    onPrevious = { weeklyDate = weeklyDate.clone().apply { (this as Calendar).add(Calendar.WEEK_OF_YEAR, -1) } as Calendar },
                    onNext = { weeklyDate = weeklyDate.clone().apply { (this as Calendar).add(Calendar.WEEK_OF_YEAR, 1) } as Calendar },
                    onDateClick = { activeDatePickerTarget = "weekly" }
                )
            }

            item {
                val monthlyStr = remember(monthlyDate) {
                    SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(monthlyDate.time)
                }
                GraphSection(
                    title = stringResource(R.string.monthly_overview),
                    totalCount = monthlyData.sum(),
                    dateLabel = monthlyStr,
                    dataPoints = monthlyData,
                    onPrevious = { monthlyDate = monthlyDate.clone().apply { (this as Calendar).add(Calendar.MONTH, -1) } as Calendar },
                    onNext = { monthlyDate = monthlyDate.clone().apply { (this as Calendar).add(Calendar.MONTH, 1) } as Calendar },
                    onDateClick = { activeDatePickerTarget = "monthly" }
                )
            }

            item {
                val yearlyStr = remember(yearlyDate) {
                    SimpleDateFormat("yyyy", Locale.getDefault()).format(yearlyDate.time)
                }
                GraphSection(
                    title = stringResource(R.string.yearly_overview),
                    totalCount = yearlyData.sum(),
                    dateLabel = yearlyStr,
                    dataPoints = yearlyData,
                    onPrevious = { yearlyDate = yearlyDate.clone().apply { (this as Calendar).add(Calendar.YEAR, -1) } as Calendar },
                    onNext = { yearlyDate = yearlyDate.clone().apply { (this as Calendar).add(Calendar.YEAR, 1) } as Calendar },
                    onDateClick = { activeDatePickerTarget = "yearly" }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GraphSection(
    title: String,
    totalCount: Int,
    dateLabel: String,
    dataPoints: List<Int>,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDateClick: (() -> Unit)? = null
) {
    val cookieShape = MaterialShapes.Cookie12Sided.toShape()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(
                        text = "$totalCount",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            LineGraph(dataPoints = dataPoints, modifier = Modifier.fillMaxWidth().height(160.dp))
            
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onPrevious,
                    shape = cookieShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
                    }
                }

                Surface(
                    onClick = { onDateClick?.invoke() },
                    enabled = onDateClick != null,
                    shape = RoundedCornerShape(24.dp),
                    color = if (onDateClick != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = if (onDateClick != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    border = BorderStroke(1.dp, if (onDateClick != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (onDateClick != null) {
                            Icon(
                                imageVector = Icons.Filled.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Surface(
                    onClick = onNext,
                    shape = cookieShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
                    }
                }
            }
        }
    }
}

@Composable
fun LineGraph(dataPoints: List<Int>, modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)

    val progress = remember(dataPoints) { Animatable(0f) }

    LaunchedEffect(progress) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
    }

    Canvas(modifier = modifier.padding(horizontal = 8.dp, vertical = 16.dp)) {
        val maxPoint = dataPoints.maxOrNull()?.toFloat()?.takeIf { it > 0 } ?: 1f
        val yFactor = size.height / (maxPoint * 1.2f)
        val xFactor = if (dataPoints.size > 1) size.width / (dataPoints.size - 1) else size.width

        val gridLines = 4
        for (i in 0..gridLines) {
            val y = size.height - (i * (size.height / gridLines))
            drawLine(
                color = surfaceVariant,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        if (dataPoints.isEmpty()) return@Canvas

        val path = Path()
        var prevX = 0f
        var prevY = size.height - (dataPoints[0] * yFactor)
        path.moveTo(prevX, prevY)

        for (index in 1 until dataPoints.size) {
            val x = index * xFactor
            val y = size.height - (dataPoints[index] * yFactor)
            
            // Generate clean cubic S-curves between points
            val controlX1 = (prevX + x) / 2f
            val controlY1 = prevY
            val controlX2 = (prevX + x) / 2f
            val controlY2 = y
            
            path.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
            
            prevX = x
            prevY = y
        }

        val fillPath = Path().apply {
            addPath(path)
            lineTo((dataPoints.size - 1) * xFactor, size.height)
            lineTo(0f, size.height)
            close()
        }

        clipRect(right = size.width * progress.value) {
            // Draw gradient area under the curve
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.3f),
                        primaryColor.copy(alpha = 0.0f)
                    )
                )
            )

            // Draw line
            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(
                    width = 4.dp.toPx(), 
                    cap = StrokeCap.Round, 
                    join = StrokeJoin.Round
                )
            )

            // Draw data point circles
            dataPoints.forEachIndexed { index, value ->
                val x = index * xFactor
                val y = size.height - (value * yFactor)

                // Glowing outer aura
                drawCircle(
                    color = primaryColor.copy(alpha = 0.15f),
                    radius = 9.dp.toPx(),
                    center = Offset(x, y)
                )
                // Outer circle border
                drawCircle(
                    color = primaryColor,
                    radius = 5.dp.toPx(),
                    center = Offset(x, y)
                )
                // Solid center white core
                drawCircle(
                    color = Color.White,
                    radius = 2.5.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }
    }
}


fun generateDailyData(entries: List<Long>, date: Calendar): List<Int> {
    val dayStart = date.clone() as Calendar
    dayStart.set(Calendar.HOUR_OF_DAY, 0)
    dayStart.set(Calendar.MINUTE, 0)
    dayStart.set(Calendar.SECOND, 0)
    dayStart.set(Calendar.MILLISECOND, 0)
    val dayStartMillis = dayStart.timeInMillis

    val dayEnd = dayStart.clone() as Calendar
    dayEnd.add(Calendar.DAY_OF_YEAR, 1)
    val dayEndMillis = dayEnd.timeInMillis

    val dayEntries = entries.filter { it >= dayStartMillis && it < dayEndMillis }
    val hourlyCounts = IntArray(24) { 0 }
    
    val cal = Calendar.getInstance()
    dayEntries.forEach { time ->
        cal.timeInMillis = time
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        if (hour in 0..23) {
            hourlyCounts[hour]++
        }
    }
    return hourlyCounts.toList()
}

fun generateWeeklyData(entries: List<Long>, date: Calendar): List<Int> {
    val weekStart = date.clone() as Calendar
    weekStart.set(Calendar.DAY_OF_WEEK, weekStart.firstDayOfWeek)
    weekStart.set(Calendar.HOUR_OF_DAY, 0)
    weekStart.set(Calendar.MINUTE, 0)
    weekStart.set(Calendar.SECOND, 0)
    weekStart.set(Calendar.MILLISECOND, 0)
    val weekStartMillis = weekStart.timeInMillis

    val weekEnd = weekStart.clone() as Calendar
    weekEnd.add(Calendar.DAY_OF_YEAR, 7)
    val weekEndMillis = weekEnd.timeInMillis

    val weekEntries = entries.filter { it >= weekStartMillis && it < weekEndMillis }
    val dailyCounts = IntArray(7) { 0 }
    
    val cal = Calendar.getInstance()
    weekEntries.forEach { time ->
        val diffDays = ((time - weekStartMillis) / (24 * 60 * 60 * 1000L)).toInt().coerceIn(0, 6)
        dailyCounts[diffDays]++
    }
    return dailyCounts.toList()
}

fun generateMonthlyData(entries: List<Long>, date: Calendar): List<Int> {
    val monthStart = date.clone() as Calendar
    monthStart.set(Calendar.DAY_OF_MONTH, 1)
    monthStart.set(Calendar.HOUR_OF_DAY, 0)
    monthStart.set(Calendar.MINUTE, 0)
    monthStart.set(Calendar.SECOND, 0)
    monthStart.set(Calendar.MILLISECOND, 0)
    val monthStartMillis = monthStart.timeInMillis
    
    val monthEnd = monthStart.clone() as Calendar
    monthEnd.add(Calendar.MONTH, 1)
    val monthEndMillis = monthEnd.timeInMillis

    val monthEntries = entries.filter { it >= monthStartMillis && it < monthEndMillis }
    val daysInMonth = monthStart.getActualMaximum(Calendar.DAY_OF_MONTH)
    val dailyCounts = IntArray(daysInMonth) { 0 }
    
    val cal = Calendar.getInstance()
    monthEntries.forEach { time ->
        cal.timeInMillis = time
        val dayIndex = cal.get(Calendar.DAY_OF_MONTH) - 1
        if (dayIndex in 0 until daysInMonth) {
            dailyCounts[dayIndex]++
        }
    }

    val chunkSize = Math.ceil(daysInMonth / 4.0).toInt()
    val weeklyChunks = mutableListOf<Int>()
    for (i in 0 until 4) {
        var sum = 0
        for (j in 0 until chunkSize) {
            val index = i * chunkSize + j
            if (index < daysInMonth) sum += dailyCounts[index]
        }
        weeklyChunks.add(sum)
    }
    return weeklyChunks
}

fun generateYearlyData(entries: List<Long>, date: Calendar): List<Int> {
    val yearStart = date.clone() as Calendar
    yearStart.set(Calendar.DAY_OF_YEAR, 1)
    yearStart.set(Calendar.HOUR_OF_DAY, 0)
    yearStart.set(Calendar.MINUTE, 0)
    yearStart.set(Calendar.SECOND, 0)
    yearStart.set(Calendar.MILLISECOND, 0)
    val yearStartMillis = yearStart.timeInMillis

    val yearEnd = yearStart.clone() as Calendar
    yearEnd.add(Calendar.YEAR, 1)
    val yearEndMillis = yearEnd.timeInMillis

    val yearEntries = entries.filter { it >= yearStartMillis && it < yearEndMillis }
    val monthlyCounts = IntArray(12) { 0 }
    
    val cal = Calendar.getInstance()
    yearEntries.forEach { time ->
        cal.timeInMillis = time
        val monthIndex = cal.get(Calendar.MONTH)
        if (monthIndex in 0..11) {
            monthlyCounts[monthIndex]++
        }
    }
    return monthlyCounts.toList()
}

@Preview(showBackground = true)
@Composable
private fun GraphScreenPreview() {
    MaterialTheme {
        GraphScreenContent(entries = emptyList())
    }
}

@Preview(showBackground = true)
@Composable
private fun GraphSectionPreview() {
    MaterialTheme {
        GraphSection(
            title = "Daily Overview",
            totalCount = 12,
            dateLabel = "2023-10-25",
            dataPoints = listOf(1, 4, 2, 5, 0),
            onPrevious = {},
            onNext = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LineGraphPreview() {
    MaterialTheme {
        LineGraph(dataPoints = listOf(1, 4, 2, 5, 0), modifier = Modifier.fillMaxWidth().height(150.dp))
    }
}
