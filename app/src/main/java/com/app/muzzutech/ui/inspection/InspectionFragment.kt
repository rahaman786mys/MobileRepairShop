package com.app.muzzutech.ui.inspection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.R
import com.app.muzzutech.adapter.CommonFaultAdapter
import com.app.muzzutech.databinding.FragmentInspectionBinding
import com.app.muzzutech.utils.PhotoUtils
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class InspectionFragment : Fragment(R.layout.fragment_inspection) {

    private var _binding: FragmentInspectionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: InspectionViewModel by viewModels()
    private lateinit var faultAdapter: CommonFaultAdapter

    private var entryId: Long = 0
    private var photoFile: File? = null
    private var photoUri: Uri? = null

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoUri != null && isAdded) {
            binding.ivInspectionPhoto.setPadding(0, 0, 0, 0)
            Glide.with(this).load(photoUri).centerCrop().into(binding.ivInspectionPhoto)
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

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        faultAdapter = CommonFaultAdapter { fault ->
            binding.etCustomFault.setText(fault.faultName)
        }
        binding.rvCommonFaults.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = faultAdapter
        }
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
        photoUri = photoFile?.let {
            FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", it)
        }
        photoUri?.let { cameraLauncher.launch(it) }
    }

    private fun saveInspection() {
        if (entryId == 0L) {
            Snackbar.make(binding.root, "Error: No entry selected", Snackbar.LENGTH_LONG).show()
            return
        }

        val fault = binding.etCustomFault.text.toString().trim()
        if (fault.isEmpty()) {
            Snackbar.make(binding.root, "Please enter or select a fault", Snackbar.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            MobileRepairApp.instance.repairRepository.getEntryById(entryId)?.let { entry ->
                val updated = entry.copy(
                    faultDetected = fault,
                    inspectionPhotoPath = photoFile?.absolutePath ?: entry.inspectionPhotoPath,
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
                    faultAdapter.submitList(faults)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
