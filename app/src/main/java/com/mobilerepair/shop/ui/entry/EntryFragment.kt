package com.mobilerepair.shop.ui.entry

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.mobilerepair.shop.R
import com.mobilerepair.shop.data.model.ServiceMan
import com.mobilerepair.shop.databinding.FragmentEntryBinding
import com.mobilerepair.shop.utils.PhotoUtils
import com.mobilerepair.shop.utils.ValidationUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class EntryFragment : Fragment(R.layout.fragment_entry) {

    private var _binding: FragmentEntryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EntryViewModel by viewModels()

    private var photoFile: File? = null
    private var photoUri: Uri? = null
    private var serviceMenList = listOf<ServiceMan>()

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoUri != null) {
            binding.ivEntryPhoto.setImageURI(photoUri)
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) openCamera()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEntryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        setupMobileWatcher()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.btnTakePhoto.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                openCamera()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        binding.btnSaveEntry.setOnClickListener {
            saveEntry()
        }

        binding.toggleGroupEntryType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                // Clear fields on switch
                binding.etMobileNumber.setText("")
                binding.etName.setText("")
                binding.etCity.setText("")
                binding.layoutRepairFields.isVisible = false
            }
        }
    }

    private fun setupMobileWatcher() {
        binding.etMobileNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.length == 10) {
                    searchMobile(s.toString())
                } else {
                    binding.layoutRepairFields.isVisible = false
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun searchMobile(mobile: String) {
        val isDealer = binding.toggleGroupEntryType.checkedButtonId == R.id.btnTypeDealer
        viewLifecycleOwner.lifecycleScope.launch {
            if (isDealer) {
                viewModel.getDealerByMobile(mobile)?.let { dealer ->
                    binding.etName.setText(dealer.name)
                    binding.etCity.setText(dealer.city)
                }
            } else {
                viewModel.getCustomerByMobile(mobile)?.let { customer ->
                    binding.etName.setText(customer.name)
                    binding.etCity.setText(customer.city)
                }
            }
            binding.layoutRepairFields.isVisible = true
        }
    }

    private fun openCamera() {
        photoFile = PhotoUtils.createPhotoFile(requireContext())
        photoUri = photoFile?.let {
            FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", it)
        }
        photoUri?.let { cameraLauncher.launch(it) }
    }

    private fun setupServiceManSpinner(men: List<ServiceMan>) {
        serviceMenList = men
        val names = men.map { it.name }.toMutableList()
        names.add(0, "Select Service Man")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerServiceMan.adapter = adapter
    }

    private fun saveEntry() {
        val name = binding.etName.text.toString().trim()
        val mobile = binding.etMobileNumber.text.toString().trim()
        val city = binding.etCity.text.toString().trim()
        val isDealer = binding.toggleGroupEntryType.checkedButtonId == R.id.btnTypeDealer

        if (!ValidationUtils.validatePhoneNumber(binding.tilMobile)) {
            return
        }

        val selectedPos = binding.spinnerServiceMan.selectedItemPosition
        val serviceManId = if (selectedPos > 0 && selectedPos <= serviceMenList.size) {
            serviceMenList[selectedPos - 1].id
        } else 0L

        viewModel.saveEntry(
            photoPath = photoFile?.absolutePath ?: "",
            name = name,
            mobile = mobile,
            city = city,
            isDealer = isDealer,
            serviceManId = serviceManId
        )
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.serviceMen.collectLatest { men ->
                    setupServiceManSpinner(men)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.saveSuccess.collectLatest { id ->
                    if (id != null && id > 0) {
                        Snackbar.make(binding.root, "Entry saved!", Snackbar.LENGTH_SHORT).show()
                        val bundle = Bundle().apply { putLong("entryId", id) }
                        findNavController().navigate(R.id.inspectionFragment, bundle)
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
