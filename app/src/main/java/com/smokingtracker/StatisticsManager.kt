package com.smokingtracker

import java.util.*
import java.util.concurrent.TimeUnit

data class StatisticsData(
    val maxPerDay: Int,
    val minPerDay: Int,
    val avgPerDay: Int,
    val totalCount: Int,
    val trackingSince: Long?,
    val longestStreakDays: Int
)

object StatisticsManager {

    fun calculateStats(entries: List<Long>): StatisticsData {
        if (entries.isEmpty()) {
            return StatisticsData(0, 0, 0, 0, null, 0)
        }

        val sortedEntries = entries.sorted()
        val totalCount = entries.size
        val trackingSince = sortedEntries.first()

        val dailyCounts = mutableMapOf<String, Int>()
        val calendar = Calendar.getInstance()

        entries.forEach { timestamp ->
            calendar.timeInMillis = timestamp
            val dayKey = "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.DAY_OF_YEAR)}"
            dailyCounts[dayKey] = (dailyCounts[dayKey] ?: 0) + 1
        }

        val maxPerDay = dailyCounts.values.maxOrNull() ?: 0
        val minPerDay = dailyCounts.values.minOrNull() ?: 0

        val today = Calendar.getInstance()
        val firstEntryDate = Calendar.getInstance().apply { timeInMillis = trackingSince }

        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        
        val firstDay = firstEntryDate.clone() as Calendar
        firstDay.set(Calendar.HOUR_OF_DAY, 0)
        firstDay.set(Calendar.MINUTE, 0)
        firstDay.set(Calendar.SECOND, 0)
        firstDay.set(Calendar.MILLISECOND, 0)

        val diffMillis = today.timeInMillis - firstDay.timeInMillis
        val diffDays = (TimeUnit.MILLISECONDS.toDays(diffMillis) + 1).toInt()

        val avgPerDay = Math.round(totalCount.toDouble() / diffDays.coerceAtLeast(1)).toInt()

        val longestStreak = calculateLongestStreak(sortedEntries)

        return StatisticsData(
            maxPerDay = maxPerDay,
            minPerDay = minPerDay,
            avgPerDay = avgPerDay,
            totalCount = totalCount,
            trackingSince = trackingSince,
            longestStreakDays = longestStreak
        )
    }

    private fun calculateLongestStreak(sortedEntries: List<Long>): Int {
        if (sortedEntries.isEmpty()) return 0

        val calendar = Calendar.getInstance()
        val entryDays = sortedEntries.map {
            calendar.timeInMillis = it
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }.distinct().sorted()

        var maxStreak = 0

        for (i in 0 until entryDays.size - 1) {
            val gapDays = TimeUnit.MILLISECONDS.toDays(entryDays[i+1] - entryDays[i]).toInt() - 1
            if (gapDays > maxStreak) {
                maxStreak = gapDays
            }
        }

        val lastEntry = entryDays.last()
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val currentGap = TimeUnit.MILLISECONDS.toDays(today - lastEntry).toInt()
        if (currentGap > maxStreak) {
            maxStreak = currentGap
        }

        return maxStreak
    }
}
