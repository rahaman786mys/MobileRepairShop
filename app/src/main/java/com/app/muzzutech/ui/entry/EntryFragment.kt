package com.app.muzzutech.ui.entry

import android.Manifest
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
import com.app.muzzutech.ui.entry.EntryViewModel.ContactResult
import com.app.muzzutech.utils.PhotoUtils
import com.app.muzzutech.utils.ValidationUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToLong
import java.io.File

class EntryFragment : Fragment(R.layout.fragment_entry) {

    companion object {
        var skipCameraLaunch = false
        private const val TAG = "EntryFragment"
    }

    private var _binding: FragmentEntryBinding? = null
    private val binding get() = _binding!!
    internal val viewModel: EntryViewModel by viewModels()

    private var serviceMenList = listOf<ServiceMan>()
    private var currentPhotoSlot = 0 // 1 = first photo, 2 = second photo

    private val brands = listOf(
        "Select Brand", "Samsung", "Apple (iPhone)", "Xiaomi (Mi/Redmi/Poco)",
        "Vivo", "Oppo", "Realme", "OnePlus", "Motorola", "Google Pixel",
        "Nokia", "Micromax", "Lava", "IQOO", "Infinix", "Techno", "Nothing", "Others"
    )

    private val extraItemsList = arrayOf("Charge", "Chip", "SIM", "Pouch", "Other")
    private val selectedExtraItems = mutableSetOf<String>()

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        Log.d(TAG, "Camera returned success=$success slot=$currentPhotoSlot")
        if (success) {
            val path = if (currentPhotoSlot == 1) viewModel.photo1Path.value else viewModel.photo2Path.value
            if (path != null) {
                showPostCaptureOptions(path, currentPhotoSlot)
            }
        }
    }

    private fun showPostCaptureOptions(path: String, slot: Int) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Photo Captured")
            .setMessage("Highlight issues on the photo or retake?")
            .setPositiveButton("Mark Issue") { _, _ ->
                val bundle = Bundle().apply { putString("imagePath", path) }
                findNavController().navigate(R.id.imageEditorFragment, bundle)
            }
            .setNeutralButton("Retake") { _, _ ->
                openCameraForSlot(slot)
            }
            .setNegativeButton("Keep As Is") { _, _ ->
                if (slot == 1) {
                    loadThumbnail(1, binding.ivEntryPhoto, path)
                } else {
                    loadThumbnail(2, binding.ivEntryPhoto2, path)
                }
                updatePhotoButtonText()
            }
            .setCancelable(false)
            .show()
    }

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) openCamera()
        else if (isAdded) Snackbar.make(binding.root, "Camera permission is needed to take photos", Snackbar.LENGTH_LONG).show()
    }

    private fun loadThumbnail(slot: Int, imageView: android.widget.ImageView, path: String?) {
        Log.d(TAG, "loadThumbnail slot=$slot path=$path exists=${path?.let { File(it).exists() }} size=${path?.let { File(it).length() }}")
        if (path == null || !isAdded) return
        val file = File(path)
        if (!file.exists() || file.length() == 0L) {
            Log.w(TAG, "Thumbnail file missing or empty for slot $slot: $path")
            return
        }
        imageView.setPadding(0, 0, 0, 0)
        imageView.imageTintList = null
        Glide.with(this)
            .load(file)
            .signature(com.bumptech.glide.signature.ObjectKey(file.lastModified()))
            .transform(MultiTransformation(CenterCrop()))
            .transition(DrawableTransitionOptions.withCrossFade(150))
            .into(imageView)
        Log.d(TAG, "Thumbnail loaded successfully for slot $slot")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return try {
            _binding = FragmentEntryBinding.inflate(inflater, container, false)
            binding.root
        } catch (e: Exception) {
            Log.e(TAG, "Inflation Error", e)
            Toast.makeText(requireContext(), "Screen Error: ${e.message}", Toast.LENGTH_LONG).show()
            View(requireContext())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (_binding == null) return

        viewModel.resetSaveState()
        setupClickListeners()
        setupMobileWatcher()
        setupBrandSpinner()
        setupExtraItemsDropdown()
        observePhotoState()
        observeViewModel()

        // Load any existing photo paths (e.g., after configuration change)
        loadThumbnail(1, binding.ivEntryPhoto, viewModel.photo1Path.value)
        loadThumbnail(2, binding.ivEntryPhoto2, viewModel.photo2Path.value)
        updatePhotoButtonText()
    }

    private fun observePhotoState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.photo1Path.collectLatest { path ->
                        Log.d(TAG, "photo1Path emitted: $path")
                        if (path != null && isAdded) {
                            loadThumbnail(1, binding.ivEntryPhoto, path)
                        }
                        updatePhotoButtonText()
                    }
                }
                launch {
                    viewModel.photo2Path.collectLatest { path ->
                        Log.d(TAG, "photo2Path emitted: $path")
                        if (path != null && isAdded) {
                            loadThumbnail(2, binding.ivEntryPhoto2, path)
                        }
                        updatePhotoButtonText()
                    }
                }
            }
        }
    }

    private fun updatePhotoButtonText() {
        val hasPhoto1 = viewModel.photo1Path.value != null
        val hasPhoto2 = viewModel.photo2Path.value != null
        binding.btnTakePhotos.text = when {
            !hasPhoto1 && !hasPhoto2 -> "Take Device Photos"
            hasPhoto1 && !hasPhoto2 -> "Take Second Photo"
            hasPhoto1 && hasPhoto2 -> "Retake Photos"
            else -> "Take Device Photos"
        }
    }

    private fun setupClickListeners() {
        binding.btnTakePhotos.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                openCamera()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        binding.cardPhoto1.setOnClickListener {
            if (viewModel.photo1Path.value != null) {
                PhotoPreviewDialog.newInstance(viewModel.photo1Path.value!!, 1)
                    .show(childFragmentManager, "preview1")
            } else {
                openCameraForSlot(1)
            }
        }

        binding.cardPhoto2.setOnClickListener {
            if (viewModel.photo2Path.value != null) {
                PhotoPreviewDialog.newInstance(viewModel.photo2Path.value!!, 2)
                    .show(childFragmentManager, "preview2")
            } else {
                openCameraForSlot(2)
            }
        }

        binding.btnSaveEntry.setOnClickListener {
            if (!viewModel.isSaving.value) {
                binding.btnSaveEntry.isEnabled = false
                saveEntry(isDraft = false)
            }
        }

        binding.btnSaveDraft.setOnClickListener {
            if (!viewModel.isSaving.value) {
                binding.btnSaveDraft.isEnabled = false
                saveEntry(isDraft = true)
            }
        }

        binding.toggleGroupEntryType.addOnButtonCheckedListener { _, _, isChecked ->
            if (isChecked) {
                binding.etMobileNumber.setText("")
                binding.etName.setText("")
                binding.etCity.setText("")
                binding.layoutRepairFields.isVisible = false
            }
        }

        binding.etAdvanceAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val amt = s.toString().toDoubleOrNull() ?: 0.0
                binding.layoutAdvancePaymentMode.isVisible = amt > 0.0
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.toggleGroupAdvanceMode.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                binding.layoutAdvanceSplit.isVisible = checkedId == R.id.btnAdvanceBoth
            }
        }
    }

    fun openCameraForSlot(slot: Int) {
        currentPhotoSlot = slot
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        try {
            val photoFile = PhotoUtils.createPhotoFile(requireContext(), "ENTRY${currentPhotoSlot}_")
            val photoUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", photoFile)
            if (currentPhotoSlot == 1) {
                viewModel.setPhoto1(photoFile.absolutePath)
            } else {
                viewModel.setPhoto2(photoFile.absolutePath)
            }
            Log.d(TAG, "Launching camera for slot $currentPhotoSlot uri=$photoUri path=${photoFile.absolutePath}")
            if (skipCameraLaunch) return
            cameraLauncher.launch(photoUri)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening camera", e)
            if (isAdded) Toast.makeText(requireContext(), "Could not open camera: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPause() {
        super.onPause()
        if (!requireActivity().isChangingConfigurations) {
            autoSaveDraft()
        }
    }

    private fun autoSaveDraft() {
        if (!isAdded) return
        if (viewModel.isSaving.value) return
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
        val preferDealer = binding.toggleGroupEntryType.checkedButtonId == R.id.btnTypeDealer
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = viewModel.lookupContact(mobile, preferDealer)) {
                is EntryViewModel.ContactResult.CustomerContact -> {
                    binding.etName.setText(result.name)
                    binding.etCity.setText(result.city)
                }
                is EntryViewModel.ContactResult.DealerContact -> {
                    binding.etName.setText(result.name)
                    binding.etCity.setText(result.city)
                }
                is EntryViewModel.ContactResult.Ambiguous -> {
                    val chosenName = if (preferDealer) result.dealerName else result.customerName
                    binding.etName.setText(chosenName)
                    Snackbar.make(binding.root, "Mobile exists as both Customer and Dealer. Using ${if (preferDealer) "Dealer" else "Customer"} entry.", Snackbar.LENGTH_LONG).show()
                }
                is EntryViewModel.ContactResult.NotFound -> {}
            }
            binding.layoutRepairFields.isVisible = true
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
            android.app.AlertDialog.Builder(requireContext())
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

    private fun saveEntry(isDraft: Boolean = false) {
        Log.w(TAG, "saveEntry called isAdded=$isAdded isDraft=$isDraft p1=${viewModel.photo1Path.value} p2=${viewModel.photo2Path.value}")
        if (!isAdded) return
        val name = binding.etName.text.toString().trim()
        val mobile = binding.etMobileNumber.text.toString().trim()
        val city = binding.etCity.text.toString().trim()
        val model = binding.etModelName.text.toString().trim()
        val isDealer = binding.toggleGroupEntryType.checkedButtonId == R.id.btnTypeDealer

        if (!isDraft) {
            if (!ValidationUtils.validatePhoneNumber(binding.tilMobile)) return
            if (name.isEmpty()) {
                Snackbar.make(binding.root, "Customer name is required", Snackbar.LENGTH_SHORT).show()
                return
            }
            if (viewModel.photo1Path.value == null || viewModel.photo2Path.value == null) {
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

        val chargeText = binding.etChargeAmount.text.toString().trim()
        val advanceText = binding.etAdvanceAmount.text.toString().trim()
        val chargeAmount = ((chargeText.toDoubleOrNull() ?: 0.0) * 100).roundToLong()
        val advanceAmount = ((advanceText.toDoubleOrNull() ?: 0.0) * 100).roundToLong()

        val advanceMode = when (binding.toggleGroupAdvanceMode.checkedButtonId) {
            R.id.btnAdvanceOnline -> "ONLINE"
            R.id.btnAdvanceBoth -> "BOTH"
            else -> "CASH"
        }
        val advCash = ((binding.etAdvanceCashAmount.text.toString().toDoubleOrNull() ?: 0.0) * 100).roundToLong()
        val advOnline = ((binding.etAdvanceOnlineAmount.text.toString().toDoubleOrNull() ?: 0.0) * 100).roundToLong()

        if (!isDraft && advanceAmount > 0 && advanceMode == "BOTH" && (advCash + advOnline) != advanceAmount) {
            Snackbar.make(binding.root, "Cash + Online must equal total advance", Snackbar.LENGTH_LONG).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.saveEntry(
                photoPath = viewModel.photo1Path.value ?: "",
                photoPath2 = viewModel.photo2Path.value ?: "",
                name = name,
                mobile = mobile,
                city = city,
                isDealer = isDealer,
                serviceManId = serviceManId,
                brand = brand,
                model = model,
                extraItems = collectExtraItems(),
                chargeAmount = chargeAmount,
                advanceAmount = advanceAmount,
                advanceMode = advanceMode,
                advCash = advCash,
                advOnline = advOnline,
                isDraft = isDraft
            )
        }
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
                    binding.btnSaveEntry.isEnabled = true
                    binding.btnSaveDraft.isEnabled = true
                    if (id != null && id > 0 && isAdded) {
                        viewModel.resetSaveState()
                        Snackbar.make(binding.root, "Entry Registered!", Snackbar.LENGTH_SHORT).show()
                        val bundle = Bundle().apply { putLong("entryId", id) }
                        findNavController().navigate(R.id.sparePartsFragment, bundle)
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.saveError.collectLatest { error ->
                    binding.btnSaveEntry.isEnabled = true
                    binding.btnSaveDraft.isEnabled = true
                    if (error != null && isAdded) {
                        viewModel.resetSaveState()
                        Snackbar.make(binding.root, "Save failed: $error", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isSaving.collectLatest { saving ->
                    if (saving) {
                        binding.btnSaveEntry.isEnabled = false
                        binding.btnSaveDraft.isEnabled = false
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
