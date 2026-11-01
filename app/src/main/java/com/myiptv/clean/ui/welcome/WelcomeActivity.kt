package com.myiptv.clean.ui.welcome

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.myiptv.clean.R
import com.myiptv.clean.ui.compose.CleanIPTVTheme
import com.myiptv.clean.ui.compose.IptvActivity
import com.myiptv.clean.ui.compose.TvBrandLogo
import com.myiptv.clean.ui.compose.isTelevisionDevice
import kotlinx.coroutines.delay

class WelcomeActivity : AppCompatActivity() {
    private var isTelevision = false
    private val navigationGate = WelcomeNavigationGate()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val background = ContextCompat.getColor(this, R.color.background)
        isTelevision = isTelevisionDevice()
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
                WelcomeScreen(onContinue = ::openContent)
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean = when (
        welcomeKeyResult(isTelevision, event.keyCode, event.action, event.isCanceled)
    ) {
        WelcomeKeyResult.OPEN_CONTENT -> {
            openContent()
            true
        }
        WelcomeKeyResult.CONSUME -> true
        WelcomeKeyResult.PASS_THROUGH -> super.dispatchKeyEvent(event)
    }

    /** The timer and a tap/remote OK can finish together; open the content only once. */
    private fun openContent() {
        if (isFinishing || isDestroyed || !navigationGate.tryOpen()) return
        startActivity(Intent(this, IptvActivity::class.java))
        finish()
    }
}

@Composable
private fun WelcomeScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    val uiMode = LocalConfiguration.current.uiMode
    val isTelevision = remember(context, uiMode) { context.isTelevisionDevice(uiMode) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnContinue by rememberUpdatedState(onContinue)
    var savedElapsedMs by rememberSaveable { mutableLongStateOf(0L) }
    val clock = remember { LionVisibleClock(savedElapsedMs) }
    var expression by remember { mutableStateOf(LionWelcomeTimeline.expressionAt(savedElapsedMs)) }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            clock.resume(SystemClock.elapsedRealtime())
            try {
                while (true) {
                    val elapsed = clock.elapsedAt(SystemClock.elapsedRealtime())
                    if (LionWelcomeTimeline.isFinished(elapsed)) {
                        currentOnContinue()
                        break
                    }
                    expression = LionWelcomeTimeline.expressionAt(elapsed)
                    delay(LionWelcomeTimeline.millisUntilNextFrame(elapsed))
                }
            } finally {
                val now = SystemClock.elapsedRealtime()
                clock.pause(now)
                savedElapsedMs = clock.elapsedAt(now)
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        val isTvLayout = isTelevision || maxWidth >= 840.dp
        val availableWidth = (maxWidth - 48.dp).coerceAtLeast(48.dp)
        val availableHeight = (maxHeight - 104.dp).coerceAtLeast(48.dp)
        val lionSize = minOf(availableWidth, availableHeight, if (isTvLayout) 360.dp else 300.dp)

        Column(
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AnimatedLion(expression = expression, size = lionSize, onContinue = onContinue)
            Spacer(Modifier.height(8.dp))
            TvBrandLogo(Modifier.width(170.dp).height(56.dp))
        }
    }
}

@Composable
private fun AnimatedLion(expression: LionExpression, size: Dp, onContinue: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val skipLabel = stringResource(R.string.welcome_skip_animation)
    Box(
        modifier = Modifier.size(size).clickable(
            interactionSource = interactionSource,
            indication = null,
            role = Role.Button,
            onClickLabel = skipLabel,
            onClick = onContinue,
        )
    ) {
        Image(
            painter = painterResource(R.drawable.samplay_lion_animation_base),
            contentDescription = stringResource(R.string.welcome_lion_description),
            modifier = Modifier.fillMaxSize(),
        )
        Image(
            painter = painterResource(expression.eyeDrawable()),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
        )
    }
}

private fun LionExpression.eyeDrawable(): Int = when (this) {
    LionExpression.HALF_CLOSED -> R.drawable.samplay_lion_eyes_01
    LionExpression.WORRIED -> R.drawable.samplay_lion_eyes_02
    LionExpression.WINK -> R.drawable.samplay_lion_eyes_03
    LionExpression.DETERMINED -> R.drawable.samplay_lion_eyes_04
    LionExpression.SKEPTICAL -> R.drawable.samplay_lion_eyes_05
    LionExpression.NEUTRAL -> R.drawable.samplay_lion_eyes_06
    LionExpression.HAPPY -> R.drawable.samplay_lion_eyes_07
    LionExpression.SLEEPY -> R.drawable.samplay_lion_eyes_08
    LionExpression.CURIOUS -> R.drawable.samplay_lion_eyes_09
    LionExpression.DIZZY -> R.drawable.samplay_lion_eyes_10
    LionExpression.CONNECTED -> R.drawable.samplay_lion_eyes_11
    LionExpression.SQUEEZED -> R.drawable.samplay_lion_eyes_12
}
