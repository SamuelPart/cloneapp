package com.myiptv.clean.ui.welcome

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.myiptv.clean.R
import com.myiptv.clean.ui.compose.CleanIPTVTheme
import com.myiptv.clean.ui.compose.IptvActivity
import com.myiptv.clean.ui.compose.TvActionButton
import com.myiptv.clean.ui.compose.TvBrandLogo
import com.myiptv.clean.ui.compose.isTelevisionDevice
import kotlinx.coroutines.delay

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                WelcomeScreen(onContinue = {
                    startActivity(Intent(this, IptvActivity::class.java))
                    finish()
                })
            }
        }
    }
}

@Composable
private fun WelcomeScreen(onContinue: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val uiMode = LocalConfiguration.current.uiMode
    val isTelevision = remember(context, uiMode) { context.isTelevisionDevice(uiMode) }
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        val isTv = isTelevision || maxWidth >= 840.dp
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = if (isTv) 30.dp else 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (isTv) 12.dp else 16.dp)
            ) {
                TvBrandLogo(Modifier.width(170.dp).height(86.dp))
                Text(
                    "TU CONTENIDO, A TUS FUENTES", color = Color.White,
                    style = if (isTv) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center
                )
                Text(
                    "Canales de TV en directo, metadatos de películas y series, y enlaces de tu catálogo M3U.",
                    modifier = Modifier.fillMaxWidth(if (isTv) 0.72f else 1f), color = Color.White.copy(alpha = 0.68f),
                    style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center
                )
                TvActionButton("Explorar contenido", onContinue, modifier = Modifier.focusRequester(focusRequester), primary = true)
                Text("La reproducción requiere enlaces públicos y autorizados.", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
            }
        }
    }
    LaunchedEffect(isTelevision) {
        if (isTelevision) {
            delay(180)
            runCatching { focusRequester.requestFocus() }
        }
    }
}
