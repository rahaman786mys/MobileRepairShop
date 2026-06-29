package com.mobilerepair.shop.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilerepair.shop.MobileRepairApp
import com.mobilerepair.shop.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val userProfileDao = MobileRepairApp.instance.database.userProfileDao()

    val profileFlow: Flow<UserProfile?> = userProfileDao.getUserProfileFlow()

    fun saveProfile(name: String, phone: String, shopName: String, shopAddress: String, email: String) {
        viewModelScope.launch {
            val existing = userProfileDao.getUserProfile()
            val profile = UserProfile(
                email = email,
                name = name,
                phone = phone,
                shopName = shopName,
                shopAddress = shopAddress,
                lastSyncTimestamp = existing?.lastSyncTimestamp ?: 0
            )
            userProfileDao.insertOrUpdate(profile)
        }
    }

    fun updateSyncTimestamp() {
        viewModelScope.launch {
            val existing = userProfileDao.getUserProfile()
            existing?.let {
                userProfileDao.insertOrUpdate(it.copy(lastSyncTimestamp = System.currentTimeMillis()))
            }
        }
    }
}
