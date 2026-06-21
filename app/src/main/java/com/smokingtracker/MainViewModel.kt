package com.smokingtracker

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.smokingtracker.data.DataStoreManager
import com.smokingtracker.data.ThemePreference
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.Calendar

class MainViewModel(private val dataStoreManager: DataStoreManager, private val context: Context) : ViewModel() {

    private val gson = Gson()

    val isRegistered: StateFlow<Boolean?> = dataStoreManager.isRegistered.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val smokingEntries: StateFlow<List<Long>> = dataStoreManager.smokingEntries.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val themePreference: StateFlow<ThemePreference> = dataStoreManager.appTheme.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThemePreference.SYSTEM
    )

    val unlockedAchievements: StateFlow<Set<String>> = dataStoreManager.unlockedAchievements.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )
    
    val dailyLimit: StateFlow<Int> = dataStoreManager.dailyLimit.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    init {
        //generateTestStatistics()

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val launches = dataStoreManager.appLaunchDates.first()
            val calNow = Calendar.getInstance().apply { timeInMillis = now }
            val todayStr = "${calNow.get(Calendar.YEAR)}-${calNow.get(Calendar.DAY_OF_YEAR)}"
            
            val alreadyLoggedToday = launches.any {
                val cal = Calendar.getInstance().apply { timeInMillis = it }
                "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}" == todayStr
            }

            if (!alreadyLoggedToday) {
                dataStoreManager.recordAppLaunch(now)
            }
            checkAchievements()
        }

        viewModelScope.launch {
            while (true) {
                checkAchievements()
                delay(60000)
            }
        }
    }

    private suspend fun checkAchievements() {
        val entries = dataStoreManager.smokingEntries.first()
        val launches = dataStoreManager.appLaunchDates.first()
        val previouslyUnlocked = dataStoreManager.unlockedAchievements.first()
        
        val newUnlockedSet = AchievementsManager.calculateUnlockedAchievements(entries, launches)

        val newlyUnlocked = newUnlockedSet - previouslyUnlocked
        newlyUnlocked.forEach { achievementId ->
            AchievementsManager.sendNotificationForAchievement(context, achievementId)
        }

        dataStoreManager.setUnlockedAchievements(newUnlockedSet)
    }

    fun registerUser() {
        viewModelScope.launch {
            dataStoreManager.saveUserProfile()
        }
    }

    fun addSmokingEntry(timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            dataStoreManager.addSmokingEntry(timestamp)
            checkAchievements()
        }
    }

    fun removeSmokingEntry(timestamp: Long) {
        viewModelScope.launch {
            dataStoreManager.removeSmokingEntry(timestamp)
            checkAchievements()
        }
    }

    fun updateThemePreference(theme: ThemePreference) {
        viewModelScope.launch {
            dataStoreManager.saveThemePreference(theme)
        }
    }
    
    fun setDailyLimit(limit: Int) {
        viewModelScope.launch {
            dataStoreManager.setDailyLimit(limit)
        }
    }
    
    data class BackupData(
        val isRegistered: Boolean,
        val smokingEntries: List<Long>,
        val appTheme: String,
        val unlockedAchievements: Set<String>,
        val dailyLimit: Int? = 0
    )

    fun backupData(uri: Uri, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                val data = BackupData(
                    isRegistered = dataStoreManager.isRegistered.first() ?: false,
                    smokingEntries = dataStoreManager.smokingEntries.first(),
                    appTheme = dataStoreManager.appTheme.first().name,
                    unlockedAchievements = dataStoreManager.unlockedAchievements.first(),
                    dailyLimit = dataStoreManager.dailyLimit.first()
                )
                
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        gson.toJson(data, writer)
                    }
                }
                onSuccess()
            } catch (e: Exception) {
                onError()
            }
        }
    }

    fun restoreData(uri: Uri, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    InputStreamReader(inputStream).use { reader ->
                        val data = gson.fromJson(reader, BackupData::class.java)
                        if (data != null) {
                            dataStoreManager.restoreFromBackup(
                                isReg = data.isRegistered,
                                entries = data.smokingEntries,
                                theme = data.appTheme,
                                achievements = data.unlockedAchievements,
                                limit = data.dailyLimit ?: 0
                            )
                            onSuccess()
                        } else {
                            onError()
                        }
                    }
                } ?: onError()
            } catch (e: Exception) {
                onError()
            }
        }
    }

//    fun generateTestStatistics() {
//        viewModelScope.launch {
//            val calendar = java.util.Calendar.getInstance()
//            calendar.set(2023, java.util.Calendar.JANUARY, 1, 0, 0, 0)
//            calendar.set(java.util.Calendar.MILLISECOND, 0)
//
//            val endDate = java.util.Calendar.getInstance()
//            endDate.set(2026, java.util.Calendar.MAY, 31, 23, 59, 59)
//
//            val allEntries = mutableListOf<Long>()
//            val random = java.util.Random()
//
//            while (calendar.before(endDate)) {
//                val chance = random.nextFloat()
//                val count = when {
//                    chance < 0.05f -> 0
//                    chance < 0.15f -> random.nextInt(2) + 6
//                    else -> random.nextInt(4) + 2
//                }
//
//                val currentDayStart = calendar.timeInMillis
//                repeat(count) {
//                    val randomTimeOffset = (random.nextFloat() * 24 * 60 * 60 * 1000L).toLong()
//                    allEntries.add(currentDayStart + randomTimeOffset)
//                }
//                calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
//            }
//
//            allEntries.sort()
//
//            dataStoreManager.restoreFromBackup(
//                isReg = true,
//                entries = allEntries,
//                theme = com.smokingtracker.data.ThemePreference.SYSTEM.name,
//                achievements = emptySet(),
//                limit = 10
//            )
//        }
//    }
}
