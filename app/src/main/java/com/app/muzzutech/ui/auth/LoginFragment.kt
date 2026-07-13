package com.app.muzzutech.ui.auth

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
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
import com.app.muzzutech.auth.RegistrationDecider
import com.app.muzzutech.data.model.Owner
import com.app.muzzutech.data.model.UserProfile
import com.app.muzzutech.databinding.FragmentLoginBinding
import com.app.muzzutech.utils.OtpManager
import com.app.muzzutech.utils.PhotoUtils
import com.app.muzzutech.utils.crpto.SecurePrefs
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.ViewCompat

/**
 * Registration requires MANDATORY dual verification (Google + phone OTP) in either
 * order. The owner record is written to /owners only after BOTH are verified and
 * both are unique. Daily login is Google-only (Google establishes the Firebase
 * session that powers duplicate checks + cloud profile restore). Local OTP
 * (OtpManager) is kept for the phone-verification factor. Workers keep phone+password.
 */
class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private var isRegisterMode = true
    private var isWorkerMode = false
    private var isLoginPhoneMode = false

    // Registration verification state — owner is written only when all are set + unique.
    private var regGoogleEmail: String? = null
    private var regGoogleUid: String? = null
    private var regPhone: String? = null
    private var profilePhotoBase64: String = ""

    private var pendingPhone: String? = null

    private val smsPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pendingPhone?.let { sendOtp(it) }
        else Toast.makeText(requireContext(), "SMS permission needed for OTP", Toast.LENGTH_LONG).show()
    }

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val email = account?.email ?: ""
            if (email.isNotEmpty()) {
                onGoogleSignInResult(email, account?.idToken)
            } else {
                Snackbar.make(binding.root, "No email returned from Google", Snackbar.LENGTH_LONG).show()
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
        if (granted) launchCamera() else Toast.makeText(requireContext(), "Camera permission needed", Toast.LENGTH_SHORT).show()
    }

    private var photoUri: Uri? = null
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoUri != null) encodePhoto(photoUri!!)
    }
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) encodePhoto(uri)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateUiForMode()
        startPhonePulseAnimation()

        binding.toggleAuthMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                isRegisterMode = checkedId == R.id.btnToggleRegister
                isWorkerMode = checkedId == R.id.btnToggleWorker
                updateUiForMode()
            }
        }

        binding.btnGoogleSync.setOnClickListener { signInWithGoogle() }
        binding.btnPhoneOtp.setOnClickListener { showPhoneInput() }

        binding.btnSendOtp.setOnClickListener {
            val phone = binding.etMobileNumber.text.toString().trim()
            if (phone.length == 10) sendOtp(phone)
            else binding.tilMobileNumber.error = "Enter valid 10-digit number"
        }
        binding.btnVerifyOtp.setOnClickListener { verifyOtp(binding.etOtp.text.toString().trim()) }
        binding.tvResendOtp.setOnClickListener {
            val phone = binding.etMobileNumber.text.toString().trim()
            if (phone.length == 10) sendOtp(phone)
        }

        binding.btnWorkerLogin.setOnClickListener { workerLogin() }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(0, 0, 0, ime.bottom)
            insets
        }

        binding.btnTakePhoto.setOnClickListener { takePhoto() }
        binding.btnUploadPhoto.setOnClickListener { uploadPhoto() }
        binding.btnCreateAccount.setOnClickListener { submitRegistration() }
        binding.btnVerifyEmailGoogle.setOnClickListener { signInWithGoogle() }
    }

    private fun startPhonePulseAnimation() {
        val pulse = ObjectAnimator.ofFloat(binding.ivLogo, View.SCALE_X, 1f, 1.06f).apply {
            duration = 1600
            interpolator = AccelerateDecelerateInterpolator()
            repeatCount = 1000
            repeatMode = ObjectAnimator.REVERSE
        }
        val pulseY = ObjectAnimator.ofFloat(binding.ivLogo, View.SCALE_Y, 1f, 1.06f).apply {
            duration = 1600
            interpolator = AccelerateDecelerateInterpolator()
            repeatCount = 1000
            repeatMode = ObjectAnimator.REVERSE
        }
        AnimatorSet().apply {
            playTogether(pulse, pulseY)
            start()
        }
        val ring = binding.root.findViewById<View>(R.id.ivLogoRing)
        if (ring != null) {
            ObjectAnimator.ofFloat(ring, View.ROTATION, 0f, 360f).apply {
                duration = 4000
                interpolator = AccelerateDecelerateInterpolator()
                repeatCount = 1000
                start()
            }
        }
    }

    private fun updateUiForMode() {
        if (isWorkerMode) {
            binding.btnGoogleSync.isVisible = false
            binding.btnPhoneOtp.isVisible = false
            binding.btnWorkerCard.isVisible = false
            hideAllInputs()
            binding.layoutWorker.isVisible = true
            return
        }
        binding.layoutWorker.isVisible = false
        binding.btnGoogleSync.isVisible = true
        binding.btnPhoneOtp.isVisible = true
        binding.btnWorkerCard.isVisible = false
        if (isRegisterMode) {
            binding.tvGoogleLabel.text = "Continue with Google"
            binding.tvPhoneOtpLabel.text = "Verify with Phone Number"
        } else {
            binding.tvGoogleLabel.text = "Sign in with Google"
            binding.tvPhoneOtpLabel.text = "Sign in with Phone Number"
        }
        hideAllInputs()
        resetRegistrationState()
        isLoginPhoneMode = false
    }

    private fun hideAllInputs() {
        binding.layoutPhoneInput.isVisible = false
        binding.layoutOtpInput.isVisible = false
        binding.layoutRegistrationDetails.isVisible = false
        binding.layoutWorker.isVisible = false
    }

    private fun resetRegistrationState() {
        regGoogleEmail = null
        regGoogleUid = null
        regPhone = null
        profilePhotoBase64 = ""
    }

    private fun showPhoneInput() {
        hideAllInputs()
        isLoginPhoneMode = !isRegisterMode
        binding.layoutPhoneInput.isVisible = true
    }

    // ── Google ────────────────────────────────────────────────────────────

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

    /**
     * SECURITY: exchange the Google ID token for a real, server-verified Firebase
     * session (signInWithCredential) instead of trusting the picked email. The
     * verified email + UID drive everything below.
     */
    private fun onGoogleSignInResult(accountEmail: String, idToken: String?) {
        binding.progressBar.isVisible = true
        lifecycleScope.launch {
            val authError = firebaseSignInWithGoogle(idToken)
            if (!isAdded || _binding == null) return@launch
            binding.progressBar.isVisible = false

            val user = FirebaseAuth.getInstance().currentUser
            val email = (user?.email ?: accountEmail).trim()
            val uid = user?.uid ?: ""
            if (email.isEmpty() || uid.isEmpty()) {
                val reason = authError ?: if (idToken.isNullOrEmpty())
                    "Google returned no ID token — this build's signing SHA-1 is not registered in Firebase."
                else "Firebase did not establish a session."
                Snackbar.make(binding.root, "Google verify failed: $reason", Snackbar.LENGTH_INDEFINITE)
                    .setAction("OK") { }
                    .show()
                Log.e("LoginFragment", "Google verify failed. idTokenPresent=${!idToken.isNullOrEmpty()} reason=$reason")
                return@launch
            }

            if (isRegisterMode) handleRegisterResult(email, uid)
            else handleLoginResult(email, uid)
        }
    }

    /** Returns null on success, or a human-readable failure reason. */
    private suspend fun firebaseSignInWithGoogle(idToken: String?): String? {
        if (idToken.isNullOrEmpty()) return "No ID token from Google (SHA-1 / web client config)."
        return try {
            val cred = GoogleAuthProvider.getCredential(idToken, null)
            FirebaseAuth.getInstance().signInWithCredential(cred).await()
            null
        } catch (e: Exception) {
            val code = (e as? com.google.firebase.auth.FirebaseAuthException)?.errorCode
            Log.e("LoginFragment", "signInWithCredential failed code=$code", e)
            (if (code != null) "$code: " else "") + (e.message ?: "Firebase auth error")
        }
    }

    /** Registration pathway: save Google info → evaluate dual-verify */
    private fun handleRegisterResult(email: String, uid: String) {
        regGoogleEmail = email
        regGoogleUid = uid
        evaluateRegistration()
    }

    /** Login pathway: find owner → login. No registration, no dual-verify. */
    private suspend fun handleLoginResult(email: String, uid: String) {
        val db = MobileRepairApp.instance.database
        val owner = db.ownerDao().getOwnerById(uid)
            ?: db.ownerDao().getOwnerByEmail(email)
            ?: FirestoreSyncManager.fetchOwnerByEmail(email)
        if (!isAdded || _binding == null) return
        if (owner != null) {
            cacheOwnerLocally(owner)
            FirestoreSyncManager.logLoginEvent(email, "owner", uid, "success", "google", owner.businessName)
            loginSuccess(email, owner.id)
        } else {
            Snackbar.make(binding.root, "No account found for $email. Please register first.", Snackbar.LENGTH_LONG).show()
            binding.toggleAuthMode.check(R.id.btnToggleRegister)
        }
    }

    // ── Phone OTP (verification factor only; OtpManager = local/Fast2SMS) ───

    private fun sendOtp(mobile: String) {
        pendingPhone = mobile
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
            return
        }
        // Early local duplicate catch (avoids sending an OTP to a number we already
        // know locally). The authoritative cloud check runs once Google is verified.
        lifecycleScope.launch {
            val localDup = findOwnerByPhoneVariants(mobile)
            if (!isAdded) return@launch
            if (localDup != null && regGoogleUid == null) {
                promptAlreadyExists("phone number")
                return@launch
            }
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
                    binding.tvOtpSentTo.text = "OTP sent for verification"
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
                    if (isLoginPhoneMode) {
                        binding.layoutOtpInput.isVisible = false
                        handlePhoneLogin(phone)
                    } else {
                        regPhone = phone
                        binding.layoutOtpInput.isVisible = false
                        evaluateRegistration()
                    }
                } else {
                    Toast.makeText(requireContext(), error ?: "Invalid OTP", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handlePhoneLogin(phone: String) {
        lifecycleScope.launch {
            val owner = findOwnerByPhoneVariants(to10Digit(phone))
                ?: FirestoreSyncManager.fetchOwnerByPhone(phone)
                ?: FirestoreSyncManager.fetchOwnerByPhone("91${to10Digit(phone)}")
            if (!isAdded || _binding == null) return@launch
            if (owner != null) {
                cacheOwnerLocally(owner)
                FirestoreSyncManager.logLoginEvent(owner.email, "owner", owner.id, "success", "phone_otp", owner.businessName)
                loginSuccess(owner.email, owner.id)
            } else {
                Snackbar.make(binding.root, "No account found for this number. Please register first.", Snackbar.LENGTH_LONG).show()
                binding.toggleAuthMode.check(R.id.btnToggleRegister)
            }
        }
    }

    // ── Dual-verification evaluation (uses RegistrationDecider) ─────────────

    private fun evaluateRegistration() {
        lifecycleScope.launch {
            val g = regGoogleEmail
            val p = regPhone
            val emailDup = if (g != null) emailExists(g) else false
            val phoneDup = if (p != null) phoneExists(p) else false
            if (!isAdded || _binding == null) return@launch

            when (RegistrationDecider.decide(g != null, p != null, emailDup, phoneDup)) {
                RegistrationDecider.Decision.BlockEmailExists -> {
                    resetRegistrationState()
                    promptAlreadyExists("account")
                }
                RegistrationDecider.Decision.BlockPhoneExists -> {
                    resetRegistrationState()
                    promptAlreadyExists("phone number")
                }
                RegistrationDecider.Decision.NeedGoogle -> {
                    hideAllInputs()
                    Snackbar.make(binding.root, "Phone verified ✓. Now continue with Google to finish.", Snackbar.LENGTH_LONG).show()
                }
                RegistrationDecider.Decision.NeedPhone -> {
                    showPhoneInput()
                    Snackbar.make(binding.root, "Google verified ✓. Now verify your phone number.", Snackbar.LENGTH_LONG).show()
                }
                RegistrationDecider.Decision.AllowForm -> showRegistrationForm()
            }
        }
    }

    private suspend fun emailExists(email: String): Boolean {
        val dao = MobileRepairApp.instance.database.ownerDao()
        if (dao.getOwnerByEmail(email) != null) return true
        return FirestoreSyncManager.fetchOwnerByEmail(email) != null
    }

    private suspend fun phoneExists(phone: String): Boolean {
        if (findOwnerByPhoneVariants(to10Digit(phone)) != null) return true
        return FirestoreSyncManager.fetchOwnerByPhone(phone) != null ||
            FirestoreSyncManager.fetchOwnerByPhone("91${to10Digit(phone)}") != null
    }

    private fun showRegistrationForm() {
        hideAllInputs()
        binding.tvRegPhone.text = "+91 ${to10Digit(regPhone ?: "")}"
        binding.etEmail.setText(regGoogleEmail ?: "")
        binding.etEmail.isEnabled = false
        binding.etEmail.isFocusable = false
        binding.etEmail.isClickable = false
        binding.etEmail.isCursorVisible = false
        binding.btnVerifyEmailGoogle.text = "✓ Verified: ${regGoogleEmail ?: ""}"
        binding.btnVerifyEmailGoogle.isEnabled = false
        binding.ivProfilePreview.isVisible = false
        binding.layoutRegistrationDetails.isVisible = true
    }

    private fun submitRegistration() {
        val name = binding.etFullName.text.toString().trim()
        val shopName = binding.etShopName.text.toString().trim()
        val address = binding.etShopAddress.text.toString().trim()
        val gst = binding.etGst.text.toString().trim()

        if (regGoogleEmail == null || regGoogleUid == null || regPhone == null) {
            Toast.makeText(requireContext(), "Please verify both Google and phone first.", Toast.LENGTH_LONG).show()
            return
        }
        if (name.isEmpty()) { binding.tilFullName.error = "Required"; return }
        if (shopName.isEmpty()) { binding.tilShopName.error = "Required"; return }
        if (address.isEmpty()) { binding.tilShopAddress.error = "Required"; return }

        completeRegistration(name, shopName, address, gst)
    }

    private fun completeRegistration(name: String, shopName: String, address: String, gst: String) {
        val email = regGoogleEmail ?: return
        val uid = regGoogleUid ?: return
        val phone = regPhone ?: return
        lifecycleScope.launch {
            // Final safety re-check right before the write.
            if (emailExists(email) || phoneExists(phone)) {
                if (!isAdded) return@launch
                promptAlreadyExists("account")
                resetRegistrationState()
                return@launch
            }

            val owner = Owner(
                id = uid,
                ownerName = name,
                phoneNumber = phone,
                email = email,
                businessName = shopName,
                shopAddress = address,
                gstNumber = gst,
                profilePhotoBase64 = profilePhotoBase64,
                googleAccountId = uid,
                createdAt = System.currentTimeMillis()
            )

            cacheOwnerLocally(owner) // local upsert + seed Profile screen
            FirestoreSyncManager.registerOwner(owner) { ok, msg ->
                activity?.runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    Toast.makeText(
                        requireContext(),
                        if (ok) "Account created" else "Saved locally (cloud pending: $msg)",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            FirestoreSyncManager.logLoginEvent(email, "owner", uid, "success", "google+otp", shopName)
            loginSuccess(email, uid)
        }
    }

    // ── Photo ───────────────────────────────────────────────────────────────

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

    // ── Worker login (unchanged) ─────────────────────────────────────────────

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
            val result = AuthManager(requireContext()).workerLogin(phone, password)
            activity?.runOnUiThread {
                if (!isAdded) return@runOnUiThread
                binding.progressBar.isVisible = false
                binding.btnWorkerLogin.isEnabled = true
                if (result.success) loginSuccess(phone, result.firebaseUid)
                else Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    // ── Session + cloud restore ──────────────────────────────────────────────

    private fun loginSuccess(identifier: String = "", firebaseUid: String = "") {
        val ctx = context ?: return
        SecurePrefs.authPrefs(ctx).edit().apply {
            putBoolean("is_logged_in", true)
            if (identifier.isNotEmpty()) putString("logged_in_identifier", identifier)
            if (firebaseUid.isNotEmpty()) putString("logged_in_firebase_uid", firebaseUid)
            apply()
        }
        if (isAdded) findNavController().navigate(R.id.action_loginFragment_to_dashboardFragment)
    }

    /** Persist a fetched owner locally and seed the Profile screen if it's empty. */
    private suspend fun cacheOwnerLocally(owner: Owner) {
        val db = MobileRepairApp.instance.database
        db.ownerDao().upsert(owner)
        val existing = db.userProfileDao().getUserProfile()
        val profileEmpty = existing == null ||
            (existing.name.isBlank() && existing.shopName.isBlank() && existing.phone.isBlank())
        if (profileEmpty) {
            val photoPath = if (owner.profilePhotoBase64.isNotBlank()) {
                PhotoUtils.saveBase64ToFile(MobileRepairApp.instance, owner.profilePhotoBase64) ?: ""
            } else ""
            db.userProfileDao().insertOrUpdate(
                UserProfile(
                    id = 1,
                    email = owner.email,
                    name = owner.ownerName,
                    phone = owner.phoneNumber,
                    shopName = owner.businessName,
                    shopAddress = owner.shopAddress,
                    gstNo = owner.gstNumber,
                    profilePhotoPath = photoPath
                )
            )
        }
    }

    private suspend fun findOwnerByPhoneVariants(mobile10: String): Owner? {
        val dao = MobileRepairApp.instance.database.ownerDao()
        return dao.getOwnerByPhone("91$mobile10")
            ?: dao.getOwnerByPhone(mobile10)
            ?: dao.getOwnerByPhone("+91$mobile10")
    }

    private fun to10Digit(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        return if (digits.length > 10) digits.takeLast(10) else digits
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    private fun promptAlreadyExists(via: String) {
        if (!isAdded || _binding == null) return
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Already Registered")
            .setMessage("This $via already exists. Please log in instead.")
            .setCancelable(false)
            .setPositiveButton("Go to Login") { _, _ ->
                binding.toggleAuthMode.check(R.id.btnToggleLogin)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
