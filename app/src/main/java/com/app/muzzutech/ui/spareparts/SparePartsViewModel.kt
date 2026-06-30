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
                supplierId = supplierId,
                supplierName = supplierName
            )
            val partId = purchaseDao.insert(part)

            if (payLater && price > 0 && supplierId.isNotEmpty()) {
                val payment = Payment(
                    personType = "SUPPLIER",
                    personMobile = supplierId,
                    personName = supplierName,
                    description = "Parts: $partName (Repair #$repairEntryId)",
                    totalAmount = price,
                    paidAmount = 0.0,
                    dueAmount = price,
                    status = "UNPAID",
                    linkedPartId = partId
                )
                paymentDao.insert(payment)
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
