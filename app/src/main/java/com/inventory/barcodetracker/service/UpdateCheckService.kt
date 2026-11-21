package com.inventory.barcodetracker.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val hasUpdate: Boolean,
    val latestTag: String = "",
    val releaseNotes: String = "",
    val downloadUrl: String = "https://github.com/gabrieldeak/shopping-tracker/releases/latest"
)

object UpdateCheckService {

    private const val GITHUB_API_URL = "https://api.github.com/repos/gabrieldeak/shopping-tracker/releases/latest"
    const val CURRENT_VERSION = "3.0.3"

    suspend fun checkForUpdates(): AppUpdateInfo {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(GITHUB_API_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                connection.setRequestProperty("User-Agent", "ShoppingTracker-Android")

                if (connection.responseCode == 200) {
                    val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(jsonStr)

                    val tagName = json.optString("tag_name", "").removePrefix("v").trim()
                    val notes = json.optString("body", "")
                    val htmlUrl = json.optString("html_url", "https://github.com/gabrieldeak/shopping-tracker/releases/latest")

                    if (tagName.isNotBlank() && isVersionHigher(tagName, CURRENT_VERSION)) {
                        AppUpdateInfo(
                            hasUpdate = true,
                            latestTag = "v$tagName",
                            releaseNotes = notes,
                            downloadUrl = htmlUrl
                        )
                    } else {
                        AppUpdateInfo(hasUpdate = false)
                    }
                } else {
                    AppUpdateInfo(hasUpdate = false)
                }
            } catch (_: Exception) {
                AppUpdateInfo(hasUpdate = false)
            }
        }
    }

    private fun isVersionHigher(latest: String, current: String): Boolean {
        try {
            val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
            val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }

            val maxLen = maxOf(latestParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val l = latestParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (l > c) return true
                if (l < c) return false
            }
        } catch (_: Exception) {}
        return false
    }
}
