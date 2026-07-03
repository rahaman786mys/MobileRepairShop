package com.app.muzzutech.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Record of spare part purchased for a repair
 */
@Entity(
    tableName = "spare_part_purchases",
    indices = [
        Index("supplierId"),
        Index("repairEntryId")
    ]
)
data class SparePartPurchase(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val repairEntryId: Long? = null,          // Links to RepairEntry (null if direct stock)
    val partName: String = "",
    val partPhotoPath: String = "",
    val purchasePrice: Double = 0.0,
    val supplierId: String = "",          // Links to Supplier Mobile
    val supplierName: String = "",        // Denormalized for quick display
    val quantity: Int = 1,
    val purchaseDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
