package com.myiptv.clean.config

import android.content.Context
import com.google.gson.Gson
import java.io.File

class ConfigManager(private val context: Context) {
    
    private val gson = Gson()
    private val configFile = File(context.filesDir, "domain_config.json")
    
    fun loadConfig(): DomainConfig {
        return try {
            if (configFile.exists()) {
                val json = configFile.readText()
                gson.fromJson(json, DomainConfig::class.java)
            } else {
                loadDefaultConfig()
            }
        } catch (e: Exception) {
            loadDefaultConfig()
        }
    }
    
    fun saveConfig(config: DomainConfig) {
        try {
            val json = gson.toJson(config)
            configFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun loadDefaultConfig(): DomainConfig {
        return DomainConfig(
            portalMain = "https://tu-servidor.com/api",
            portalBackup = "https://tu-servidor-backup.com/api",
            epgMain = "https://tu-servidor.com/epg",
            epgBackup = "https://tu-servidor-backup.com/epg",
            dccoreMain = "https://tu-servidor.com/core",
            dccoreBackup = "https://tu-servidor-backup.com/core",
            enableLive = true,
            enableVOD = true,
            enableDownloads = true,
            enableFavorites = true,
            version = "1.0.0"
        )
    }
}

data class DomainConfig(
    val portalMain: String,
    val portalBackup: String,
    val epgMain: String,
    val epgBackup: String,
    val dccoreMain: String,
    val dccoreBackup: String,
    val enableLive: Boolean,
    val enableVOD: Boolean,
    val enableDownloads: Boolean,
    val enableFavorites: Boolean,
    val version: String
)
