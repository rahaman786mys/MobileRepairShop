package com.app.muzzutech.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val userProfileDao = MobileRepairApp.instance.database.userProfileDao()

    val profileFlow: Flow<UserProfile?> = userProfileDao.getUserProfileFlow()

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
  }
}

    fun updateSyncTimestamp() {
        viewModelScope.launch {
            val existing = userProfileDao.getUserProfile() ?: UserProfile(id = 1)
            userProfileDao.insertOrUpdate(existing.copy(lastSyncTimestamp = System.currentTimeMillis()))
        }
    }
}
