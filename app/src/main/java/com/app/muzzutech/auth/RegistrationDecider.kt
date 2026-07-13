package com.app.muzzutech.auth

/**
 * Pure, side-effect-free decision logic for the mandatory dual-verification
 * registration flow. Kept separate from [com.app.muzzutech.ui.auth.LoginFragment]
 * so the duplicate-checking / dual-verification rules can be unit-tested directly.
 *
 * Rules:
 *  - Both Google (verified email) AND phone (verified via OTP) are mandatory.
 *  - If the Google email already belongs to an account -> block, send to Login.
 *  - If the phone already belongs to an account -> block, send to Login.
 *  - Otherwise, whichever factor is still missing must be verified next.
 *  - Only when both are verified AND both are unique may the details form open.
 */
object RegistrationDecider {

    sealed class Decision {
        /** Both verified and unique -> show the business-details form. */
        object AllowForm : Decision()
        /** Google email already registered -> block + redirect to Login. */
        object BlockEmailExists : Decision()
        /** Phone number already registered -> block + redirect to Login. */
        object BlockPhoneExists : Decision()
        /** Waiting for Google verification. */
        object NeedGoogle : Decision()
        /** Waiting for phone (OTP) verification. */
        object NeedPhone : Decision()
    }

    /**
     * @param googleVerified  a Google account has been verified this session
     * @param phoneVerified   a phone number has been verified via OTP this session
     * @param emailExists     the verified email already exists in /owners
     * @param phoneExists     the verified phone already exists in /owners
     *
     * Duplicate checks take priority (block early), then any missing factor is
     * requested, then the form is allowed.
     */
    fun decide(
        googleVerified: Boolean,
        phoneVerified: Boolean,
        emailExists: Boolean,
        phoneExists: Boolean
    ): Decision {
        if (googleVerified && emailExists) return Decision.BlockEmailExists
        if (phoneVerified && phoneExists) return Decision.BlockPhoneExists
        if (!googleVerified) return Decision.NeedGoogle
        if (!phoneVerified) return Decision.NeedPhone
        return Decision.AllowForm
    }
}
