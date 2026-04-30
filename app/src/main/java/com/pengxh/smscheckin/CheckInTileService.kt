package com.pengxh.smscheckin

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log

class CheckInTileService : TileService() {

    companion object {
        private const val TAG = "CheckInTileService"
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val prefs = getSharedPreferences("sms_checkin_prefs", MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("service_enabled", false)

        if (isEnabled) {
            disableService()
        } else {
            enableService()
        }
    }

    private fun updateTile() {
        val prefs = getSharedPreferences("sms_checkin_prefs", MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("service_enabled", false)

        qsTile.apply {
            state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = getString(R.string.app_name)
            updateTile()
        }
    }

    private fun enableService() {
        Log.d(TAG, "启用服务")
        val prefs = getSharedPreferences("sms_checkin_prefs", MODE_PRIVATE)
        prefs.edit().putBoolean("service_enabled", true).apply()
        SmsReceiver.isEnabled = true

        val serviceIntent = Intent(this, CheckInForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        DailyReportReceiver.scheduleDailyReport(this)
        KeepAliveWorker.schedule(this)
        ProactiveTriggerReceiver.scheduleAll(this)

        updateTile()
    }

    private fun disableService() {
        Log.d(TAG, "禁用服务")
        val prefs = getSharedPreferences("sms_checkin_prefs", MODE_PRIVATE)
        prefs.edit().putBoolean("service_enabled", false).apply()
        SmsReceiver.isEnabled = false

        stopService(Intent(this, CheckInForegroundService::class.java))
        DailyReportReceiver.cancelDailyReport(this)
        KeepAliveWorker.cancel(this)
        ProactiveTriggerReceiver.cancelAll(this)

        updateTile()
    }
}
