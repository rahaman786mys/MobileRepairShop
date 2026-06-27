package com.mobilerepair.shop.ui.inspection

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import com.mobilerepair.shop.MobileRepairApp
import com.mobilerepair.shop.R
import com.mobilerepair.shop.databinding.FragmentInspectionBinding
import com.mobilerepair.shop.utils.PhotoUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class InspectionFragment : Fragment(R.layout.fragment_inspection) {

    private var _binding: FragmentInspectionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: InspectionViewModel by viewModels()

    private var entryId: Long = 0
    private var photoFile: File? = null

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoFile != null) {
            binding.ivInspectionPhoto.setImageURI(
                FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", photoFile!!)
            )
            // Run AI analysis
            val bitmap = BitmapFactory.decodeFile(photoFile!!.absolutePath)
            viewModel.analyzePhoto(bitmap)
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) openCamera()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInspectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        entryId = arguments?.getLong("entryId", 0) ?: 0

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.btnTakeInspectionPhoto.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                openCamera()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        binding.btnSaveInspection.setOnClickListener {
            saveInspection()
        }
    }

    private fun openCamera() {
        photoFile = PhotoUtils.createPhotoFile(requireContext(), "INSP_")
        val uri = photoFile?.let {
            FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", it)
        }
        uri?.let { cameraLauncher.launch(it) }
    }

    private fun saveInspection() {
        if (entryId == 0L) {
            Snackbar.make(binding.root, "Error: No entry selected", Snackbar.LENGTH_LONG).show()
            return
        }

        val fault = binding.etCustomFault.text.toString().trim()
        if (fault.isEmpty()) {
            Snackbar.make(binding.root, "Please enter or select a fault", Snackbar.LENGTH_LONG).show()
            return
        }

        // Update entry with inspection data
        viewLifecycleOwner.lifecycleScope.launch {
            MobileRepairApp.instance.repairRepository.getEntryById(entryId)?.let { entry ->
                val updated = entry.copy(
                    faultDetected = fault,
                    faultDescription = binding.tvAISuggestions.text.toString(),
                    inspectionPhotoPath = photoFile?.absolutePath ?: "",
                    inspectionDate = System.currentTimeMillis(),
                    inspectionDone = true
                )
                MobileRepairApp.instance.repairRepository.update(updated)

                val bundle = Bundle().apply { putLong("entryId", entryId) }
                findNavController().navigate(R.id.quotationFragment, bundle)
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.commonFaults.collectLatest { faults ->
                    // Show common faults (simplified - show in a text block)
                    if (faults.isNotEmpty()) {
                        val faultText = faults.joinToString("\n") { "${it.faultName} (₹${it.defaultCharge})" }
                        binding.tvAISuggestions.text = "Common Faults:\n$faultText"
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.aiSuggestions.collectLatest { suggestions ->
                    if (suggestions.isNotEmpty()) {
                        binding.tvAISuggestions.text = "AI Suggestions:\n${suggestions.joinToString("\n")}"
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
