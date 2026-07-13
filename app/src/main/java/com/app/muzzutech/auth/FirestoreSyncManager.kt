package com.app.muzzutech.auth

import android.content.Context
import android.util.Log
import com.app.muzzutech.data.model.AuthSession
import com.app.muzzutech.data.model.Owner
import com.app.muzzutech.data.model.ServiceMan
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object FirestoreSyncManager {

    private const val TAG = "FirestoreSync"
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val scope = CoroutineScope(Dispatchers.IO)

    fun logLoginEvent(
        email: String,
        role: String,
        ownerId: String,
        status: String,
        method: String,
        businessName: String = "",
        deviceInfo: String = ""
    ) {
        scope.launch {
            try {
                val data = hashMapOf(
                    "email" to email,
                    "role" to role,
                    "ownerId" to ownerId,
                    "status" to status,
                    "method" to method,
                    "businessName" to businessName,
                    "device" to deviceInfo,
                    "ip" to "",
                    "date" to java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()),
                    "timestamp" to com.google.firebase.Timestamp.now()
                )
                firestore.collection("login_logs")
                    .add(data)
                    .addOnSuccessListener { Log.d(TAG, "Login event logged: $email $status") }
                    .addOnFailureListener { e -> Log.w(TAG, "Failed to log login event", e) }
            } catch (e: Exception) {
                Log.w(TAG, "Error logging login event", e)
            }
        }
    }

    /**
     * Submit a support / phone-change ticket to the "tickets" collection so the
     * founder can review it in the Founder Console. The current app user's
     * identity is auto-attached. [onResult] is invoked on the main thread.
     */
    fun submitTicket(
        subject: String,
        summary: String,
        type: String,
        ownerId: String,
        userName: String,
        businessName: String,
        email: String,
        phone: String,
        extra: Map<String, Any> = emptyMap(),
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        try {
            val data = hashMapOf<String, Any>(
                "subject" to subject,
                "summary" to summary,
                "type" to type,
                "status" to "open",
                "ownerId" to ownerId,
                "userName" to userName,
                "businessName" to businessName,
                "email" to email,
                "phone" to phone,
                "appVersion" to com.app.muzzutech.BuildConfig.VERSION_NAME,
                "platform" to "android",
                "date" to java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()),
                "createdAt" to com.google.firebase.Timestamp.now()
            )
            data.putAll(extra)
            firestore.collection("tickets")
                .add(data)
                .addOnSuccessListener { ref ->
                    Log.d(TAG, "Ticket submitted: ${ref.id} ($type)")
                    onResult(true, ref.id)
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to submit ticket", e)
                    onResult(false, e.message ?: "Failed to submit ticket")
                }
        } catch (e: Exception) {
            Log.w(TAG, "Error submitting ticket", e)
            onResult(false, e.message ?: "Error submitting ticket")
        }
    }

    fun syncOwner(owner: Owner) {
        scope.launch {
            try {
                val data = hashMapOf(
                    "email" to (owner.email ?: ""),
                    "businessName" to (owner.businessName ?: ""),
                    "ownerName" to (owner.ownerName ?: ""),
                    "phone" to (owner.phoneNumber ?: ""),
                    "shopAddress" to (owner.shopAddress ?: ""),
                    "gstNumber" to (owner.gstNumber ?: ""),
                    "profilePhotoBase64" to (owner.profilePhotoBase64 ?: ""),
                    "googleAccountId" to (owner.googleAccountId ?: ""),
                    "createdAt" to com.google.firebase.Timestamp(owner.createdAt / 1000, 0),
                    "subscriptionTier" to (owner.subscriptionTier ?: "FREE"),
                    "subscriptionExpiresAt" to (owner.subscriptionExpiresAt ?: 0L),
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
                firestore.collection("owners")
                    .document(owner.id ?: return@launch)
                    .set(data)
                    .addOnSuccessListener { Log.d(TAG, "Owner synced: ${owner.email}") }
                    .addOnFailureListener { e -> Log.w(TAG, "Failed to sync owner", e) }
            } catch (e: Exception) {
                Log.w(TAG, "Error syncing owner", e)
            }
        }
    }

    /**
     * Fetch an owner record from Firestore so a returning user's profile can be
     * restored on login (including on a fresh device). Returns null if not found.
     */
    suspend fun fetchOwnerByPhone(phone: String): Owner? {
        return try {
            val snap = firestore.collection("owners")
                .whereEqualTo("phone", phone).limit(1).get().await()
            snap.documents.firstOrNull()?.let { docToOwner(it.id, it.data) }
        } catch (e: Exception) {
            Log.w(TAG, "fetchOwnerByPhone failed", e); null
        }
    }

    suspend fun fetchOwnerByEmail(email: String): Owner? {
        return try {
            val snap = firestore.collection("owners")
                .whereEqualTo("email", email).limit(1).get().await()
            snap.documents.firstOrNull()?.let { docToOwner(it.id, it.data) }
        } catch (e: Exception) {
            Log.w(TAG, "fetchOwnerByEmail failed", e); null
        }
    }

    private fun docToOwner(id: String, d: Map<String, Any?>?): Owner? {
        if (d == null) return null
        val createdAt = when (val c = d["createdAt"]) {
            is com.google.firebase.Timestamp -> c.toDate().time
            is Number -> c.toLong()
            else -> System.currentTimeMillis()
        }
        val subExp = (d["subscriptionExpiresAt"] as? Number)?.toLong()?.takeIf { it > 0L }
        return Owner(
            id = id,
            businessName = d["businessName"] as? String ?: "",
            ownerName = d["ownerName"] as? String ?: "",
            phoneNumber = d["phone"] as? String ?: "",
            email = d["email"] as? String ?: "",
            shopAddress = d["shopAddress"] as? String ?: "",
            gstNumber = d["gstNumber"] as? String ?: "",
            profilePhotoBase64 = d["profilePhotoBase64"] as? String ?: "",
            googleAccountId = (d["googleAccountId"] as? String)?.takeIf { it.isNotEmpty() },
            createdAt = createdAt,
            subscriptionTier = d["subscriptionTier"] as? String ?: "FREE",
            subscriptionExpiresAt = subExp
        )
    }

    fun syncWorker(worker: ServiceMan, ownerId: String) {
        scope.launch {
            try {
                val data = hashMapOf(
                    "name" to (worker.name ?: ""),
                    "phone" to (worker.mobile ?: ""),
                    "ownerId" to ownerId,
                    "role" to (worker.designation.ifEmpty { "Technician" }),
                    "workerAuthUid" to (worker.workerAuthUid ?: ""),
                    "workerAuthEmail" to (worker.email ?: ""),
                    "canLogin" to (worker.canLogin && worker.isActive),
                    "isActive" to worker.isActive,
                    "entryCount" to 0,
                    "createdAt" to com.google.firebase.Timestamp.now(),
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
                firestore.collection("workers")
                    .add(data)
                    .addOnSuccessListener { doc ->
                        Log.d(TAG, "Worker synced: ${worker.name}")
                        // Update Room with Firestore doc ID if needed
                    }
                    .addOnFailureListener { e -> Log.w(TAG, "Failed to sync worker", e) }
            } catch (e: Exception) {
                Log.w(TAG, "Error syncing worker", e)
            }
        }
    }

    fun updateWorkerStatus(workerId: Long, canLogin: Boolean, worker: ServiceMan?) {
        scope.launch {
            try {
                val query = firestore.collection("workers")
                    .whereEqualTo("phone", worker?.mobile ?: return@launch)
                query.get().addOnSuccessListener { docs ->
                    for (doc in docs) {
                        doc.reference.update("canLogin", canLogin, "isActive", canLogin, "updatedAt", com.google.firebase.Timestamp.now())
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error updating worker status", e)
            }
        }
    }

    fun syncSession(session: AuthSession) {
        scope.launch {
            try {
                val data = hashMapOf(
                    "userId" to session.userId,
                    "userType" to session.userType,
                    "firebaseUid" to session.firebaseUid,
                    "loginTimestamp" to session.loginTimestamp,
                    "active" to session.isActive,
                    "createdAt" to com.google.firebase.Timestamp.now()
                )
                firestore.collection("auth_sessions")
                    .add(data)
                    .addOnSuccessListener { Log.d(TAG, "Session synced: ${session.userType}") }
                    .addOnFailureListener { e -> Log.w(TAG, "Failed to sync session", e) }
            } catch (e: Exception) {
                Log.w(TAG, "Error syncing session", e)
            }
        }
    }

    fun deactivateSession(firebaseUid: String) {
        scope.launch {
            try {
                firestore.collection("auth_sessions")
                    .whereEqualTo("firebaseUid", firebaseUid)
                    .whereEqualTo("active", true)
                    .get()
                    .addOnSuccessListener { docs ->
                        for (doc in docs) {
                            doc.reference.update("active", false)
                        }
                    }
            } catch (e: Exception) {
                Log.w(TAG, "Error deactivating sessions", e)
            }
        }
    }

    fun deleteWorkerFromFirestore(phone: String) {
        scope.launch {
            try {
                firestore.collection("workers")
                    .whereEqualTo("phone", phone)
                    .get()
                    .addOnSuccessListener { docs ->
                        for (doc in docs) {
                            doc.reference.delete()
                        }
                    }
            } catch (e: Exception) {
                Log.w(TAG, "Error deleting worker from Firestore", e)
            }
        }
    }
}
