package com.app.muzzutech

import com.app.muzzutech.auth.RegistrationDecider
import com.app.muzzutech.auth.RegistrationDecider.Decision
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the mandatory dual-verification decision logic.
 * Maps to the registration scenarios in the implementation spec (Part 6).
 * Scenarios 5-8 (login, cloud restore, logout) are integration-level and are
 * verified on-device; this covers the deterministic registration decisions.
 */
class RegistrationDeciderTest {

    // Scenarios 1 & 2: both verified (Google-first OR phone-first) + both unique -> open the details form.
    @Test
    fun bothVerifiedAndUnique_allowsForm() {
        assertEquals(
            Decision.AllowForm,
            RegistrationDecider.decide(googleVerified = true, phoneVerified = true, emailExists = false, phoneExists = false)
        )
    }

    // Scenario 3: Google email already registered -> block, redirect to Login.
    @Test
    fun existingEmail_isBlocked() {
        assertEquals(
            Decision.BlockEmailExists,
            RegistrationDecider.decide(googleVerified = true, phoneVerified = true, emailExists = true, phoneExists = false)
        )
        // Blocked as soon as Google is verified, even before phone is done.
        assertEquals(
            Decision.BlockEmailExists,
            RegistrationDecider.decide(googleVerified = true, phoneVerified = false, emailExists = true, phoneExists = false)
        )
    }

    // Scenario 4: phone already tied to another account -> block, redirect to Login.
    @Test
    fun existingPhone_isBlocked() {
        assertEquals(
            Decision.BlockPhoneExists,
            RegistrationDecider.decide(googleVerified = true, phoneVerified = true, emailExists = false, phoneExists = true)
        )
        assertEquals(
            Decision.BlockPhoneExists,
            RegistrationDecider.decide(googleVerified = false, phoneVerified = true, emailExists = false, phoneExists = true)
        )
    }

    // Deterministic priority: an existing email is reported before an existing phone.
    @Test
    fun emailDuplicateTakesPriorityOverPhoneDuplicate() {
        assertEquals(
            Decision.BlockEmailExists,
            RegistrationDecider.decide(googleVerified = true, phoneVerified = true, emailExists = true, phoneExists = true)
        )
    }

    // Scenario 9: the form is NEVER reachable with only one factor verified,
    // so no partial owner record can be written if registration is abandoned halfway.
    @Test
    fun onlyGoogleVerified_requiresPhone_noForm() {
        assertEquals(
            Decision.NeedPhone,
            RegistrationDecider.decide(googleVerified = true, phoneVerified = false, emailExists = false, phoneExists = false)
        )
    }

    @Test
    fun onlyPhoneVerified_requiresGoogle_noForm() {
        assertEquals(
            Decision.NeedGoogle,
            RegistrationDecider.decide(googleVerified = false, phoneVerified = true, emailExists = false, phoneExists = false)
        )
    }

    @Test
    fun nothingVerified_requiresGoogleFirst() {
        assertEquals(
            Decision.NeedGoogle,
            RegistrationDecider.decide(googleVerified = false, phoneVerified = false, emailExists = false, phoneExists = false)
        )
    }

    // Explicit gate check: AllowForm ONLY when both are verified.
    @Test
    fun formAllowedOnlyWhenBothVerified() {
        assertEquals(Decision.NeedGoogle, RegistrationDecider.decide(false, false, false, false))
        assertEquals(Decision.NeedPhone, RegistrationDecider.decide(true, false, false, false))
        assertEquals(Decision.NeedGoogle, RegistrationDecider.decide(false, true, false, false))
        assertEquals(Decision.AllowForm, RegistrationDecider.decide(true, true, false, false))
    }
}
