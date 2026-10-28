package com.myiptv.clean.databinding

import androidx.viewbinding.ViewBinding
import com.myiptv.clean.R

class FragmentHomeBinding private constructor(root: View) : ViewBinding(root) {
    override fun getRoot(): View = root
    companion object {
        @JvmStatic fun inflate(inflater: android.view.LayoutInflater): FragmentHomeBinding =
            FragmentHomeBinding(inflater.inflate(R.layout.fragment_home, null, false))
    }
}