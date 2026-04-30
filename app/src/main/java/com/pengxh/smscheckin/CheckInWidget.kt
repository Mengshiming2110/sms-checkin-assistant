package com.pengxh.smscheckin

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class CheckInWidget : AppWidgetProvider() {

    companion object {
        private const val ACTION_TOGGLE_SERVICE = "com.pengxh.smscheckin.TOGGLE_SERVICE"
        private const val ACTION_OPEN_APP = "com.pengxh.smscheckin.OPEN_APP"

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            val prefs = context.getSharedPreferences("sms_checkin_prefs", Context.MODE_PRIVATE)
            val isEnabled = prefs.getBoolean("service_enabled", false)
            val todayCount = SmsReceiver.getTodayCount(context)

            val views = RemoteViews(context.packageName, R.layout.widget_checkin)

            views.setTextViewText(R.id.widgetCountText, todayCount.toString())
            views.setTextViewText(
                R.id.widgetStatusText,
                if (isEnabled) context.getString(R.string.service_running) else context.getString(R.string.service_stopped)
            )

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context, widgetId, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetCountText, openAppPendingIntent)

            val toggleIntent = Intent(context, CheckInWidget::class.java).apply {
                action = ACTION_TOGGLE_SERVICE
            }
            val togglePendingIntent = PendingIntent.getBroadcast(
                context, widgetId, toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetToggleBtn, togglePendingIntent)

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_TOGGLE_SERVICE -> {
                val prefs = context.getSharedPreferences("sms_checkin_prefs", Context.MODE_PRIVATE)
                val isEnabled = prefs.getBoolean("service_enabled", false)

                if (isEnabled) {
                    SmsReceiver.isEnabled = false
                    prefs.edit().putBoolean("service_enabled", false).apply()
                    context.stopService(Intent(context, CheckInForegroundService::class.java))
                    DailyReportReceiver.cancelDailyReport(context)
                    KeepAliveWorker.cancel(context)
                    MissedCheckInReceiver.cancel(context)
                    ProactiveTriggerReceiver.cancelAll(context)
                } else {
                    SmsReceiver.isEnabled = true
                    prefs.edit().putBoolean("service_enabled", true).apply()
                    context.startForegroundService(Intent(context, CheckInForegroundService::class.java))
                    DailyReportReceiver.scheduleDailyReport(context)
                    KeepAliveWorker.schedule(context)
                    MissedCheckInReceiver.schedule(context)
                    ProactiveTriggerReceiver.scheduleAll(context)
                }

                val appWidgetManager = AppWidgetManager.getInstance(context)
                val widgetIds = appWidgetManager.getAppWidgetIds(
                    android.content.ComponentName(context, CheckInWidget::class.java)
                )
                for (widgetId in widgetIds) {
                    updateWidget(context, appWidgetManager, widgetId)
                }
            }
        }
    }
}
