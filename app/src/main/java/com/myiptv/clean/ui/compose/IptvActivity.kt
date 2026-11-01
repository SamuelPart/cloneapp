package com.myiptv.clean.ui.compose

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.myiptv.clean.R
import com.myiptv.clean.notifications.CatalogNotificationRequests
import com.myiptv.clean.notifications.CatalogNotificationTarget
import com.myiptv.clean.notifications.CatalogSyncScheduler
import com.myiptv.clean.notifications.EXTRA_CATALOG_MEDIA_ID
import com.myiptv.clean.update.AppUpdateSyncScheduler
import com.myiptv.clean.update.EXTRA_OPEN_APP_UPDATES

class IptvActivity : AppCompatActivity() {
    private val notificationRequests = CatalogNotificationRequests()
    private var notificationTarget by mutableStateOf<CatalogNotificationTarget?>(null)
    private var updateRequest by mutableStateOf<Long?>(null)
    private var updateRequestSequence = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        receiveIntent(intent)
        CatalogSyncScheduler.schedule(applicationContext)
        AppUpdateSyncScheduler.schedule(applicationContext)
        val background = ContextCompat.getColor(this, R.color.background)
        val isTelevision = isTelevisionDevice()
        window.statusBarColor = background
        window.navigationBarColor = background
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
            if (isTelevision) {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        setContent {
            CleanIPTVTheme {
                IptvApp(
                    notificationTarget = notificationTarget,
                    onNotificationHandled = { handledSequence ->
                        // A newer tap may arrive while the previous request is being navigated.
                        if (notificationRequests.consume(handledSequence)) {
                            notificationTarget = null
                            intent?.removeExtra(EXTRA_CATALOG_MEDIA_ID)
                        }
                    },
                    updateRequest = updateRequest,
                    onUpdateRequestHandled = { handledSequence ->
                        if (updateRequest == handledSequence) {
                            updateRequest = null
                            intent?.removeExtra(EXTRA_OPEN_APP_UPDATES)
                        }
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        receiveIntent(intent)
    }

    private fun receiveIntent(intent: Intent?) {
        notificationRequests.accept(intent?.getStringExtra(EXTRA_CATALOG_MEDIA_ID))?.let {
            updateRequest = null // The latest tapped notification takes precedence.
            notificationTarget = it
        }
        if (intent?.getBooleanExtra(EXTRA_OPEN_APP_UPDATES, false) == true) {
            notificationTarget?.let { notificationRequests.consume(it.sequence) }
            notificationTarget = null
            updateRequestSequence += 1
            updateRequest = updateRequestSequence
        }
    }
}
