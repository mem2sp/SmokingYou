package com.smokingtracker.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object WidgetUpdateManager {

    fun updateAll(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        val quickAddComponent = ComponentName(context, QuickAddWidgetProvider::class.java)
        val quickAddIds = appWidgetManager.getAppWidgetIds(quickAddComponent)
        if (quickAddIds.isNotEmpty()) {
            QuickAddWidgetProvider.updateAppWidgets(context, appWidgetManager, quickAddIds)
        }

        val timerComponent = ComponentName(context, TimerWidgetProvider::class.java)
        val timerIds = appWidgetManager.getAppWidgetIds(timerComponent)
        if (timerIds.isNotEmpty()) {
            TimerWidgetProvider.updateAppWidgets(context, appWidgetManager, timerIds)
        }
    }

    fun updateAllAsync(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            updateAll(context)
        }
    }
}
