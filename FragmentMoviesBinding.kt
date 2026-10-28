package com.myiptv.clean.databinding

import androidx.viewbinding.ViewBinding
import com.myiptv.clean.R

class FragmentMoviesBinding private constructor(root: View) : ViewBinding(root) {
    override fun getRoot(): View = root
    companion object {
        @JvmStatic fun inflate(inflater: android.view.LayoutInflater): FragmentMoviesBinding =
            FragmentMoviesBinding(inflater.inflate(R.layout.fragment_movies, null, false))
    }
}