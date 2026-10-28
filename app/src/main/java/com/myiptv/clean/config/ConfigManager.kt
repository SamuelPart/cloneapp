package com.myiptv.clean.config

import android.content.Context
import com.google.gson.Gson
import java.io.File

class ConfigManager(context: Context) {

    private val gson = Gson()
    private val configFile = File(context.filesDir, CONFIG_FILE_NAME)

    fun loadConfig(): DomainConfig {
        if (!configFile.exists()) return DomainConfig()

        return runCatching {
            gson.fromJson(configFile.readText(), DomainConfig::class.java) ?: DomainConfig()
        }.getOrDefault(DomainConfig())
    }

    fun saveConfig(config: DomainConfig) {
        runCatching {
            configFile.writeText(gson.toJson(config))
        }
    }

    private companion object {
        const val CONFIG_FILE_NAME = "domain_config.json"
    }
}

data class DomainConfig(
    val portalMain: String = "https://example.invalid/api/",
    val portalBackup: String = "https://backup.example.invalid/api/",
    val epgMain: String = "https://example.invalid/epg/",
    val epgBackup: String = "https://backup.example.invalid/epg/",
    val dccoreMain: String = "https://example.invalid/core/",
    val dccoreBackup: String = "https://backup.example.invalid/core/",
    val enableLive: Boolean = true,
    val enableVOD: Boolean = true,
    val enableDownloads: Boolean = false,
    val enableFavorites: Boolean = true,
    val version: String = "1.0.0"
)
