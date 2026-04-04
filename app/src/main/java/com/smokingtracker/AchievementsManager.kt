package com.smokingtracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.Calendar
import java.util.concurrent.TimeUnit

enum class AchievementCategory(val titleResId: Int) {
    LOGIN(R.string.ach_category_login),
    NO_SMOKE(R.string.ach_category_nosmoke)
}

data class Achievement(
    val id: String,
    val titleResId: Int,
    val descResId: Int,
    val category: AchievementCategory,
    val condition: (Long, List<Long>, List<Long>) -> Boolean // (timeWithoutSmoking, entries, launches)
)

object AchievementsManager {
    private const val CHANNEL_ID = "achievements_channel"
    private const val CHANNEL_NAME = "Achievements"
    
    val achievementsList = listOf(
        Achievement("login_1", R.string.ach_curiosity_title, R.string.ach_curiosity_desc, AchievementCategory.LOGIN) { _, _, launches -> 
            launches.isNotEmpty() 
        },
        Achievement("login_3", R.string.ach_interest_title, R.string.ach_interest_desc, AchievementCategory.LOGIN) { _, _, launches -> 
            hasConsecutiveDays(launches, 3)
        },
        Achievement("login_7", R.string.ach_exploration_title, R.string.ach_exploration_desc, AchievementCategory.LOGIN) { _, _, launches -> 
            hasConsecutiveDays(launches, 7)
        },
        Achievement("login_30", R.string.ach_discipline_title, R.string.ach_discipline_desc, AchievementCategory.LOGIN) { _, _, launches -> 
            hasConsecutiveDays(launches, 30)
        },
        Achievement("login_365", R.string.ach_statistics_title, R.string.ach_statistics_desc, AchievementCategory.LOGIN) { _, _, launches -> 
            hasConsecutiveDays(launches, 365)
        },

        Achievement("nosmoke_1d", R.string.ach_nosmoke_1d_title, R.string.ach_nosmoke_1d_desc, AchievementCategory.NO_SMOKE) { timeWithoutSmoking, _, _ -> 
            timeWithoutSmoking >= TimeUnit.DAYS.toMillis(1)
        },
        Achievement("nosmoke_3d", R.string.ach_nosmoke_3d_title, R.string.ach_nosmoke_3d_desc, AchievementCategory.NO_SMOKE) { timeWithoutSmoking, _, _ -> 
            timeWithoutSmoking >= TimeUnit.DAYS.toMillis(3)
        },
        Achievement("nosmoke_1w", R.string.ach_nosmoke_1w_title, R.string.ach_nosmoke_1w_desc, AchievementCategory.NO_SMOKE) { timeWithoutSmoking, _, _ -> 
            timeWithoutSmoking >= TimeUnit.DAYS.toMillis(7)
        },
        Achievement("nosmoke_1m", R.string.ach_nosmoke_1m_title, R.string.ach_nosmoke_1m_desc, AchievementCategory.NO_SMOKE) { timeWithoutSmoking, _, _ -> 
            timeWithoutSmoking >= TimeUnit.DAYS.toMillis(30)
        },
        Achievement("nosmoke_1y", R.string.ach_nosmoke_1y_title, R.string.ach_nosmoke_1y_desc, AchievementCategory.NO_SMOKE) { timeWithoutSmoking, _, _ -> 
            timeWithoutSmoking >= TimeUnit.DAYS.toMillis(365)
        }
    )

    private fun hasConsecutiveDays(dates: List<Long>, requiredConsecutiveDays: Int): Boolean {
        if (dates.isEmpty()) return false

        val sortedDays = dates.map {
            val cal = Calendar.getInstance().apply { timeInMillis = it }
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.distinct().sorted()

        var currentStreak = 1
        var maxStreak = 1

        for (i in 1 until sortedDays.size) {
            val diff = sortedDays[i] - sortedDays[i - 1]
            val daysDiff = TimeUnit.MILLISECONDS.toDays(diff)
            if (daysDiff == 1L) {
                currentStreak++
                if (currentStreak > maxStreak) maxStreak = currentStreak
            } else if (daysDiff > 1L) {
                currentStreak = 1
            }
        }

        return maxStreak >= requiredConsecutiveDays
    }

    fun calculateUnlockedAchievements(entries: List<Long>, launches: List<Long>): Set<String> {
        val lastEntry = entries.maxOrNull()
        val now = System.currentTimeMillis()
        val timeWithoutSmoking = if (lastEntry != null) {
            now - lastEntry
        } else if (launches.isNotEmpty()) {
            now - launches.minOrNull()!!
        } else {
            0L
        }

        val unlocked = mutableSetOf<String>()
        achievementsList.forEach { achievement ->
            if (achievement.condition(timeWithoutSmoking, entries, launches)) {
                unlocked.add(achievement.id)
            }
        }
        return unlocked
    }

    fun sendNotificationForAchievement(context: Context, achievementId: String) {
        val achievement = achievementsList.find { it.id == achievementId } ?: return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        createNotificationChannel(context)

        val title = context.getString(achievement.titleResId)
        val desc = context.getString(achievement.descResId)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_cigarette) // Use app icon as notification icon
            .setContentTitle(context.getString(R.string.notification_title, title))
            .setContentText(desc)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(achievement.id.hashCode(), builder.build())
            } catch (e: SecurityException) {
            }
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val descriptionText = "Notifications for unlocked achievements"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
