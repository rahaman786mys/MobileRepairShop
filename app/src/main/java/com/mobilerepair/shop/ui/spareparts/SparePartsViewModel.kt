package com.mobilerepair.shop.ui.spareparts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilerepair.shop.MobileRepairApp
import com.mobilerepair.shop.data.model.SparePartPurchase
import com.mobilerepair.shop.data.model.Supplier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SparePartsViewModel : ViewModel() {

    private val purchaseDao = MobileRepairApp.instance.database.sparePartPurchaseDao()
    private val supplierDao = MobileRepairApp.instance.database.supplierDao()

    private val _suppliers = MutableStateFlow<List<Supplier>>(emptyList())
    val suppliers: StateFlow<List<Supplier>> = _suppliers

    private val _addedParts = MutableStateFlow<List<SparePartPurchase>>(emptyList())
    val addedParts: StateFlow<List<SparePartPurchase>> = _addedParts

    init {
        loadSuppliers()
    }

    private fun loadSuppliers() {
        viewModelScope.launch {
            supplierDao.getActiveSuppliers().collect { list ->
                _suppliers.value = list
            }
        }
    }

    fun addPart(repairEntryId: Long, partName: String, photoPath: String, price: Double, supplierId: Long, supplierName: String) {
        viewModelScope.launch {
            val part = SparePartPurchase(
                repairEntryId = repairEntryId,
                partName = partName,
                partPhotoPath = photoPath,
                purchasePrice = price,
                supplierId = supplierId,
                supplierName = supplierName
            )
            purchaseDao.insert(part)
            // Reload parts for this entry
            purchaseDao.getPurchasesByRepairId(repairEntryId).collect {
                _addedParts.value = it
            }
        }
    }

    fun loadPartsForEntry(repairEntryId: Long) {
        viewModelScope.launch {
            purchaseDao.getPurchasesByRepairId(repairEntryId).collect { list ->
                _addedParts.value = list
            }
        }
    }
}
