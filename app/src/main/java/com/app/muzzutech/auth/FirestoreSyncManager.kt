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
