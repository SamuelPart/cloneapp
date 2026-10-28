package com.myiptv.clean.databinding

import androidx.viewbinding.ViewBinding
import com.myiptv.clean.R

class FragmentSettingsBinding private constructor(root: View) : ViewBinding(root) {
    override fun getRoot(): View = root
    companion object {
        @JvmStatic fun inflate(inflater: android.view.LayoutInflater): FragmentSettingsBinding =
            FragmentSettingsBinding(inflater.inflate(R.layout.fragment_settings, null, false))
    }
}