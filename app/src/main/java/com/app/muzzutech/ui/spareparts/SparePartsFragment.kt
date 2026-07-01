package com.app.muzzutech.ui.spareparts

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.muzzutech.R
import com.app.muzzutech.data.model.Supplier
import com.app.muzzutech.databinding.FragmentSparePartsBinding
import com.app.muzzutech.utils.PhotoUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class SparePartsFragment : Fragment(R.layout.fragment_spare_parts) {

    private var _binding: FragmentSparePartsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SparePartsViewModel by viewModels()
    private lateinit var partAdapter: com.app.muzzutech.adapter.AddedPartAdapter

    private var entryId: Long = 0
    private var photoFile: File? = null
    private var photoUri: Uri? = null
    private var suppliersList = listOf<Supplier>()

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoUri != null) {
            binding.ivPartPhoto.setImageURI(photoUri)
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) openCamera()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSparePartsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        entryId = arguments?.getLong("entryId", 0) ?: 0
        viewModel.loadPartsForEntry(entryId)

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        partAdapter = com.app.muzzutech.adapter.AddedPartAdapter { part ->
            viewModel.deletePart(part)
        }
        binding.rvAddedParts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = partAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnUploadPhoto.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                openCamera()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        binding.btnAddPart.setOnClickListener {
            addPart()
        }

        binding.btnAddSupplierQuick.setOnClickListener {
            findNavController().navigate(R.id.supplierAddFragment)
        }

        binding.btnCompleteParts.setOnClickListener {
            val bundle = Bundle().apply { putLong("entryId", entryId) }
            findNavController().navigate(R.id.handoverFragment, bundle)
        }
    }

    private fun openCamera() {
        photoFile = PhotoUtils.createPhotoFile(requireContext(), "PART_")
        photoUri = photoFile?.let {
            FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", it)
        }
        photoUri?.let { cameraLauncher.launch(it) }
    }

    private fun setupSupplierSpinner(suppliers: List<Supplier>) {
        suppliersList = suppliers
        val names = suppliers.map { "${it.name} (${it.mobile})" }.toMutableList()
        names.add(0, "Select Supplier *")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSupplier.adapter = adapter
    }

    private fun addPart() {
        val partName = binding.etPartName.text.toString().trim()
        val priceText = binding.etPurchasePrice.text.toString().trim()
        val quantity = binding.etQuantity.text.toString().trim().toIntOrNull() ?: 1

        if (partName.isEmpty()) {
            Snackbar.make(binding.root, "Please enter part name", Snackbar.LENGTH_LONG).show()
            return
        }

        val price = priceText.toDoubleOrNull() ?: 0.0
        val selectedPos = binding.spinnerSupplier.selectedItemPosition

        if (selectedPos == 0) {
            Snackbar.make(binding.root, "Please select a supplier (or add new)", Snackbar.LENGTH_LONG).show()
            return
        }

        val supplier = if (selectedPos > 0 && selectedPos <= suppliersList.size) {
            suppliersList[selectedPos - 1]
        } else null

        val payLater = binding.radioPayLater.isChecked

        viewModel.addPart(
            repairEntryId = entryId,
            partName = partName,
            photoPath = photoFile?.absolutePath ?: "",
            price = price, // Per unit price
            quantity = quantity,
            supplierId = supplier?.mobile ?: "",
            supplierName = supplier?.name ?: "",
            payLater = payLater
        )

        binding.etPartName.text?.clear()
        binding.etPurchasePrice.text?.clear()
        binding.etQuantity.text?.clear()
        binding.ivPartPhoto.setImageResource(R.drawable.ic_add)
        photoFile = null
        Snackbar.make(
            binding.root,
            if (payLater) "Part added! Due recorded for supplier." else "Part added! Marked as paid.",
            Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.suppliers.collectLatest { list ->
                    setupSupplierSpinner(list)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.addedParts.collectLatest { list ->
                    binding.tvAddedPartsLabel.text = "Added Parts (${list.size})"
                    partAdapter.submitList(list)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadSuppliers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
