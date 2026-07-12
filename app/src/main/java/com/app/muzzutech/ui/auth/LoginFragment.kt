package com.app.muzzutech.ui.auth

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.R
import com.app.muzzutech.auth.AuthManager
import com.app.muzzutech.auth.FirestoreSyncManager
import com.app.muzzutech.auth.SmsGateway
import com.app.muzzutech.data.model.Owner
import com.app.muzzutech.databinding.FragmentLoginBinding
import com.app.muzzutech.utils.OtpManager
import com.app.muzzutech.utils.crpto.SecurePrefs
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private var isRegisterMode = true
    private var pendingPhone: String? = null
    private var pendingRegPhone: String? = null
    private var profilePhotoBase64: String = ""
    private var emailVerifiedByGoogle = false

    private val smsPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            pendingPhone?.let { sendOtp(it) }
        } else {
            Toast.makeText(requireContext(), "SMS permission needed for OTP", Toast.LENGTH_LONG).show()
        }
    }

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val email = account?.email ?: ""
            if (email.isNotEmpty()) {
                if (isRegisterMode && binding.layoutRegistrationDetails.isVisible) {
                    binding.etEmail.setText(email)
                    binding.etEmail.isEnabled = false
                    emailVerifiedByGoogle = true
                    binding.btnVerifyEmailGoogle.text = "✓ Verified: $email"
                    binding.btnVerifyEmailGoogle.isEnabled = false
                    Toast.makeText(requireContext(), "Email verified via Google", Toast.LENGTH_SHORT).show()
                } else {
                    loginSuccess(email)
                }
            } else {
                Snackbar.make(binding.root, "No email returned", Snackbar.LENGTH_LONG).show()
            }
        } catch (e: ApiException) {
            val msg = when (e.statusCode) {
                10 -> "Configuration Error (Code 10). SHA-1 not registered in Google Console."
                7 -> "Network error. Check internet."
                12501 -> "Sign-in cancelled."
                else -> "Sign-in failed (Code: ${e.statusCode})"
            }
            Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
            Log.e("LoginFragment", "Google Sign-In Error: ${e.statusCode} - ${e.message}")
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            launchCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission needed", Toast.LENGTH_SHORT).show()
        }
    }

    private var photoUri: Uri? = null
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoUri != null) {
            encodePhoto(photoUri!!)
        }
    }
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            encodePhoto(uri)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateUiForMode()

        binding.toggleAuthMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                isRegisterMode = checkedId == R.id.btnToggleRegister
                updateUiForMode()
            }
        }

        binding.btnGoogleSync.setOnClickListener { signInWithGoogle() }

        binding.btnWhatsApp.setOnClickListener {
            Toast.makeText(requireContext(), "WhatsApp login coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnPhoneOtp.setOnClickListener { showPhoneInput() }

        binding.btnSendOtp.setOnClickListener {
            val phone = binding.etMobileNumber.text.toString().trim()
            if (phone.length == 10) {
                sendOtp(phone)
            } else {
                binding.tilMobileNumber.error = "Enter valid 10-digit number"
            }
        }

        binding.btnVerifyOtp.setOnClickListener {
            val otp = binding.etOtp.text.toString().trim()
            verifyOtp(otp)
        }

        binding.tvResendOtp.setOnClickListener {
            val phone = binding.etMobileNumber.text.toString().trim()
            if (phone.length == 10) sendOtp(phone)
        }

        binding.btnWorkerLogin.setOnClickListener { workerLogin() }

        binding.btnTakePhoto.setOnClickListener { takePhoto() }
        binding.btnUploadPhoto.setOnClickListener { uploadPhoto() }
        binding.btnCreateAccount.setOnClickListener { submitRegistration() }
        binding.btnVerifyEmailGoogle.setOnClickListener { signInWithGoogleForEmail() }
    }

    private fun updateUiForMode() {
        binding.tvGoogleLabel.text = if (isRegisterMode) "Register with Google" else "Login with Google"
        binding.tvWhatsAppLabel.text = if (isRegisterMode) "Register with WhatsApp" else "Login with WhatsApp"
        binding.tvPhoneOtpLabel.text = if (isRegisterMode) "Register with Phone OTP" else "Login with Phone OTP"
        binding.layoutWorker.isVisible = !isRegisterMode
        hideAllInputs()
    }

    private fun hideAllInputs() {
        binding.layoutPhoneInput.isVisible = false
        binding.layoutOtpInput.isVisible = false
        binding.layoutRegistrationDetails.isVisible = false
    }

    private fun showPhoneInput() {
        hideAllInputs()
        binding.layoutPhoneInput.isVisible = true
    }

    private fun signInWithGoogleForEmail() {
        val webClientId = getString(R.string.default_web_client_id)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(webClientId)
            .build()
        val client = GoogleSignIn.getClient(requireActivity(), gso)
        client.signOut().addOnCompleteListener {
            googleSignInLauncher.launch(client.signInIntent)
        }
    }

    private fun signInWithGoogle() {
        val webClientId = getString(R.string.default_web_client_id)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(webClientId)
            .build()
        val client = GoogleSignIn.getClient(requireActivity(), gso)
        client.signOut().addOnCompleteListener {
            googleSignInLauncher.launch(client.signInIntent)
        }
    }

    private fun sendOtp(mobile: String) {
        pendingPhone = mobile
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
            return
        }

        if (isRegisterMode) {
            lifecycleScope.launch {
                val db = MobileRepairApp.instance.database
                val existing = db.ownerDao().getOwnerByPhone("91$mobile")
                if (existing != null) {
                    activity?.runOnUiThread {
                        Toast.makeText(requireContext(), "Number already registered. Please login.", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                proceedWithOtp(mobile)
            }
        } else {
            proceedWithOtp(mobile)
        }
    }

    private fun proceedWithOtp(mobile: String) {
        binding.progressBar.isVisible = true
        binding.btnSendOtp.isEnabled = false
        binding.tilMobileNumber.error = null
        OtpManager.sendOtp(mobile) { success, error ->
            activity?.runOnUiThread {
                if (!isAdded) return@runOnUiThread
                binding.progressBar.isVisible = false
                binding.btnSendOtp.isEnabled = true
                if (success) {
                    binding.layoutPhoneInput.isVisible = false
                    binding.layoutOtpInput.isVisible = true
                    binding.tvOtpSentTo.text = if (isRegisterMode) "OTP sent for registration" else "OTP sent for login"
                    binding.tvOtpPhone.text = "+91 $mobile"
                    Toast.makeText(requireContext(), "OTP sent via SMS", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), error ?: "Failed to send OTP", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun verifyOtp(otp: String) {
        binding.progressBar.isVisible = true
        OtpManager.verifyOtp(otp) { success, error ->
            activity?.runOnUiThread {
                if (!isAdded) return@runOnUiThread
                binding.progressBar.isVisible = false
                if (success) {
                    val phone = OtpManager.getCurrentPhone() ?: ""
                    if (isRegisterMode) {
                        pendingRegPhone = phone
                        showRegistrationForm(phone)
                    } else {
                        loginWithPhone(phone)
                    }
                } else {
                    Toast.makeText(requireContext(), error ?: "Invalid OTP", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showRegistrationForm(phone: String) {
        binding.layoutOtpInput.isVisible = false
        binding.tvRegPhone.text = "+91 $phone"
        hideAllInputs()
        binding.layoutRegistrationDetails.isVisible = true
    }

    private fun takePhoto() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }
        launchCamera()
    }

    private fun launchCamera() {
        try {
            val photoFile = File(requireContext().cacheDir, "profile_${System.currentTimeMillis()}.jpg")
            photoFile.parentFile?.mkdirs()
            photoUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", photoFile)
            cameraLauncher.launch(photoUri!!)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Camera error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun uploadPhoto() {
        galleryLauncher.launch("image/*")
    }

    private fun encodePhoto(uri: Uri) {
        lifecycleScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    val input = requireContext().contentResolver.openInputStream(uri)
                    val bmp = BitmapFactory.decodeStream(input)
                    input?.close()
                    val ratio = minOf(300.0 / bmp.width, 300.0 / bmp.height)
                    Bitmap.createScaledBitmap(bmp, (bmp.width * ratio).toInt(), (bmp.height * ratio).toInt(), true)
                }
                val output = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, output)
                profilePhotoBase64 = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
                binding.ivProfilePreview.setImageBitmap(bitmap)
                binding.ivProfilePreview.isVisible = true
                Toast.makeText(requireContext(), "Photo added", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to load photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun submitRegistration() {
        val name = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val shopName = binding.etShopName.text.toString().trim()
        val address = binding.etShopAddress.text.toString().trim()
        val gst = binding.etGst.text.toString().trim()

        if (name.isEmpty()) { binding.tilFullName.error = "Required"; return }
        if (!emailVerifiedByGoogle) {
            Toast.makeText(requireContext(), "Verify your email via Google Sign-In first", Toast.LENGTH_LONG).show()
            return
        }
        if (shopName.isEmpty()) { binding.tilShopName.error = "Required"; return }
        if (address.isEmpty()) { binding.tilShopAddress.error = "Required"; return }

        val phone = pendingRegPhone ?: return
        registerWithPhone(phone, name, email, shopName, address, gst, profilePhotoBase64)
    }

    private fun registerWithPhone(phone: String, name: String = "", email: String = "", shopName: String = "", address: String = "", gst: String = "", photoBase64: String = "") {
        lifecycleScope.launch {
            try {
                val db = MobileRepairApp.instance.database
                val existingOwner = db.ownerDao().getOwnerByPhone(phone)
                if (existingOwner != null) {
                    activity?.runOnUiThread {
                        Toast.makeText(requireContext(), "Number already registered. Please login.", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                val auth = FirebaseAuth.getInstance()
                val authEmail = "phone_${phone}@muzzutech.online"
                val authPass = phone.substring(phone.length.coerceAtLeast(8) - 8)

                var firebaseUid = ""
                try {
                    val authResult = auth.signInWithEmailAndPassword(authEmail, authPass).await()
                    firebaseUid = authResult.user?.uid ?: ""
                } catch (e1: Exception) {
                    try {
                        val authResult = auth.createUserWithEmailAndPassword(authEmail, authPass).await()
                        firebaseUid = authResult.user?.uid ?: ""
                    } catch (e2: Exception) {
                        try {
                            val authResult = auth.signInAnonymously().await()
                            firebaseUid = authResult.user?.uid ?: ""
                        } catch (e3: Exception) {
                            Log.w("LoginFragment", "All Firebase Auth methods failed — continuing local: $e3")
                        }
                    }
                }

                val ownerId = if (firebaseUid.isNotEmpty()) firebaseUid else "phone_${phone}_${System.currentTimeMillis()}"
                val owner = Owner(
                    id = ownerId,
                    ownerName = name.ifEmpty { "User" },
                    phoneNumber = phone,
                    email = email.ifEmpty { authEmail },
                    businessName = shopName,
                    shopAddress = address,
                    gstNumber = gst,
                    profilePhotoBase64 = photoBase64,
                    createdAt = System.currentTimeMillis()
                )

                db.ownerDao().upsert(owner)

                FirestoreSyncManager.syncOwner(owner)
                FirestoreSyncManager.logLoginEvent(phone, "owner", ownerId, "success", "phone_otp", shopName)
                activity?.runOnUiThread {
                    val status = if (firebaseUid.isNotEmpty()) "Synced to cloud" else "Saved locally"
                    Toast.makeText(requireContext(), "Registration $status", Toast.LENGTH_SHORT).show()
                }

                val prefs = SecurePrefs.authPrefs(requireContext())
                prefs.edit().putString("phone_auth_email", authEmail)
                    .putString("phone_auth_pass", authPass).apply()

                loginSuccess(phone, ownerId)
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "Registration failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun loginWithPhone(phone: String) {
        lifecycleScope.launch {
            try {
                val auth = FirebaseAuth.getInstance()
                val prefs = SecurePrefs.authPrefs(requireContext())
                val authEmail = prefs.getString("phone_auth_email", "") ?: ""
                val authPass = prefs.getString("phone_auth_pass", "") ?: ""

                if (authEmail.isNotEmpty() && authPass.isNotEmpty()) {
                    try {
                        auth.signInWithEmailAndPassword(authEmail, authPass).await()
                    } catch (e: Exception) { }
                }

                loginSuccess(phone, auth.currentUser?.uid ?: "")
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun workerLogin() {
        val phone = binding.etWorkerPhone.text.toString().trim()
        val password = binding.etWorkerPassword.text.toString().trim()

        if (phone.isEmpty() || password.isEmpty()) {
            Snackbar.make(binding.root, "Enter email/phone and password", Snackbar.LENGTH_LONG).show()
            return
        }

        binding.progressBar.isVisible = true
        binding.btnWorkerLogin.isEnabled = false

        lifecycleScope.launch {
            val authManager = AuthManager(requireContext())
            val result = authManager.workerLogin(phone, password)
            activity?.runOnUiThread {
                if (!isAdded) return@runOnUiThread
                binding.progressBar.isVisible = false
                binding.btnWorkerLogin.isEnabled = true
                if (result.success) {
                    loginSuccess(phone, result.firebaseUid)
                } else {
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun loginSuccess(identifier: String = "", firebaseUid: String = "") {
        val ctx = context ?: return
        val prefs = SecurePrefs.authPrefs(ctx)
        prefs.edit().apply {
            putBoolean("is_logged_in", true)
            if (identifier.isNotEmpty()) putString("logged_in_identifier", identifier)
            if (firebaseUid.isNotEmpty()) putString("logged_in_firebase_uid", firebaseUid)
            apply()
        }

        if (isAdded) {
            findNavController().navigate(R.id.action_loginFragment_to_dashboardFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
