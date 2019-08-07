package com.artf.chatapp

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.artf.chatapp.databinding.FragmentSearchBinding
import com.artf.chatapp.utils.extension.getVm
import kotlinx.android.synthetic.main.fragment_search.view.*

class SearchFragment : Fragment() {

    private val firebaseVm by lazy { getVm<FirebaseViewModel>() }
    private lateinit var searchView: SearchView
    private lateinit var searchItem: MenuItem
    private lateinit var binding: FragmentSearchBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSearchBinding.inflate(LayoutInflater.from(context))
        binding.lifecycleOwner = this
        binding.firebaseVm = firebaseVm

        binding.recyclerView.adapter = SearchAdapter(SearchAdapter.OnClickListener { product ->

            //movieDetailViewModel.onReviewListItemClick(product)
        })

        firebaseVm.userList.observe(viewLifecycleOwner, Observer {
            binding.root.searchView.visibility = if(it.isNullOrEmpty()) View.GONE else View.VISIBLE
            binding.root.visibility = View.VISIBLE
        })

        binding.root.setOnClickListener {
            onSearchViewClose()
        }

        setHasOptionsMenu(true)
        return binding.root
    }

    private fun onSearchViewClose() {
        binding.root.visibility = View.GONE
        searchItem.collapseActionView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        searchItem = menu.findItem(R.id.search)
        searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextFocusChangeListener { view, b ->
            if (b.not()) onSearchViewClose() else binding.root.visibility = View.VISIBLE
        }
        super.onCreateOptionsMenu(menu, inflater)
    }
}