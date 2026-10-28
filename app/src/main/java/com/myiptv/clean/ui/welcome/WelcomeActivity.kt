package com.myiptv.clean.ui.welcome

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myiptv.clean.ui.compose.CleanIPTVTheme
import com.myiptv.clean.ui.compose.IptvActivity
import kotlinx.coroutines.delay

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(9, 13, 20)
        window.navigationBarColor = android.graphics.Color.rgb(9, 13, 20)
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
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color(0xFF090D14))) {
        val isTv = maxWidth >= 840.dp
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(Brush.linearGradient(listOf(Color(0xFF10172A), Color(0xFF090D14), Color(0xFF101C25))))
            drawCircle(Color(0xFF9870FF).copy(alpha = 0.18f), radius = size.maxDimension * 0.35f, center = Offset(size.width * 0.88f, size.height * 0.16f))
            drawCircle(Color(0xFF6DE3CF).copy(alpha = 0.11f), radius = size.maxDimension * 0.3f, center = Offset(size.width * 0.12f, size.height * 0.92f))
            drawCircle(Color.White.copy(alpha = 0.14f), radius = size.minDimension * 0.18f, center = Offset(size.width * 0.83f, size.height * 0.22f), style = Stroke(width = 1.5.dp.toPx()))
            for (index in 0..9) {
                drawLine(Color.White.copy(alpha = 0.035f), Offset(size.width * index / 10f, 0f), Offset(size.width * (index + 1) / 10f, size.height), 1.dp.toPx())
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isTv) 80.dp else 26.dp, vertical = if (isTv) 48.dp else 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(if (isTv) 96.dp else 80.dp).clip(RoundedCornerShape(28.dp)).background(
                    Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, Color(0xFF7252D2)))
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.LiveTv, contentDescription = "CLEAN IPTV", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(if (isTv) 48.dp else 40.dp))
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("CLEAN IPTV", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, letterSpacing = 4.sp)
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color(0xFFFFD38B), modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "Tu contenido,\na tus fuentes.",
                modifier = Modifier.fillMaxWidth(if (isTv) 0.75f else 1f),
                color = Color.White,
                style = if (isTv) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = if (isTv) 62.sp else 43.sp
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Canales de TV en directo de distintos países, metadatos de películas y series y enlaces que configuras en tu dispositivo.",
                modifier = Modifier.fillMaxWidth(if (isTv) 0.58f else 0.94f),
                color = Color.White.copy(alpha = 0.73f),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                WelcomeFeature("TV en directo")
                WelcomeFeature("TMDb")
                WelcomeFeature("Directo / iframe")
            }
            Spacer(Modifier.height(30.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth(if (isTv) 0.38f else 0.82f)
                    .height(54.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged { buttonFocused = it.isFocused }
                    .border(if (buttonFocused) 2.dp else 0.dp, if (buttonFocused) MaterialTheme.colorScheme.secondary else Color.Transparent, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text("Explorar contenido", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(15.dp))
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
