package com.myiptv.clean.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.myiptv.clean.BuildConfig

@Composable
internal fun AppUpdateSettings() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Versión instalada: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            color = TvMuted, style = MaterialTheme.typography.bodySmall)
        Text("Esta versión se actualiza desde Google Play. El catálogo de películas y series se actualiza por separado.",
            color = TvMuted, style = MaterialTheme.typography.bodySmall)
    }
}
