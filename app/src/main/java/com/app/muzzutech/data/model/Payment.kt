package com.app.muzzutech.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val personType: String,         // "DEALER", "SUPPLIER", "CUSTOMER"
    val personMobile: String,       // Phone number (links to Customer/Dealer/Supplier)
    val personName: String = "",
    val description: String = "",   // e.g., "Repair charge for Galaxy S21" or "Parts purchase - Display"
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val dueAmount: Double = 0.0,   // totalAmount - paidAmount
    val status: String = "UNPAID",  // "UNPAID", "PARTIAL", "PAID"
    val linkedEntryId: Long = 0,   // Links to RepairEntry (if customer/dealer)
    val linkedSaleId: Long = 0,    // Links to Sale (if supplier direct purchase)
    val linkedPartId: Long = 0,    // Links to SparePartPurchase (if supplier parts)
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
