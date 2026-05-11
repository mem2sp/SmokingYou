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
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
fun GraphScreen(viewModel: MainViewModel) {
    val entries by viewModel.smokingEntries.collectAsState()
    GraphScreenContent(entries = entries)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreenContent(entries: List<Long>) {
    var dailyDate by remember { mutableStateOf(Calendar.getInstance()) }
    var weeklyDate by remember { mutableStateOf(Calendar.getInstance()) }
    var monthlyDate by remember { mutableStateOf(Calendar.getInstance()) }
    var yearlyDate by remember { mutableStateOf(Calendar.getInstance()) }

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
                            text = stringResource(R.string.analytics_title),
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
                val dailyStr = dateFormat.format(dailyDate.time)
                val dailyData = generateDailyData(entries, dailyDate)
                GraphSection(
                    title = stringResource(R.string.daily_overview),
                    totalCount = dailyData.sum(),
                    dateLabel = dailyStr,
                    dataPoints = dailyData,
                    onPrevious = { dailyDate = dailyDate.clone().apply { (this as Calendar).add(Calendar.DAY_OF_YEAR, -1) } as Calendar },
                    onNext = { dailyDate = dailyDate.clone().apply { (this as Calendar).add(Calendar.DAY_OF_YEAR, 1) } as Calendar }
                )
            }

            item {
                val weekStart = weeklyDate.clone() as Calendar
                weekStart.set(Calendar.DAY_OF_WEEK, weekStart.firstDayOfWeek)
                val weekEnd = weekStart.clone() as Calendar
                weekEnd.add(Calendar.DAY_OF_YEAR, 6)
                
                val shortFormat = SimpleDateFormat("d MMM", Locale.getDefault())
                val weeklyStr = "${shortFormat.format(weekStart.time)} - ${shortFormat.format(weekEnd.time)}"
                
                val weeklyData = generateWeeklyData(entries, weeklyDate)
                GraphSection(
                    title = stringResource(R.string.weekly_overview),
                    totalCount = weeklyData.sum(),
                    dateLabel = weeklyStr,
                    dataPoints = weeklyData,
                    onPrevious = { weeklyDate = weeklyDate.clone().apply { (this as Calendar).add(Calendar.WEEK_OF_YEAR, -1) } as Calendar },
                    onNext = { weeklyDate = weeklyDate.clone().apply { (this as Calendar).add(Calendar.WEEK_OF_YEAR, 1) } as Calendar }
                )
            }

            item {
                val monthlyStr = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(monthlyDate.time)
                val monthlyData = generateMonthlyData(entries, monthlyDate)
                GraphSection(
                    title = stringResource(R.string.monthly_overview),
                    totalCount = monthlyData.sum(),
                    dateLabel = monthlyStr,
                    dataPoints = monthlyData,
                    onPrevious = { monthlyDate = monthlyDate.clone().apply { (this as Calendar).add(Calendar.MONTH, -1) } as Calendar },
                    onNext = { monthlyDate = monthlyDate.clone().apply { (this as Calendar).add(Calendar.MONTH, 1) } as Calendar }
                )
            }

            item {
                val yearlyStr = SimpleDateFormat("yyyy", Locale.getDefault()).format(yearlyDate.time)
                val yearlyData = generateYearlyData(entries, yearlyDate)
                GraphSection(
                    title = stringResource(R.string.yearly_overview),
                    totalCount = yearlyData.sum(),
                    dateLabel = yearlyStr,
                    dataPoints = yearlyData,
                    onPrevious = { yearlyDate = yearlyDate.clone().apply { (this as Calendar).add(Calendar.YEAR, -1) } as Calendar },
                    onNext = { yearlyDate = yearlyDate.clone().apply { (this as Calendar).add(Calendar.YEAR, 1) } as Calendar }
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
    onNext: () -> Unit
) {
    val cookieShape = MaterialShapes.Cookie12Sided.toShape()
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(28.dp), // Expressive larger corners
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
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
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
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
    val surfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)

    val progress = remember(dataPoints) { Animatable(0f) }

    LaunchedEffect(progress) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600)
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

        dataPoints.forEachIndexed { index, value ->
            val x = index * xFactor
            val y = size.height - (value * yFactor)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        clipRect(right = size.width * progress.value) {
            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(
                    width = 4.dp.toPx(), 
                    cap = StrokeCap.Round, 
                    join = StrokeJoin.Round
                )
            )

            dataPoints.forEachIndexed { index, value ->
                val x = index * xFactor
                val y = size.height - (value * yFactor)

                drawCircle(
                    color = Color.White.copy(alpha = 0.5f),
                    radius = 6.dp.toPx(),
                    center = Offset(x, y)
                )
                drawCircle(
                    color = primaryColor,
                    radius = 4.dp.toPx(),
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

    val dayEnd = dayStart.clone() as Calendar
    dayEnd.add(Calendar.DAY_OF_YEAR, 1)

    val dayEntries = entries.filter { it >= dayStart.timeInMillis && it < dayEnd.timeInMillis }
    val hourlyCounts = IntArray(24) { 0 }
    
    dayEntries.forEach { time ->
        val c = Calendar.getInstance().apply { timeInMillis = time }
        hourlyCounts[c.get(Calendar.HOUR_OF_DAY)]++
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

    val dailyCounts = IntArray(7) { 0 }
    for (i in 0 until 7) {
        val currentDayStart = weekStart.clone() as Calendar
        currentDayStart.add(Calendar.DAY_OF_YEAR, i)
        val currentDayEnd = currentDayStart.clone() as Calendar
        currentDayEnd.add(Calendar.DAY_OF_YEAR, 1)
        
        dailyCounts[i] = entries.count { it >= currentDayStart.timeInMillis && it < currentDayEnd.timeInMillis }
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
    
    val daysInMonth = monthStart.getActualMaximum(Calendar.DAY_OF_MONTH)
    val dailyCounts = IntArray(daysInMonth) { 0 }
    
    for (i in 0 until daysInMonth) {
         val currentDayStart = monthStart.clone() as Calendar
         currentDayStart.add(Calendar.DAY_OF_MONTH, i)
         val currentDayEnd = currentDayStart.clone() as Calendar
         currentDayEnd.add(Calendar.DAY_OF_MONTH, 1)
         
         dailyCounts[i] = entries.count { it >= currentDayStart.timeInMillis && it < currentDayEnd.timeInMillis }
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

    val monthlyCounts = IntArray(12) { 0 }
    for (i in 0 until 12) {
        val currentMonthStart = yearStart.clone() as Calendar
        currentMonthStart.set(Calendar.MONTH, i)
        val currentMonthEnd = currentMonthStart.clone() as Calendar
        currentMonthEnd.add(Calendar.MONTH, 1)
        
        monthlyCounts[i] = entries.count { it >= currentMonthStart.timeInMillis && it < currentMonthEnd.timeInMillis }
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
