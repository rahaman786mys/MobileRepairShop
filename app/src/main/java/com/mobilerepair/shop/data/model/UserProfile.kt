package com.mobilerepair.shop.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User/Shop Profile details for professional branding and backup
 */
@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val email: String, // Primary key is email since it's unique from Google
    val name: String = "",
    val phone: String = "",
    val shopName: String = "",
    val shopAddress: String = "",
    val lastSyncTimestamp: Long = 0
)
