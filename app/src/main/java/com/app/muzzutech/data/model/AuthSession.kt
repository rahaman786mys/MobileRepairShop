package com.app.muzzutech.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "auth_sessions")
data class AuthSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "",
    val userType: String = "",        // "OWNER" or "WORKER"
    val firebaseUid: String = "",
    val loginTimestamp: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
