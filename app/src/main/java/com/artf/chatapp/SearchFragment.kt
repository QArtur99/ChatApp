package com.artf.chatapp

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.artf.chatapp.databinding.FragmentSearchBinding
import com.artf.chatapp.utils.FragmentState
import com.artf.chatapp.utils.NetworkState
import com.artf.chatapp.utils.extension.getVm

class SearchFragment : Fragment() {

    private val firebaseVm by lazy { getVm<FirebaseViewModel>() }
    private lateinit var searchView: SearchView
    private lateinit var searchItem: MenuItem
    private lateinit var binding: FragmentSearchBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSearchBinding.inflate(LayoutInflater.from(context))
        binding.lifecycleOwner = this
        binding.firebaseVm = firebaseVm

        binding.recyclerView.itemAnimator = null
        binding.recyclerView.adapter = SearchAdapter(SearchAdapter.OnClickListener { user ->
            firebaseVm.setReceiver(user)
            firebaseVm.setFragmentState(FragmentState.CHAT)
            onSearchViewClose()
        })

        firebaseVm.userSearchStatus.observe(viewLifecycleOwner, Observer {
            binding.searchView.visibility = View.VISIBLE
            when(it){
                NetworkState.LOADING ->{
                    binding.progressBar.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                    binding.info.visibility = View.GONE
                }
                NetworkState.LOADED -> {
                    binding.progressBar.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                    binding.info.visibility = View.GONE
                }
                NetworkState.FAILED -> {
                    binding.progressBar.visibility = View.GONE
                    binding.recyclerView.visibility = View.GONE
                    binding.info.visibility = View.VISIBLE
                }
                else -> {}
            }
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
        setOnQueryTextFocusChangeListener()
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun setOnQueryTextFocusChangeListener() {
        searchView.setOnQueryTextFocusChangeListener { view, b ->
            if (b.not()) onSearchViewClose()
            else {
                binding.root.visibility = View.VISIBLE
                binding.searchView.visibility = View.GONE
            }
        }
    }
}