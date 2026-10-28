package com.myiptv.clean.ui.compose

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent

class IptvActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(9, 13, 20)
        window.navigationBarColor = Color.rgb(9, 13, 20)
        setContent {
            CleanIPTVTheme {
                IptvApp()
            }
        }
    }
}
