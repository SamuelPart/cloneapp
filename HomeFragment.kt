package com.myiptv.clean.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.myiptv.clean.databinding.FragmentHomeBinding
import com.myiptv.clean.databinding.FragmentChannelsBinding
import com.myiptv.clean.databinding.FragmentMoviesBinding

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Setup UI and load data
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
