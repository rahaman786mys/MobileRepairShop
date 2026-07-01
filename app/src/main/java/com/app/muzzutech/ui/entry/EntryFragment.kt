package com.app.muzzutech.ui.entry

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
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
import com.app.muzzutech.R
import com.app.muzzutech.data.model.ServiceMan
import com.app.muzzutech.databinding.FragmentEntryBinding
import com.app.muzzutech.utils.PhotoUtils
import com.app.muzzutech.utils.ValidationUtils
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class EntryFragment : Fragment(R.layout.fragment_entry) {

    private var _binding: FragmentEntryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EntryViewModel by viewModels()

    private var photoFile: File? = null
    private var photoUri: Uri? = null
    private var photoFile2: File? = null
    private var photoUri2: Uri? = null
    private var serviceMenList = listOf<ServiceMan>()

    private val brands = listOf(
        "Select Brand", "Samsung", "Apple (iPhone)", "Xiaomi (Mi/Redmi/Poco)", 
        "Vivo", "Oppo", "Realme", "OnePlus", "Motorola", "Google Pixel", 
        "Nokia", "Micromax", "Lava", "IQOO", "Infinix", "Techno", "Nothing", "Others"
    )

    private val extraItemsList = arrayOf("Charge", "Chip", "SIM", "Pouch", "Other")
    private val selectedExtraItems = mutableSetOf<String>()

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoUri != null && isAdded) {
            binding.ivEntryPhoto.setPadding(0, 0, 0, 0)
            Glide.with(this).load(photoUri).centerCrop().into(binding.ivEntryPhoto)
        }
    }

    private val cameraLauncher2 = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoUri2 != null && isAdded) {
            binding.ivEntryPhoto2.setPadding(0, 0, 0, 0)
            Glide.with(this).load(photoUri2).centerCrop().into(binding.ivEntryPhoto2)
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) openCamera()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return try {
            _binding = FragmentEntryBinding.inflate(inflater, container, false)
            binding.root
        } catch (e: Exception) {
            Log.e("EntryFragment", "Inflation Error", e)
            val msg = e.message ?: e.toString()
            Toast.makeText(requireContext(), "Screen Error: $msg", Toast.LENGTH_LONG).show()
            View(requireContext())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (_binding == null) return

        Log.d("EntryFragment", "onViewCreated started")
        
        try {
            viewModel.resetSaveState()

            setupClickListeners()
            setupMobileWatcher()
            setupBrandSpinner()
            setupExtraItemsDropdown()
            observeViewModel()
            
            savedInstanceState?.let { bundle ->
                bundle.getString("photo1")?.let { 
                    photoFile = File(it)
                    Glide.with(this).load(photoFile).centerCrop().into(binding.ivEntryPhoto)
                }
                bundle.getString("photo2")?.let { 
                    photoFile2 = File(it)
                    Glide.with(this).load(photoFile2).centerCrop().into(binding.ivEntryPhoto2)
                }
            }
        } catch (e: Exception) {
            Log.e("EntryFragment", "onViewCreated Error", e)
            val msg = e.message ?: e.toString()
            Toast.makeText(requireContext(), "Start Error: $msg", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupBrandSpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, brands)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerBrand.adapter = adapter
    }

    private fun setupExtraItemsDropdown() {
        binding.etExtraItems.setOnClickListener {
            val selectedArray = BooleanArray(extraItemsList.size) { i ->
                selectedExtraItems.contains(extraItemsList[i])
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Extra Items Received")
                .setMultiChoiceItems(extraItemsList, selectedArray) { _, which, isChecked ->
                    if (isChecked) selectedExtraItems.add(extraItemsList[which])
                    else selectedExtraItems.remove(extraItemsList[which])
                }
                .setPositiveButton("Done") { _, _ ->
                    binding.etExtraItems.setText(selectedExtraItems.joinToString(", "))
                    binding.tilOtherItem.isVisible = selectedExtraItems.contains("Other")
                }
                .show()
        }
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

        binding.btnTakePhoto2.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                openCamera2()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        binding.btnSaveEntry.setOnClickListener {
            saveEntry(isDraft = false)
        }

        binding.btnSaveDraft.setOnClickListener {
            saveEntry(isDraft = true)
        }

        binding.toggleGroupEntryType.addOnButtonCheckedListener { _, _, isChecked ->
            if (isChecked) {
                binding.etMobileNumber.setText("")
                binding.etName.setText("")
                binding.etCity.setText("")
                binding.layoutRepairFields.isVisible = false
            }
        }
    }

    override fun onPause() {
        super.onPause()
        autoSaveDraft()
    }

    private fun autoSaveDraft() {
        if (!isAdded) return
        val mobile = binding.etMobileNumber.text.toString().trim()
        if (mobile.length >= 4) {
            saveEntry(isDraft = true)
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
        photoFile = PhotoUtils.createPhotoFile(requireContext(), "ENTRY1_")
        photoUri = photoFile?.let {
            FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", it)
        }
        photoUri?.let { cameraLauncher.launch(it) }
    }

    private fun openCamera2() {
        photoFile2 = PhotoUtils.createPhotoFile(requireContext(), "ENTRY2_")
        photoUri2 = photoFile2?.let {
            FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", it)
        }
        photoUri2?.let { cameraLauncher2.launch(it) }
    }

    private fun collectExtraItems(): String {
        val items = selectedExtraItems.toMutableList()
        if (items.contains("Other")) {
            val otherText = binding.etOtherItem.text.toString().trim()
            if (otherText.isNotEmpty()) {
                items.remove("Other")
                items.add("Other: $otherText")
            }
        }
        return items.joinToString(", ")
    }

    private fun setupServiceManSpinner(men: List<ServiceMan>) {
        if (!isAdded) return
        serviceMenList = men
        val names = men.map { it.name }.toMutableList()
        names.add(0, "Select Specialist *")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerServiceMan.adapter = adapter
    }

    private fun saveEntry(isDraft: Boolean = false) {
        if (!isAdded) return
        val name = binding.etName.text.toString().trim()
        val mobile = binding.etMobileNumber.text.toString().trim()
        val city = binding.etCity.text.toString().trim()
        val model = binding.etModelName.text.toString().trim()
        val isDealer = binding.toggleGroupEntryType.checkedButtonId == R.id.btnTypeDealer

        if (!isDraft) {
            if (!ValidationUtils.validatePhoneNumber(binding.tilMobile)) return

            if (photoFile == null || photoFile2 == null) {
                Snackbar.make(binding.root, "Mandatory: 2 photos required", Snackbar.LENGTH_SHORT).show()
                return
            }

            if (binding.spinnerBrand.selectedItemPosition <= 0) {
                Snackbar.make(binding.root, "Please select a brand", Snackbar.LENGTH_SHORT).show()
                return
            }

            if (model.isEmpty()) {
                binding.etModelName.error = "Model name required"
                return
            }

            if (binding.spinnerServiceMan.selectedItemPosition <= 0) {
                Snackbar.make(binding.root, "Mandatory: Assign a specialist", Snackbar.LENGTH_SHORT).show()
                return
            }
        } else if (mobile.isEmpty()) {
            return 
        }

        val brand = if (binding.spinnerBrand.selectedItemPosition > 0) {
            brands[binding.spinnerBrand.selectedItemPosition]
        } else ""

        val selectedPos = binding.spinnerServiceMan.selectedItemPosition
        val serviceManId = if (selectedPos > 0 && selectedPos <= serviceMenList.size) {
            serviceMenList[selectedPos - 1].id
        } else 0L

        viewModel.saveEntry(
            photoPath = photoFile?.absolutePath ?: "",
            photoPath2 = photoFile2?.absolutePath ?: "",
            name = name,
            mobile = mobile,
            city = city,
            isDealer = isDealer,
            serviceManId = serviceManId,
            brand = brand,
            model = model,
            extraItems = collectExtraItems(),
            isDraft = isDraft
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
                    if (id != null && id > 0 && isAdded) {
                        viewModel.resetSaveState()
                        Snackbar.make(binding.root, "Entry Registered!", Snackbar.LENGTH_SHORT).show()
                        val bundle = Bundle().apply { putLong("entryId", id) }
                        findNavController().navigate(R.id.inspectionFragment, bundle)
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("photo1", photoFile?.absolutePath)
        outState.putString("photo2", photoFile2?.absolutePath)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
