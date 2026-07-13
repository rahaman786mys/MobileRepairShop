package com.app.muzzutech.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.auth.FirestoreSyncManager
import com.app.muzzutech.data.model.UserProfile
import com.app.muzzutech.utils.PhotoUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val userProfileDao = MobileRepairApp.instance.database.userProfileDao()
    private val ownerDao = MobileRepairApp.instance.database.ownerDao()

    val profileFlow: Flow<UserProfile?> = userProfileDao.getUserProfileFlow()

    init {
        viewModelScope.launch {
            seedProfileFromOwnerIfNeeded()
            // Push the owner record to the cloud so it appears in the Founder Console.
            syncOwnerToCloud()
        }
    }

    /**
     * The Profile screen binds to [UserProfile], but registration stores the details
     * in an [com.app.muzzutech.data.model.Owner] record. When the profile has never
     * been filled, copy the registration details across so the screen isn't blank.
     */
    private suspend fun seedProfileFromOwnerIfNeeded() {
        val existing = userProfileDao.getUserProfile()
        val alreadyFilled = existing != null &&
            (existing.name.isNotBlank() || existing.shopName.isNotBlank() || existing.phone.isNotBlank())
        if (alreadyFilled) return

        val owner = ownerDao.getFirstOwner() ?: return

        val photoPath = if (owner.profilePhotoBase64.isNotBlank()) {
            PhotoUtils.saveBase64ToFile(MobileRepairApp.instance, owner.profilePhotoBase64) ?: ""
        } else ""

        userProfileDao.insertOrUpdate(
            UserProfile(
                id = 1,
                email = owner.email,
                name = owner.ownerName,
                phone = owner.phoneNumber,
                shopName = owner.businessName,
                shopAddress = owner.shopAddress,
                gstNo = owner.gstNumber,
                profilePhotoPath = photoPath,
                lastSyncTimestamp = existing?.lastSyncTimestamp ?: 0
            )
        )
    }

    /**
     * Mirror the local profile onto the Owner record and push it to Firestore so the
     * Founder Console's Businesses tab stays in sync with the latest details.
     */
    private suspend fun syncOwnerToCloud() {
        val profile = userProfileDao.getUserProfile() ?: return
        val owner = ownerDao.getFirstOwner() ?: return
        val updated = owner.copy(
            ownerName = profile.name.ifBlank { owner.ownerName },
            businessName = profile.shopName.ifBlank { owner.businessName },
            phoneNumber = profile.phone.ifBlank { owner.phoneNumber },
            email = profile.email.ifBlank { owner.email },
            shopAddress = profile.shopAddress.ifBlank { owner.shopAddress },
            gstNumber = profile.gstNo.ifBlank { owner.gstNumber }
        )
        ownerDao.upsert(updated)
        FirestoreSyncManager.syncOwner(updated)
    }

    fun saveProfile(name: String, phone: String, shopName: String, shopAddress: String, email: String, gstNo: String = "", profilePhotoPath: String = "") {
        viewModelScope.launch {
            val existing = userProfileDao.getUserProfile()
            val profile = UserProfile(
                id = 1,
                email = email,
                name = name,
                phone = phone,
                shopName = shopName,
                shopAddress = shopAddress,
                gstNo = gstNo,
                profilePhotoPath = profilePhotoPath,
                lastSyncTimestamp = existing?.lastSyncTimestamp ?: 0
            )
            userProfileDao.insertOrUpdate(profile)
            // Reflect the saved details on the Owner record + cloud (Founder Console).
            syncOwnerToCloud()
        }
    }

    fun updateSyncTimestamp(status: String = "SUCCESS") {
        viewModelScope.launch {
            val existing = userProfileDao.getUserProfile() ?: UserProfile(id = 1)
            userProfileDao.insertOrUpdate(
                existing.copy(
                    lastSyncTimestamp = System.currentTimeMillis(),
                    lastSyncStatus = status
                )
            )
        }
    }
}
