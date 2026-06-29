package com.app.muzzutech.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dealers")
data class Dealer(
    @PrimaryKey
    val mobileNumber: String, // Unique 10-digit number
    val name: String?,
    val city: String?,
    val createdAt: Long = System.currentTimeMillis()
)
