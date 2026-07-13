package com.app.muzzutech.auth

import android.content.Intent
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.data.db.AppDatabase
import com.app.muzzutech.data.model.AuthSession
import com.app.muzzutech.data.model.ServiceMan

/**
 * Owner-side worker credential lifecycle: create login, revoke (terminate), and
 * regenerate a password. Pure Room + [PasswordHasher] — deliberately NO Firebase —
 * so it can be unit-tested and reused from the owner UI.
 *
 * The plain-text password is returned to the caller ONLY for the one-time share
 * screen and is never persisted here (only its PBKDF2 hash is stored).
 *
 * Terminate is a FULL revoke: it kills any active login session AND invalidates
 * the stored password so the old one can never log in again. It intentionally
 * does NOT touch [ServiceMan.isActive], which belongs to payroll/employment state.
 */
class WorkerCredentialManager(
    private val db: AppDatabase = MobileRepairApp.instance.database
) {
    private val workers get() = db.serviceManDao()
    private val sessions get() = db.authSessionDao()

    /** Result of creating/regenerating a login. [plainPassword] is transient. */
    data class Credential(val worker: ServiceMan, val plainPassword: String)

    data class AuthOutcome(val success: Boolean, val message: String, val worker: ServiceMan? = null)

    companion object {
        const val WORKER = "WORKER"
        const val MAX_WORKERS_PER_OWNER = 10
        const val PASSWORD_LENGTH = 10
    }

    /**
     * Create a worker whose Worker ID = phone, with a fresh generated password.
     * Returns null if the phone is blank, already used (phone must be unique
     * since it is the login id), or the owner is at the worker limit.
     */
    suspend fun addWorker(ownerId: String, name: String, phone: String): Credential? {
        val cleanPhone = phone.trim()
        if (cleanPhone.isEmpty()) return null
        if (workers.findByPhone(cleanPhone) != null) return null
        if (workers.getWorkerCountByOwner(ownerId) >= MAX_WORKERS_PER_OWNER) return null

        val password = PasswordHasher.generateStrongPassword(PASSWORD_LENGTH)
        val worker = ServiceMan(
            name = name.trim(),
            mobile = cleanPhone,
            ownerId = ownerId,
            passwordHash = PasswordHasher.hash(password),
            canLogin = true,
            isActive = true
        )
        val id = workers.insert(worker)
        return Credential(worker.copy(id = id), password)
    }

    /**
     * Generate a NEW password for the same phone / Worker ID and re-enable login.
     * Returns the new plain-text password (transient) or null if the worker is gone.
     */
    suspend fun regeneratePassword(workerId: Long): String? {
        val worker = workers.getServiceManById(workerId) ?: return null
        val password = PasswordHasher.generateStrongPassword(PASSWORD_LENGTH)
        workers.update(worker.copy(passwordHash = PasswordHasher.hash(password), canLogin = true))
        return password
    }

    /**
     * Full revoke. Does BOTH:
     *  1) deactivates any active login session for this worker, and
     *  2) invalidates the stored password (overwrites the hash with an unusable
     *     random one) and blocks login (canLogin = false).
     * Payroll/employment state (isActive) is left untouched.
     */
    suspend fun terminateAccess(workerId: Long): Boolean {
        val worker = workers.getServiceManById(workerId) ?: return false
        // (1) Kill active session(s) for this worker.
        sessions.deactivateSessionsByUser(workerId.toString(), WORKER)
        // (2) Invalidate the password so the old one can never authenticate again.
        val deadHash = PasswordHasher.hash(PasswordHasher.generateStrongPassword(24))
        workers.update(worker.copy(passwordHash = deadHash, canLogin = false))
        return true
    }

    /** Pure credential check — the single source of truth for worker login. */
    suspend fun authenticate(phone: String, password: String): AuthOutcome {
        val worker = workers.findByPhone(phone.trim()) ?: return AuthOutcome(false, "Worker not found")
        if (!worker.canLogin) return AuthOutcome(false, "Account disabled by owner")
        if (!worker.isActive) return AuthOutcome(false, "Account is inactive")
        if (!PasswordHasher.verify(password, worker.passwordHash)) return AuthOutcome(false, "Incorrect password")
        return AuthOutcome(true, "OK", worker)
    }

    /** Create the local active session row for a worker (mirrors AuthManager). */
    suspend fun startWorkerSession(workerId: Long) {
        val worker = workers.getServiceManById(workerId) ?: return
        sessions.deactivateSessionsByUser(workerId.toString(), WORKER)
        sessions.insert(
            AuthSession(
                userId = workerId.toString(),
                userType = WORKER,
                firebaseUid = worker.workerAuthUid ?: "",
                isActive = true
            )
        )
    }

    /** True if this worker currently has an active session (used to force logout). */
    suspend fun isWorkerSessionActive(workerId: Long): Boolean =
        sessions.countActiveSessionsByUser(workerId.toString(), WORKER) > 0
}

/**
 * Builds the native share payload for handing credentials to a worker.
 * Kept separate (and pure) so the share text is unit-testable.
 */
object WorkerCredentialShare {

    fun buildMessage(phone: String, password: String): String =
        "Your MuZZu Tech Professional worker login — Phone: $phone Password: $password. Keep this private."

    fun buildShareIntent(phone: String, password: String): Intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, buildMessage(phone, password))
        }
}
