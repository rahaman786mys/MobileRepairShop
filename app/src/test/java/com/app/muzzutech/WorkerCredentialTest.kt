package com.app.muzzutech

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.app.muzzutech.auth.PasswordHasher
import com.app.muzzutech.auth.WorkerCredentialManager
import com.app.muzzutech.auth.WorkerCredentialShare
import com.app.muzzutech.data.db.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Worker credential lifecycle test matrix (spec Part 6). Exercises the REAL
 * WorkerCredentialManager against an in-memory Room DB — the same logic the
 * owner UI and worker login path use.
 */
@Config(application = TestApplication::class)
@RunWith(RobolectricTestRunner::class)
class WorkerCredentialTest {

    private lateinit var db: AppDatabase
    private lateinit var mgr: WorkerCredentialManager

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<Context>() as TestApplication
        db = app.database
        mgr = WorkerCredentialManager(db)
    }

    @After
    fun teardown() {
        db.clearAllTables()
    }

    // --- Password generator: crypto-secure, 10-char, guarantees each class ---
    @Test
    fun passwordGenerator_is10CharAlphanumeric_withEachClass() {
        repeat(300) {
            val pw = PasswordHasher.generateStrongPassword(10)
            assertEquals("length is 10", 10, pw.length)
            assertTrue("alphanumeric only", pw.all { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' })
            assertTrue("has uppercase", pw.any { it.isUpperCase() })
            assertTrue("has lowercase", pw.any { it.isLowerCase() })
            assertTrue("has digit", pw.any { it.isDigit() })
        }
    }

    // --- Scenario 1: add worker -> password generated -> worker can log in ---
    @Test
    fun scenario1_addWorker_thenLoginWithGeneratedPassword() = runBlocking {
        val cred = mgr.addWorker("owner1", "Asha", "9000000001")!!
        assertTrue("login succeeds with generated password", mgr.authenticate("9000000001", cred.plainPassword).success)
        assertFalse("a wrong password is rejected", mgr.authenticate("9000000001", "WrongPass99").success)
    }

    // --- Scenario 2: native share sheet carries the correct text ---
    @Test
    fun scenario2_shareIntent_hasCorrectActionTypeAndText() {
        val intent = WorkerCredentialShare.buildShareIntent("9000000001", "Ab3Cd5Ef7K")
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("text/plain", intent.type)
        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
        assertTrue("contains phone", text.contains("9000000001"))
        assertTrue("contains password", text.contains("Ab3Cd5Ef7K"))
        assertTrue("branded message", text.contains("MuZZu Tech Professional"))
        assertTrue("privacy note", text.contains("Keep this private"))
    }

    // --- Scenario 3: terminate while logged in -> active session is killed ---
    @Test
    fun scenario3_terminate_killsActiveSession() = runBlocking {
        val cred = mgr.addWorker("owner1", "Asha", "9000000001")!!
        mgr.startWorkerSession(cred.worker.id)
        assertTrue("session active before terminate", mgr.isWorkerSessionActive(cred.worker.id))
        mgr.terminateAccess(cred.worker.id)
        assertFalse("session killed after terminate", mgr.isWorkerSessionActive(cred.worker.id))
    }

    // --- Scenario 4: old password rejected after termination ---
    @Test
    fun scenario4_oldPasswordRejectedAfterTermination() = runBlocking {
        val cred = mgr.addWorker("owner1", "Asha", "9000000001")!!
        assertTrue(mgr.authenticate("9000000001", cred.plainPassword).success)
        mgr.terminateAccess(cred.worker.id)
        assertFalse("old password no longer works", mgr.authenticate("9000000001", cred.plainPassword).success)
    }

    // --- Scenario 5: regenerate after termination -> new works, old rejected ---
    @Test
    fun scenario5_regenerate_newWorks_oldStillRejected() = runBlocking {
        val cred = mgr.addWorker("owner1", "Asha", "9000000001")!!
        val old = cred.plainPassword
        mgr.terminateAccess(cred.worker.id)
        val new = mgr.regeneratePassword(cred.worker.id)!!
        assertNotEquals("new password differs from old", old, new)
        assertTrue("new password works", mgr.authenticate("9000000001", new).success)
        assertFalse("old password still rejected", mgr.authenticate("9000000001", old).success)
    }

    // --- Scenario 6: owner isolation ---
    @Test
    fun scenario6_ownerIsolation_terminateDoesNotAffectOtherOwner() = runBlocking {
        val a = mgr.addWorker("ownerA", "Asha", "9000000001")!!
        val b = mgr.addWorker("ownerB", "Bilal", "9000000002")!!
        mgr.startWorkerSession(a.worker.id)
        mgr.startWorkerSession(b.worker.id)

        mgr.terminateAccess(a.worker.id)

        // Owner B's worker is completely unaffected.
        assertTrue("B session still active", mgr.isWorkerSessionActive(b.worker.id))
        assertTrue("B can still log in", mgr.authenticate("9000000002", b.plainPassword).success)
        // Owner A's worker is revoked.
        assertFalse("A session killed", mgr.isWorkerSessionActive(a.worker.id))
        assertFalse("A password revoked", mgr.authenticate("9000000001", a.plainPassword).success)
    }

    // --- Requirement #3: terminate does BOTH; payroll state (isActive) preserved ---
    @Test
    fun terminate_killsSession_andInvalidatesPassword_preservingPayrollState() = runBlocking {
        val cred = mgr.addWorker("owner1", "Asha", "9000000001")!!
        mgr.startWorkerSession(cred.worker.id)

        assertTrue(mgr.terminateAccess(cred.worker.id))

        assertFalse("1) session killed", mgr.isWorkerSessionActive(cred.worker.id))
        assertFalse("2) password invalidated", mgr.authenticate("9000000001", cred.plainPassword).success)
        val stored = db.serviceManDao().findByPhone("9000000001")!!
        assertFalse("login blocked (canLogin=false)", stored.canLogin)
        assertTrue("payroll isActive preserved", stored.isActive)
    }

    // --- Requirement #2: plaintext is never persisted, only the PBKDF2 hash ---
    @Test
    fun plaintextPassword_isNeverStored_onlyHash() = runBlocking {
        val cred = mgr.addWorker("owner1", "Asha", "9000000001")!!
        val stored = db.serviceManDao().findByPhone("9000000001")!!
        assertNotEquals("stored value is not the plaintext", cred.plainPassword, stored.passwordHash)
        assertFalse("hash does not contain the plaintext", stored.passwordHash.contains(cred.plainPassword))
        assertTrue("stored hash verifies the plaintext", PasswordHasher.verify(cred.plainPassword, stored.passwordHash))
    }
}
