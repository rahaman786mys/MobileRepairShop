package com.app.muzzutech.ui.spareparts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.data.model.Payment
import com.app.muzzutech.data.model.SparePartPurchase
import com.app.muzzutech.data.model.Supplier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SparePartsViewModel : ViewModel() {

    private val database = MobileRepairApp.instance.database
    private val purchaseDao = database.sparePartPurchaseDao()
    private val supplierDao = database.supplierDao()
    private val paymentDao = database.paymentDao()

    private val _suppliers = MutableStateFlow<List<Supplier>>(emptyList())
    val suppliers: StateFlow<List<Supplier>> = _suppliers

    private val _addedParts = MutableStateFlow<List<SparePartPurchase>>(emptyList())
    val addedParts: StateFlow<List<SparePartPurchase>> = _addedParts

    init {
        loadSuppliers()
    }

    fun loadSuppliers() {
        viewModelScope.launch {
            supplierDao.getActiveSuppliers().collect { list ->
                _suppliers.value = list
            }
        }
    }

    fun addPart(
        repairEntryId: Long,
        partName: String,
        photoPath: String,
        price: Double,
        quantity: Int,
        supplierId: String,
        supplierName: String,
        payLater: Boolean
    ) {
        viewModelScope.launch {
            val part = SparePartPurchase(
                repairEntryId = repairEntryId,
                partName = partName,
                partPhotoPath = photoPath,
                purchasePrice = price,
                quantity = quantity,
                supplierId = supplierId,
                supplierName = supplierName
            )
            val partId = purchaseDao.insert(part)
            val totalCost = price * quantity

            if (totalCost > 0 && supplierId.isNotEmpty()) {
                val payment = Payment(
                    personType = "SUPPLIER",
                    personMobile = supplierId,
                    personName = supplierName,
                    description = "Parts: $partName x $quantity (Repair #$repairEntryId)",
                    totalAmount = totalCost,
                    paidAmount = if (payLater) 0.0 else totalCost,
                    dueAmount = if (payLater) totalCost else 0.0,
                    status = if (payLater) "UNPAID" else "PAID",
                    linkedPartId = partId
                )
                val paymentId = paymentDao.insert(payment)

                // If paid immediately, create a transaction record
                if (!payLater) {
                    val transaction = com.app.muzzutech.data.model.PaymentTransaction(
                        paymentId = paymentId,
                        personType = "SUPPLIER",
                        personMobile = supplierId,
                        personName = supplierName,
                        amount = totalCost,
                        paymentMode = "CASH",
                        note = "Immediate payment for $partName x $quantity"
                    )
                    database.paymentTransactionDao().insert(transaction)
                }
            }
        }
    }

    fun deletePart(part: SparePartPurchase) {
        viewModelScope.launch {
            purchaseDao.delete(part)
        }
    }

    fun loadPartsForEntry(repairEntryId: Long) {
        viewModelScope.launch {
            purchaseDao.getPurchasesByRepairId(repairEntryId).collectLatest { list ->
                _addedParts.value = list
            }
        }
    }
}
