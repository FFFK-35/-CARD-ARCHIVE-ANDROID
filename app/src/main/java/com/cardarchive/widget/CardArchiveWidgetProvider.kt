package com.cardarchive.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class CardArchiveWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateWidget(context, manager, it) }
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, CardArchiveWidgetProvider::class.java))
            ids.forEach { updateWidget(context, manager, it) }
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val state = CheckInStore.read(context)
            val views = RemoteViews(context.packageName, R.layout.widget_card_archive)
            views.setTextViewText(R.id.widget_status, if (state.isCheckedInToday) "今日已完成" else "待打卡")
            views.setTextViewText(R.id.widget_progress, "阶段进度 ${state.stageProgress} / 3")
            views.setProgressBar(R.id.widget_progress_bar, 3, state.stageProgress, false)
            views.setTextViewText(R.id.widget_coins, "金币 ${state.coins}")
            views.setTextViewText(R.id.widget_total, "累计 ${state.totalCheckIns} 次")
            views.setTextViewText(R.id.widget_check_in, if (state.isCheckedInToday) "今日已完成" else context.getString(R.string.check_in))
            val action = PendingIntent.getBroadcast(context, id, Intent(context, CheckInReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_check_in, action)
            val open = PendingIntent.getActivity(context, id + 10000, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, open)
            manager.updateAppWidget(id, views)
        }
    }
}
