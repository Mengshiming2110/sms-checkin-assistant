package com.pengxh.smscheckin

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object UpdateChecker {

    private const val GITHUB_API = "https://api.github.com/repos/Mengshiming2110/sms-checkin-assistant/releases/latest"
    private var downloadId: Long = -1

    data class UpdateInfo(
        val version: String,
        val changelog: String,
        val downloadUrl: String
    )

    fun checkForUpdate(context: Context, onResult: (Result<UpdateInfo>) -> Unit) {
        thread {
            try {
                val url = URL(GITHUB_API)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.setRequestProperty("Accept", "application/vnd.github+json")

                val code = conn.responseCode
                if (code != 200) {
                    throw RuntimeException("HTTP $code")
                }

                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val json = reader.readText()
                reader.close()
                conn.disconnect()

                val root = JSONObject(json)
                val tagName = root.getString("tag_name")
                val body = root.optString("body", "").take(500)
                val assets = root.getJSONArray("assets")
                if (assets.length() == 0) {
                    throw RuntimeException("No APK asset found")
                }
                val downloadUrl = assets.getJSONObject(0).getString("browser_download_url")

                val latestVersion = tagName.trimStart('v')
                val pi = context.packageManager.getPackageInfo(context.packageName, 0)
                val currentVersion = pi.versionName ?: "1.0"

                if (isNewer(latestVersion, currentVersion)) {
                    onResult(Result.success(UpdateInfo(latestVersion, body, downloadUrl)))
                } else {
                    onResult(Result.failure(AlreadyLatestException()))
                }
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun downloadAndInstall(context: Context, url: String) {
        val apkFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
        apkFile.delete()

        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(context.getString(R.string.update_download_title))
            setDescription(context.getString(R.string.update_downloading))
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationUri(Uri.fromFile(apkFile))
        }

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = manager.enqueue(request)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        ctx.unregisterReceiver(this)
                        installApk(ctx, apkFile)
                    }
                }
            }
        }

        context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED)
    }

    fun installApk(context: Context, file: File) {
        if (!file.exists()) {
            Toast.makeText(context, R.string.update_download_failed, Toast.LENGTH_SHORT).show()
            return
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, R.string.update_download_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun isNewer(latest: String, current: String): Boolean {
        val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    class AlreadyLatestException : Exception()
}
