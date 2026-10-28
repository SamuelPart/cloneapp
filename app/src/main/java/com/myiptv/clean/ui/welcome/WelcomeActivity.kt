package com.myiptv.clean.ui.welcome

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.myiptv.clean.databinding.ActivityWelcomeBinding
import com.myiptv.clean.ui.MainActivity

class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding
    private val handler = Handler(Looper.getMainLooper())
    private val openHome = Runnable {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            handler.postDelayed(openHome, SPLASH_DURATION_MS)
        } else {
            openHome.run()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(openHome)
        super.onDestroy()
    }

    private companion object {
        const val SPLASH_DURATION_MS = 700L
    }
}
