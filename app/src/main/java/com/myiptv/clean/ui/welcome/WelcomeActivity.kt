package com.myiptv.clean.ui.welcome

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.myiptv.clean.R
import com.myiptv.clean.ui.compose.CleanIPTVTheme
import com.myiptv.clean.ui.compose.IptvActivity
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
    var buttonFocused by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val uiMode = LocalConfiguration.current.uiMode
    val isTelevision = remember(context, uiMode) { context.isTelevisionDevice(uiMode) }
    val background = MaterialTheme.colorScheme.background
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(background)) {
        val isTv = isTelevision || maxWidth >= 840.dp
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(Brush.linearGradient(listOf(Color(0xFF081722), background, Color(0xFF09181C))))
            drawCircle(Color(0xFF38BDF8).copy(alpha = 0.12f), radius = size.maxDimension * 0.35f, center = Offset(size.width * 0.88f, size.height * 0.16f))
            drawCircle(Color(0xFF6DE3CF).copy(alpha = 0.11f), radius = size.maxDimension * 0.3f, center = Offset(size.width * 0.12f, size.height * 0.92f))
            drawCircle(Color.White.copy(alpha = 0.14f), radius = size.minDimension * 0.18f, center = Offset(size.width * 0.83f, size.height * 0.22f), style = Stroke(width = 1.5.dp.toPx()))
            for (index in 0..9) {
                drawLine(Color.White.copy(alpha = 0.035f), Offset(size.width * index / 10f, 0f), Offset(size.width * (index + 1) / 10f, size.height), 1.dp.toPx())
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (isTv) 80.dp else 26.dp, vertical = if (isTv) 22.dp else 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(if (isTv) 64.dp else 80.dp).clip(RoundedCornerShape(28.dp)).background(
                    Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, Color(0xFF116B99)))
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.LiveTv, contentDescription = "XUPER TV", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(if (isTv) 32.dp else 40.dp))
            }
            Spacer(Modifier.height(if (isTv) 8.dp else 18.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("XUPER", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, letterSpacing = 1.sp)
                Text("TV", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(if (isTv) 10.dp else 18.dp))
            Text(
                "Tu contenido,\na tus fuentes.",
                modifier = Modifier.fillMaxWidth(if (isTv) 0.75f else 1f),
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = if (isTv) 40.sp else 43.sp
            )
            Spacer(Modifier.height(if (isTv) 8.dp else 12.dp))
            Text(
                "Canales de TV en directo de distintos países, metadatos de películas y series y enlaces que configuras en tu dispositivo.",
                modifier = Modifier.fillMaxWidth(if (isTv) 0.58f else 0.94f),
                color = Color.White.copy(alpha = 0.73f),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(if (isTv) 12.dp else 22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                WelcomeFeature("TV en directo")
                WelcomeFeature("TMDb")
                WelcomeFeature("Directo / iframe")
            }
            Spacer(Modifier.height(if (isTv) 16.dp else 30.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth(if (isTv) 0.38f else 0.82f)
                    .height(if (isTv) 48.dp else 54.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged { buttonFocused = it.isFocused }
                    .border(if (buttonFocused) 2.dp else 0.dp, if (buttonFocused) MaterialTheme.colorScheme.secondary else Color.Transparent, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text("Explorar contenido", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(if (isTv) 8.dp else 15.dp))
            Text(
                "La reproducción requiere enlaces públicos y autorizados.",
                modifier = Modifier.fillMaxWidth(if (isTv) 0.68f else 1f),
                color = Color.White.copy(alpha = 0.52f),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center
            )
        }
    }
    LaunchedEffect(Unit) {
        delay(180)
        runCatching { focusRequester.requestFocus() }
    }
}

@Composable
private fun WelcomeFeature(label: String) {
    Surface(
        modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(50)),
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.06f)
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = Color.White.copy(alpha = 0.86f), style = MaterialTheme.typography.labelMedium)
    }
}
