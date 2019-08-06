package com.artf.chatapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.artf.chatapp.databinding.FragmentSearchBinding
import com.artf.chatapp.utils.extension.getVm

class SearchFragment : Fragment() {

    private val firebaseVm by lazy { getVm<FirebaseViewModel>() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentSearchBinding.inflate(LayoutInflater.from(context))
        binding.lifecycleOwner = this
        binding.firebaseVm = firebaseVm

        binding.recyclerView.adapter = SearchAdapter(SearchAdapter.OnClickListener { product ->
            //movieDetailViewModel.onReviewListItemClick(product)
        })


        return binding.root
    }
}