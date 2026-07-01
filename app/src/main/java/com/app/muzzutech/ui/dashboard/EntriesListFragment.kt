package com.app.muzzutech.ui.dashboard

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.R
import com.app.muzzutech.adapter.RepairEntryAdapter
import com.app.muzzutech.databinding.FragmentEntriesListBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class EntriesListFragment : Fragment(R.layout.fragment_entries_list) {

    private var _binding: FragmentEntriesListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: RepairEntryAdapter
    private var searchJob: Job? = null

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

        // Initial load
        loadEntries("")

        // Real-time Search
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                loadEntries(s.toString().trim())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadEntries(query: String) {
        searchJob?.cancel()
        searchJob = viewLifecycleOwner.lifecycleScope.launch {
            if (query.isEmpty()) {
                MobileRepairApp.instance.repairRepository.getAllEntries().collectLatest { entries ->
                    updateUi(entries)
                }
            } else {
                MobileRepairApp.instance.repairRepository.searchEntries(query).collectLatest { entries ->
                    updateUi(entries)
                }
            }
        }
    }

    private fun updateUi(entries: List<com.app.muzzutech.data.model.RepairEntry>) {
        adapter.submitList(entries)
        binding.layoutEmpty.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
