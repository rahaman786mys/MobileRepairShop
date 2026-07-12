package com.app.muzzutech.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "owners")
data class Owner(
    @PrimaryKey
    val id: String = "",
    val businessName: String = "",
    val ownerName: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val shopAddress: String = "",
    val gstNumber: String = "",
    val profilePhotoBase64: String = "",
    val googleAccountId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val subscriptionTier: String = "FREE",
    val subscriptionExpiresAt: Long? = null
)
