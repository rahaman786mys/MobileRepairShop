package com.app.muzzutech.ui.spareparts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
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
    private val repairRepository = MobileRepairApp.instance.repairRepository

    private val _suppliers = MutableStateFlow<List<Supplier>>(emptyList())
    val suppliers: StateFlow<List<Supplier>> = _suppliers

    private val _addedParts = MutableStateFlow<List<SparePartPurchase>>(emptyList())
    val addedParts: StateFlow<List<SparePartPurchase>> = _addedParts

    private val _addError = MutableStateFlow<String?>(null)
    val addError: StateFlow<String?> = _addError

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
        price: Long,
        quantity: Int,
        supplierId: String,
        supplierName: String,
        payLater: Boolean,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val entry = repairRepository.getEntryById(repairEntryId)
                if (entry != null && (entry.workStatus == "Done" || entry.handoverDone)) {
                    _addError.value = "Cannot add parts: repair already completed/handed over"
                    return@launch
                }
                database.withTransaction {
                    val safePartName = partName.take(100)
                    val part = SparePartPurchase(
                        repairEntryId = repairEntryId,
                        partName = safePartName,
                        partPhotoPath = photoPath,
                        purchasePrice = price,
                        quantity = quantity,
                        supplierId = supplierId.take(20),
                        supplierName = supplierName.take(100)
                    )
                    val partId = purchaseDao.insert(part)
                    val totalCost = price * quantity

                    // Atomically update the RepairEntry with the latest part info
                    if (entry != null) {
                        repairRepository.update(entry.copy(
                            sparePartName = safePartName,
                            sparePartPurchasePrice = totalCost.coerceAtMost(entry.sparePartPurchasePrice + totalCost)
                        ))
                    }

                    if (totalCost > 0L && supplierId.isNotEmpty()) {
                        val payment = Payment(
                            personType = "SUPPLIER",
                            personMobile = supplierId,
                            personName = supplierName,
                            description = "Parts: $partName x $quantity (Repair #$repairEntryId)",
                            totalAmount = totalCost,
                            paidAmount = if (payLater) 0L else totalCost,
                            dueAmount = if (payLater) totalCost else 0L,
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
            } finally {
                onComplete()
            }
        }
    }

    fun deletePart(part: SparePartPurchase) {
        viewModelScope.launch {
            val repairId = part.repairEntryId ?: return@launch
            database.withTransaction {
                val entry = repairRepository.getEntryById(repairId)
                val partCost = part.purchasePrice * part.quantity
                purchaseDao.delete(part)
                val linkedPayment = paymentDao.getPaymentByLinkedPartId(part.id)
                if (linkedPayment != null) {
                    paymentDao.delete(linkedPayment)
                }
                // Subtract deleted part cost from the RepairEntry aggregate
                if (entry != null && partCost > 0L) {
                    repairRepository.update(entry.copy(
                        sparePartPurchasePrice = (entry.sparePartPurchasePrice - partCost).coerceAtLeast(0L)
                    ))
                }
            }
        }
    }

    fun loadPartsForEntry(repairEntryId: Long) {
        viewModelScope.launch {
            purchaseDao.getPurchasesByRepairId(repairEntryId).collectLatest { list ->
                _addedParts.value = list
            }
        }
    }

    fun resetAddError() {
        _addError.value = null
    }
}
