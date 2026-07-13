package com.app.muzzutech.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.R
import com.app.muzzutech.auth.FirestoreSyncManager
import com.app.muzzutech.data.model.Owner
import com.app.muzzutech.data.model.UserProfile
import com.app.muzzutech.databinding.FragmentProfileBinding
import com.app.muzzutech.utils.PhotoUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class ProfileFragment : Fragment(R.layout.fragment_profile) {

  private var _binding: FragmentProfileBinding? = null
  private val binding get() = _binding!!
  private val viewModel: ProfileViewModel by viewModels()

  private var profilePhotoFile: File? = null
  private var profilePhotoUri: Uri? = null
  private var currentPhotoPath: String = ""

  // Last profile loaded from DB — used as the baseline for edit-count comparisons.
  private var loadedProfile: UserProfile? = null
  private var ownerRef: Owner? = null
  private var ownerId: String = ""
  private var photoChanged = false

  private data class TicketCategory(val label: String, val type: String)

  private val ticketCategories = listOf(
    TicketCategory("Phone Number Change", "phone_change"),
    TicketCategory("Email / Gmail Change", "email_change"),
    TicketCategory("Report an Issue / Bug", "report_issue"),
    TicketCategory("Other / Suggestion", "other")
  )

  private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
    if (success && profilePhotoUri != null && isAdded) {
      Glide.with(this).load(profilePhotoUri).circleCrop().into(binding.ivProfilePhoto)
      currentPhotoPath = profilePhotoFile?.absolutePath ?: ""
      photoChanged = true
    }
  }

  private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
    uri?.let {
      if (isAdded) {
        val destFile = PhotoUtils.createPhotoFile(requireContext(), "GAL_")
        if (PhotoUtils.copyUriToFile(requireContext(), it, destFile)) {
          currentPhotoPath = destFile.absolutePath
          photoChanged = true
          Glide.with(this).load(destFile).circleCrop().into(binding.ivProfilePhoto)
        } else {
          Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }

  private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
    if (granted) openProfileCamera() else Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_SHORT).show()
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    _binding = FragmentProfileBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    loadOwnerIdentity()
    observeProfile()
    setupListeners()
    binding.btnTakeProfilePhoto.setOnClickListener { onPhotoButtonClicked() }
    applyEditLocks()
  }

  private fun loadOwnerIdentity() {
    viewLifecycleOwner.lifecycleScope.launch {
      val owner = MobileRepairApp.instance.database.ownerDao().getFirstOwner()
      ownerRef = owner
      ownerId = owner?.id ?: ""
    }
  }

  private fun onPhotoButtonClicked() {
    if (ProfileEditLimits.isLocked(requireContext(), ProfileEditLimits.KEY_PHOTO, ProfileEditLimits.MAX_PHOTO)) {
      Snackbar.make(binding.root, "Photo change limit reached (${ProfileEditLimits.MAX_PHOTO}x).", Snackbar.LENGTH_LONG).show()
      return
    }
    showPhotoPicker()
  }

  private fun showPhotoPicker() {
    val options = arrayOf("Take Camera Photo", "Choose from Gallery")
    AlertDialog.Builder(requireContext())
      .setTitle("Profile Photo")
      .setItems(options) { _, which ->
        when (which) {
          0 -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
              == PackageManager.PERMISSION_GRANTED) {
              openProfileCamera()
            } else {
              cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
          }
          1 -> openProfileGallery()
        }
      }
      .show()
  }

  private fun openProfileCamera() {
    profilePhotoFile = PhotoUtils.createPhotoFile(requireContext(), "PROF_")
    profilePhotoUri = profilePhotoFile?.let {
      FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", it)
    }
    profilePhotoUri?.let { cameraLauncher.launch(it) }
  }

  private fun openProfileGallery() {
    galleryLauncher.launch("image/*")
  }

  private fun observeProfile() {
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.profileFlow.collectLatest { profile ->
          profile?.let {
            loadedProfile = it
            binding.etProfileName.setText(it.name)
            binding.etProfileEmail.setText(it.email)
            binding.etProfilePhone.setText(it.phone)
            binding.etShopName.setText(it.shopName)
            binding.etShopAddress.setText(it.shopAddress)
            binding.etShopGst.setText(it.gstNo)

            if (it.profilePhotoPath.isNotEmpty()) {
              Glide.with(this@ProfileFragment)
                .load(it.profilePhotoPath)
                .circleCrop()
                .into(binding.ivProfilePhoto)
              currentPhotoPath = it.profilePhotoPath
            }
            applyEditLocks()
          }
        }
      }
    }
  }

  /** Disable fields that have hit their edit limit and show how many edits remain. */
  private fun applyEditLocks() {
    val ctx = context ?: return
    lockField(binding.tilProfileName, binding.etProfileName, ProfileEditLimits.KEY_NAME, ProfileEditLimits.MAX_NAME)
    lockField(binding.tilShopName, binding.etShopName, ProfileEditLimits.KEY_SHOP_NAME, ProfileEditLimits.MAX_SHOP_NAME)
    lockField(binding.tilShopAddress, binding.etShopAddress, ProfileEditLimits.KEY_ADDRESS, ProfileEditLimits.MAX_ADDRESS)

    // Phone number: not directly editable — changed via a ticket ("Raise a Ticket").
    binding.etProfilePhone.isEnabled = false
    binding.tilProfilePhone.helperText = "Locked. Use \"Raise a Ticket\" below to request a change."

    val photoLocked = ProfileEditLimits.isLocked(ctx, ProfileEditLimits.KEY_PHOTO, ProfileEditLimits.MAX_PHOTO)
    binding.btnTakeProfilePhoto.isEnabled = !photoLocked
    binding.btnTakeProfilePhoto.text = if (photoLocked) {
      "Photo change limit reached"
    } else {
      val left = ProfileEditLimits.remaining(ctx, ProfileEditLimits.KEY_PHOTO, ProfileEditLimits.MAX_PHOTO)
      "Take / Upload Profile Photo ($left left)"
    }
  }

  private fun lockField(til: TextInputLayout, et: TextInputEditText, key: String, max: Int) {
    val ctx = context ?: return
    val locked = ProfileEditLimits.isLocked(ctx, key, max)
    et.isEnabled = !locked
    til.helperText = if (locked) {
      "Editing locked (limit reached). Raise a ticket to change."
    } else {
      "${ProfileEditLimits.remaining(ctx, key, max)} edit(s) left"
    }
  }

  private fun setupListeners() {
    binding.btnSaveProfile.setOnClickListener { saveProfileWithLimits() }
    binding.btnRaiseTicket.setOnClickListener { showRaiseTicketDialog() }
  }

  private fun saveProfileWithLimits() {
    val ctx = context ?: return

    val nameLocked = ProfileEditLimits.isLocked(ctx, ProfileEditLimits.KEY_NAME, ProfileEditLimits.MAX_NAME)
    val shopLocked = ProfileEditLimits.isLocked(ctx, ProfileEditLimits.KEY_SHOP_NAME, ProfileEditLimits.MAX_SHOP_NAME)
    val addressLocked = ProfileEditLimits.isLocked(ctx, ProfileEditLimits.KEY_ADDRESS, ProfileEditLimits.MAX_ADDRESS)
    val photoLocked = ProfileEditLimits.isLocked(ctx, ProfileEditLimits.KEY_PHOTO, ProfileEditLimits.MAX_PHOTO)

    val prev = loadedProfile

    val name = if (nameLocked) prev?.name ?: "" else binding.etProfileName.text.toString().trim()
    val shop = if (shopLocked) prev?.shopName ?: "" else binding.etShopName.text.toString().trim()
    val address = if (addressLocked) prev?.shopAddress ?: "" else binding.etShopAddress.text.toString().trim()
    val phone = prev?.phone ?: binding.etProfilePhone.text.toString().trim() // always locked
    val email = binding.etProfileEmail.text.toString().trim()
    val gst = binding.etShopGst.text.toString().trim()
    val photoPath = if (photoLocked) prev?.profilePhotoPath ?: currentPhotoPath else currentPhotoPath

    if (name.isEmpty()) {
      Snackbar.make(binding.root, "Full Name is required", Snackbar.LENGTH_SHORT).show()
      return
    }

    if (prev != null) {
      if (!nameLocked && name != prev.name) ProfileEditLimits.increment(ctx, ProfileEditLimits.KEY_NAME)
      if (!shopLocked && shop != prev.shopName) ProfileEditLimits.increment(ctx, ProfileEditLimits.KEY_SHOP_NAME)
      if (!addressLocked && address != prev.shopAddress) ProfileEditLimits.increment(ctx, ProfileEditLimits.KEY_ADDRESS)
      if (!photoLocked && photoChanged && photoPath != prev.profilePhotoPath) ProfileEditLimits.increment(ctx, ProfileEditLimits.KEY_PHOTO)
    }

    viewModel.saveProfile(name, phone, shop, address, email, gst, photoPath)
    photoChanged = false
    applyEditLocks()
    Snackbar.make(binding.root, "Profile saved successfully!", Snackbar.LENGTH_SHORT).show()
  }

  /** Single ticket entry point: user picks a request type, then subject + details. */
  private fun showRaiseTicketDialog() {
    val ctx = context ?: return
    val view = layoutInflater.inflate(R.layout.dialog_ticket, null)
    val acCategory = view.findViewById<MaterialAutoCompleteTextView>(R.id.acTicketCategory)
    val etSubject = view.findViewById<TextInputEditText>(R.id.etTicketSubject)
    val etSummary = view.findViewById<TextInputEditText>(R.id.etTicketSummary)
    val tilSummary = view.findViewById<TextInputLayout>(R.id.tilTicketSummary)
    val tvUser = view.findViewById<TextView>(R.id.tvTicketUser)
    val tvWarning = view.findViewById<TextView>(R.id.tvTicketWarning)

    val name = loadedProfile?.name?.ifBlank { ownerRef?.ownerName ?: "" } ?: ownerRef?.ownerName ?: ""
    val biz = loadedProfile?.shopName?.ifBlank { ownerRef?.businessName ?: "" } ?: ownerRef?.businessName ?: ""
    tvUser.text = "Submitting as: ${name.ifBlank { "You" }}" + if (biz.isNotBlank()) " ($biz)" else ""

    acCategory.setSimpleItems(ticketCategories.map { it.label }.toTypedArray())

    var selected: TicketCategory? = null
    acCategory.setOnItemClickListener { _, _, pos, _ ->
      selected = ticketCategories[pos]
      applyCategoryToDialog(selected!!, etSubject, tilSummary, tvWarning)
    }

    val dialog = AlertDialog.Builder(ctx)
      .setTitle("Raise a Ticket")
      .setView(view)
      .setPositiveButton("Submit", null)
      .setNegativeButton("Cancel", null)
      .create()

    dialog.setOnShowListener {
      dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
        val cat = selected
        if (cat == null) {
          acCategory.error = "Select a request type"
          return@setOnClickListener
        }
        val subject = etSubject.text.toString().trim()
        val summary = etSummary.text.toString().trim()
        if (subject.isEmpty()) { etSubject.error = "Required"; return@setOnClickListener }
        if (summary.isEmpty()) { etSummary.error = "Required"; return@setOnClickListener }
        submitTicket(cat, subject, summary)
        dialog.dismiss()
      }
    }
    dialog.show()
  }

  private fun applyCategoryToDialog(
    cat: TicketCategory,
    etSubject: TextInputEditText,
    tilSummary: TextInputLayout,
    tvWarning: TextView
  ) {
    val ctx = context ?: return
    etSubject.setText(cat.label)
    tvWarning.visibility = View.GONE

    when (cat.type) {
      "phone_change" -> {
        tilSummary.hint = "Enter your new 10-digit number and the reason."
        val used = ProfileEditLimits.count(ctx, ProfileEditLimits.KEY_PHONE_CHANGE)
        val max = ProfileEditLimits.MAX_PHONE_CHANGE
        when {
          used >= max -> {
            tvWarning.visibility = View.VISIBLE
            tvWarning.text = "You've used all $max phone-number change requests. This one will be sent for manual review by support."
          }
          used == max - 1 -> {
            tvWarning.visibility = View.VISIBLE
            tvWarning.text = "This is your LAST self-service phone-number change request."
          }
        }
      }
      "email_change" -> tilSummary.hint = "Enter your new email and the reason."
      "report_issue" -> tilSummary.hint = "Describe the issue or bug you found."
      else -> tilSummary.hint = "Describe your request or suggestion."
    }
  }

  private fun submitTicket(cat: TicketCategory, subject: String, summary: String) {
    val name = loadedProfile?.name?.ifBlank { ownerRef?.ownerName ?: "" } ?: ownerRef?.ownerName ?: ""
    val biz = loadedProfile?.shopName?.ifBlank { ownerRef?.businessName ?: "" } ?: ownerRef?.businessName ?: ""
    val email = loadedProfile?.email?.ifBlank { ownerRef?.email ?: "" } ?: ownerRef?.email ?: ""
    val phone = loadedProfile?.phone?.ifBlank { ownerRef?.phoneNumber ?: "" } ?: ownerRef?.phoneNumber ?: ""

    Snackbar.make(binding.root, "Submitting ticket...", Snackbar.LENGTH_SHORT).show()
    viewLifecycleOwner.lifecycleScope.launch {
      // Tickets require an authenticated session (Firestore rules) — establish one if missing.
      val ensured = FirestoreSyncManager.ensureSignedIn(requireContext(), phone)
      if (!isAdded) return@launch
      val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
      val diag = "v${com.app.muzzutech.BuildConfig.VERSION_NAME} · auth=${uid?.take(6) ?: "NONE"} · ensured=$ensured"

      FirestoreSyncManager.submitTicket(
        subject = subject,
        summary = summary,
        type = cat.type,
        ownerId = ownerId,
        userName = name,
        businessName = biz,
        email = email,
        phone = phone,
        category = cat.label
      ) { ok, msg ->
        activity?.runOnUiThread {
          if (!isAdded) return@runOnUiThread
          if (ok) {
            if (cat.type == "phone_change") {
              ProfileEditLimits.increment(requireContext(), ProfileEditLimits.KEY_PHONE_CHANGE)
            }
            Snackbar.make(binding.root, "Ticket submitted. We'll review it soon.", Snackbar.LENGTH_LONG).show()
          } else {
            showTicketError("$msg\n\n[$diag]")
          }
        }
      }
    }
  }

  private fun showTicketError(detail: String) {
    if (!isAdded) return
    AlertDialog.Builder(requireContext())
      .setTitle("Ticket failed")
      .setMessage(detail)
      .setPositiveButton("OK", null)
      .show()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
