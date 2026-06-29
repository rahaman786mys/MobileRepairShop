package com.app.muzzutech.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Main Repair Entry - Tracks a phone repair from entry to handover
 */
@Entity(tableName = "repair_entries")
data class RepairEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // === ENTRY STAGE ===
    val deviceBrand: String = "",              // e.g., Samsung, iPhone
    val deviceModel: String = "",              // e.g., Galaxy S21
    val entryPhotoPath: String = "",           // Photo of phone at entry
    val customerName: String = "",
    val customerMobile: String = "",
    val customerCity: String = "",
    val dealerName: String = "",               // If coming from a dealer
    val dealerMobile: String = "",
    val serviceManId: Long = 0,                // Assigned service man
    val entryDate: Long = System.currentTimeMillis(),

    // === INSPECTION STAGE ===
    val faultDetected: String = "",            // Main fault
    val faultDescription: String = "",
    val additionalFaults: String = "",         // Comma-separated or JSON
    val inspectionPhotoPath: String = "",
    val inspectionDate: Long = 0,
    val inspectionDone: Boolean = false,

    // === QUOTATION STAGE ===
    val chargeAmount: Double = 0.0,
    val advanceAmount: Double = 0.0,
    val quotationDate: Long = 0,
    val quotationDone: Boolean = false,

    // === SPARE PARTS STAGE ===
    val sparePartPhotoPath: String = "",
    val sparePartName: String = "",
    val sparePartPurchasePrice: Double = 0.0,
    val supplierId: Long = 0,
    val sparePartDate: Long = 0,
    val sparePartDone: Boolean = false,

    // === COMPLETION STAGE ===
    val workStatus: String = "Pending",        // Pending, InProgress, Done
    val completionDate: Long = 0,
    val workDone: Boolean = false,

    // === HANDOVER STAGE ===
    val finalAmount: Double = 0.0,
    val paymentMode: String = "",              // Cash, Online, Both
    val onlineAmount: Double = 0.0,
    val cashAmount: Double = 0.0,
    val handoverDate: Long = 0,
    val handoverDone: Boolean = false,

    // === METADATA ===
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
