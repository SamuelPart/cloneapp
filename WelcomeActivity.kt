package com.myiptv.clean.ui.welcome

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.myiptv.clean.databinding.ActivityWelcomeBinding
import com.myiptv.clean.ui.home.HomeActivity

class WelcomeActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityWelcomeBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Simular carga
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToHome()
        }, 2000)
    }
    
    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}
