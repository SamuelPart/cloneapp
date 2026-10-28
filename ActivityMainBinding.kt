package com.myiptv.clean.databinding

import androidx.viewbinding.ViewBinding
import com.myiptv.clean.R

class ActivityMainBinding private constructor(root: View) : ViewBinding(root) {
    
    override fun getRoot(): View = root
    
    companion object {
        @JvmStatic
        fun inflate(inflater: android.view.LayoutInflater): ActivityMainBinding =
            ActivityMainBinding(inflater.inflate(R.layout.activity_main, null, false))
    }
}