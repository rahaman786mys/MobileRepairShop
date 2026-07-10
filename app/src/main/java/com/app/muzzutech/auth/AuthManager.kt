package com.app.muzzutech.auth

import android.content.Context
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.data.model.AuthSession
import com.app.muzzutech.data.model.Owner
import com.app.muzzutech.data.model.ServiceMan
import com.app.muzzutech.utils.crpto.SecurePrefs
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await

class AuthManager(private val context: Context) {

    data class AuthResult(
        val success: Boolean,
        val message: String = "",
        val firebaseUid: String = "",
        val userType: String = "" // "OWNER" or "WORKER"
    )

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val db = MobileRepairApp.instance.database
    private val prefs = SecurePrefs.appSettings(context)

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean("auth_logged_in", false)
    }

    fun getLoggedInUserType(): String {
        return prefs.getString("auth_user_type", "") ?: ""
    }

    fun getLoggedInFirebaseUid(): String {
        return prefs.getString("auth_firebase_uid", "") ?: ""
    }

    fun getLoggedInUserId(): Long {
        return prefs.getLong("auth_user_id", 0L)
    }

    suspend fun signUpWithEmail(
        businessName: String,
        ownerName: String,
        phone: String,
        email: String,
        password: String
    ): AuthResult {
        return try {
            val firebaseResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = firebaseResult.user ?: return AuthResult(false, "Failed to create user")

            firebaseUser.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(ownerName).build()).await()

            val owner = Owner(
                id = firebaseUser.uid,
                businessName = businessName,
                ownerName = ownerName,
                phoneNumber = phone,
                email = email,
                createdAt = System.currentTimeMillis()
            )
            db.ownerDao().upsert(owner)

            createSession(firebaseUser.uid, "OWNER", firebaseUser.uid)
            saveLoginState(firebaseUser.uid, "OWNER", 0L)

            FirestoreSyncManager.syncOwner(owner)
            FirestoreSyncManager.logLoginEvent(email, "owner", firebaseUser.uid, "success", "email_password", businessName)

            AuthResult(true, "Sign up successful", firebaseUser.uid, "OWNER")
        } catch (e: Exception) {
            AuthResult(false, e.message ?: "Sign up failed")
        }
    }

    suspend fun loginWithEmail(email: String, password: String): AuthResult {
        return try {
            val firebaseResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = firebaseResult.user ?: return AuthResult(false, "Login failed")

            val owner = db.ownerDao().getOwnerByEmail(email)
            if (owner == null) {
                firebaseAuth.signOut()
                return AuthResult(false, "No owner account found")
            }

            createSession(firebaseUser.uid, "OWNER", firebaseUser.uid)
            saveLoginState(firebaseUser.uid, "OWNER", 0L)

            FirestoreSyncManager.logLoginEvent(email, "owner", firebaseUser.uid, "success", "email_password", owner.businessName ?: "")

            AuthResult(true, "Login successful", firebaseUser.uid, "OWNER")
        } catch (e: Exception) {
            FirestoreSyncManager.logLoginEvent(email, "owner", "", "failed", "email_password")
            AuthResult(false, e.message ?: "Login failed")
        }
    }

    suspend fun workerLogin(phone: String, password: String): AuthResult {
        return try {
            val worker = db.serviceManDao().findByPhone(phone)
                ?: return AuthResult(false, "Worker not found")

            if (!worker.canLogin) return AuthResult(false, "Account disabled by owner")
            if (!worker.isActive) return AuthResult(false, "Account is inactive")
            if (!PasswordHasher.verify(password, worker.passwordHash)) {
                return AuthResult(false, "Incorrect password")
            }

            val firebaseUid = worker.workerAuthUid ?: ""
            createSession(firebaseUid, "WORKER", firebaseUid, worker.id.toString())
            saveLoginState(firebaseUid, "WORKER", worker.id)

            FirestoreSyncManager.logLoginEvent(worker.mobile ?: phone, "worker", worker.ownerId ?: "", "success", "phone_password", "", worker.name ?: "")

            AuthResult(true, "Worker login successful", firebaseUid, "WORKER")
        } catch (e: Exception) {
            FirestoreSyncManager.logLoginEvent(phone, "worker", "", "failed", "phone_password")
            AuthResult(false, e.message ?: "Login failed")
        }
    }

    suspend fun signInWithGoogle(idToken: String): AuthResult {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val firebaseResult = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = firebaseResult.user ?: return AuthResult(false, "Google sign-in failed")

            val owner = db.ownerDao().getOwnerById(firebaseUser.uid)
            if (owner == null) {
                val newOwner = Owner(
                    id = firebaseUser.uid,
                    ownerName = firebaseUser.displayName ?: "",
                    email = firebaseUser.email ?: "",
                    googleAccountId = firebaseUser.uid,
                    createdAt = System.currentTimeMillis()
                )
                db.ownerDao().upsert(newOwner)
                FirestoreSyncManager.syncOwner(newOwner)
            } else {
                FirestoreSyncManager.syncOwner(owner)
            }

            createSession(firebaseUser.uid, "OWNER", firebaseUser.uid)
            saveLoginState(firebaseUser.uid, "OWNER", 0L)

            val email = firebaseUser.email ?: ""
            FirestoreSyncManager.logLoginEvent(email, "owner", firebaseUser.uid, "success", "google", owner?.businessName ?: "")

            AuthResult(true, "Google sign-in successful", firebaseUser.uid, "OWNER")
        } catch (e: Exception) {
            AuthResult(false, e.message ?: "Google sign-in failed")
        }
    }

    suspend fun createWorker(
        ownerId: String,
        name: String,
        phone: String,
        password: String
    ): ServiceMan? {
        return try {
            val workerCount = db.serviceManDao().getWorkerCountByOwner(ownerId)
            if (workerCount >= 10) return null

            val passwordHash = PasswordHasher.hash(password)
            val worker = ServiceMan(
                name = name,
                mobile = phone,
                isActive = true,
                ownerId = ownerId,
                passwordHash = passwordHash,
                canLogin = true
            )
            val workerId = db.serviceManDao().insert(worker)
            val saved = worker.copy(id = workerId)
            FirestoreSyncManager.syncWorker(saved, ownerId)
            saved
        } catch (e: Exception) {
            null
        }
    }

    suspend fun disableWorker(workerId: Long) {
        val worker = db.serviceManDao().getServiceManById(workerId) ?: return
        db.serviceManDao().update(worker.copy(canLogin = false, isActive = false))
        FirestoreSyncManager.updateWorkerStatus(workerId, false, worker)
        FirestoreSyncManager.deleteWorkerFromFirestore(worker.mobile ?: "")
    }

    suspend fun enableWorker(workerId: Long) {
        val worker = db.serviceManDao().getServiceManById(workerId) ?: return
        db.serviceManDao().update(worker.copy(canLogin = true, isActive = true))
        FirestoreSyncManager.updateWorkerStatus(workerId, true, worker)
        FirestoreSyncManager.syncWorker(worker.copy(canLogin = true, isActive = true), worker.ownerId ?: "")
    }

    suspend fun logoutAllSessionsForWorker(workerId: Long) {
        val worker = db.serviceManDao().getServiceManById(workerId) ?: return
        val uid = worker.workerAuthUid ?: worker.mobile
        db.authSessionDao().deactivateAllSessions(uid)
        FirestoreSyncManager.deactivateSession(uid)
    }

    fun logout() {
        val uid = prefs.getString("auth_firebase_uid", "") ?: ""
        firebaseAuth.signOut()
        prefs.edit().clear().apply()
        if (uid.isNotEmpty()) {
            FirestoreSyncManager.deactivateSession(uid)
        }
    }

    private suspend fun createSession(firebaseUid: String, userType: String, uid: String, userId: String = "") {
        db.authSessionDao().deactivateAllSessions(firebaseUid)
        db.authSessionDao().insert(
            AuthSession(
                userId = userId.ifEmpty { uid },
                userType = userType,
                firebaseUid = firebaseUid,
                loginTimestamp = System.currentTimeMillis(),
                isActive = true
            )
        )
    }

    private fun saveLoginState(firebaseUid: String, userType: String, userId: Long) {
        prefs.edit().apply {
            putBoolean("auth_logged_in", true)
            putString("auth_user_type", userType)
            putString("auth_firebase_uid", firebaseUid)
            putLong("auth_user_id", userId)
            apply()
        }
    }

    fun isWorkerLoggedIn(): Boolean {
        return isLoggedIn() && getLoggedInUserType() == "WORKER"
    }

    fun isOwnerLoggedIn(): Boolean {
        return isLoggedIn() && getLoggedInUserType() == "OWNER"
    }
}
