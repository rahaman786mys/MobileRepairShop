package com.app.muzzutech.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User/Shop Profile details for professional branding and backup
 */
@Entity(tableName = "user_profile")
data class UserProfile(
  @PrimaryKey val id: Int = 1, // Always 1 for the shop owner
  val email: String = "",
  val name: String = "",
  val phone: String = "",
  val shopName: String = "",
  val shopAddress: String = "",
  val gstNo: String = "", // Optional GST number
  val profilePhotoPath: String = "", // Local path to profile photo
  val lastSyncTimestamp: Long = 0
)
