package com.myiptv.clean.databinding

import androidx.viewbinding.ViewBinding
import com.myiptv.clean.R

class FragmentChannelsBinding private constructor(root: View) : ViewBinding(root) {
    override fun getRoot(): View = root
    companion object {
        @JvmStatic fun inflate(inflater: android.view.LayoutInflater): FragmentChannelsBinding =
            FragmentChannelsBinding(inflater.inflate(R.layout.fragment_channels, null, false))
    }
}