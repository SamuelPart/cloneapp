package com.myiptv.clean.databinding

import androidx.viewbinding.ViewBinding
import com.myiptv.clean.R

class FragmentFavoritesBinding private constructor(root: View) : ViewBinding(root) {
    override fun getRoot(): View = root
    companion object {
        @JvmStatic fun inflate(inflater: android.view.LayoutInflater): FragmentFavoritesBinding =
            FragmentFavoritesBinding(inflater.inflate(R.layout.fragment_favorites, null, false))
    }
}