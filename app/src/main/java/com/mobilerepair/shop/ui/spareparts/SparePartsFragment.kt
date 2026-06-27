package com.mobilerepair.shop.ui.spareparts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.mobilerepair.shop.R
import com.mobilerepair.shop.data.model.SparePartPurchase
import com.mobilerepair.shop.data.model.Supplier
import com.mobilerepair.shop.databinding.FragmentSparePartsBinding
import com.mobilerepair.shop.utils.PhotoUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class SparePartsFragment : Fragment(R.layout.fragment_spare_parts) {

    private var _binding: FragmentSparePartsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SparePartsViewModel by viewModels()

    private var entryId: Long = 0
    private var photoFile: File? = null
    private var suppliersList = listOf<Supplier>()

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoFile != null) {
            binding.ivPartPhoto.setImageURI(
                FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", photoFile!!)
            )
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSparePartsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        entryId = arguments?.getLong("entryId", 0) ?: 0
        viewModel.loadPartsForEntry(entryId)

        binding.btnTakePartPhoto.setOnClickListener {
            photoFile = PhotoUtils.createPhotoFile(requireContext(), "PART_")
            val uri = photoFile?.let { FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", it) }
            uri?.let { cameraLauncher.launch(it) }
        }

        binding.btnAddPart.setOnClickListener { addPart() }
        binding.btnCompleteParts.setOnClickListener {
            Snackbar.make(binding.root, "Parts completed!", Snackbar.LENGTH_SHORT).show()
            val bundle = Bundle().apply { putLong("entryId", entryId) }
            findNavController().navigate(R.id.handoverFragment, bundle)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.suppliers.collectLatest { suppliers ->
                    suppliersList = suppliers
                    val names = suppliers.map { it.name }.toMutableList().apply { add(0, "Select Supplier") }
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, names)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.spinnerSupplier.adapter = adapter
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.addedParts.collectLatest { parts ->
                    // Show parts count
                    binding.btnCompleteParts.text = if (parts.isEmpty()) "Skip & Next" else "Complete (${parts.size} parts) & Next"
                }
            }
        }
    }

    private fun addPart() {
        val partName = binding.etPartName.text.toString().trim()
        val priceText = binding.etPurchasePrice.text.toString().trim()

        if (partName.isEmpty()) {
            Snackbar.make(binding.root, "Please enter part name", Snackbar.LENGTH_LONG).show()
            return
        }

        val price = priceText.toDoubleOrNull() ?: 0.0
        val selectedPos = binding.spinnerSupplier.selectedItemPosition
        val supplierId = if (selectedPos > 0 && selectedPos <= suppliersList.size) suppliersList[selectedPos - 1].id else 0L
        val supplierName = if (selectedPos > 0 && selectedPos <= suppliersList.size) suppliersList[selectedPos - 1].name else ""

        viewModel.addPart(entryId, partName, photoFile?.absolutePath ?: "", price, supplierId, supplierName)

        // Clear fields
        binding.etPartName.text?.clear()
        binding.etPurchasePrice.text?.clear()
        photoFile = null
        Snackbar.make(binding.root, "Part added!", Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
