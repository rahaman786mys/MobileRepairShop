package com.mobilerepair.shop.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.mobilerepair.shop.MobileRepairApp
import com.mobilerepair.shop.R
import com.mobilerepair.shop.adapter.RepairEntryAdapter
import com.mobilerepair.shop.databinding.FragmentEntriesListBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class EntriesListFragment : Fragment(R.layout.fragment_entries_list) {

    private var _binding: FragmentEntriesListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: RepairEntryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEntriesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = RepairEntryAdapter { entry ->
            val bundle = Bundle().apply { putLong("entryId", entry.id) }
            findNavController().navigate(R.id.entryDetailFragment, bundle)
        }
        binding.rvEntries.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEntries.adapter = adapter

        // Load all entries
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                MobileRepairApp.instance.repairRepository.getAllEntries().collectLatest { entries ->
                    adapter.submitList(entries)
                }
            }
        }

        // Search
        binding.etSearch.setOnEditorActionListener { _, _, _ ->
            val query = binding.etSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                viewLifecycleOwner.lifecycleScope.launch {
                    MobileRepairApp.instance.repairRepository.searchEntries(query).collect {
                        adapter.submitList(it)
                    }
                }
            }
            false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
