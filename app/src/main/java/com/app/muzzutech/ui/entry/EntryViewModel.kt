package com.app.muzzutech.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EntryViewModel : ViewModel() {

    private val repository by lazy { MobileRepairApp.instance.repairRepository }
    private val db by lazy { MobileRepairApp.instance.database }
    private val serviceManDao by lazy { db.serviceManDao() }
    private val customerDao by lazy { db.customerDao() }
    private val dealerDao by lazy { db.dealerDao() }

    private val _serviceMen = MutableStateFlow<List<ServiceMan>>(emptyList())
    val serviceMen: StateFlow<List<ServiceMan>> = _serviceMen

    private val _saveSuccess = MutableStateFlow<Long?>(null)
    val saveSuccess: StateFlow<Long?> = _saveSuccess

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    // Track draft entry IDs by mobile to prevent duplicates from autoSaveDraft
    private val draftEntryIds = mutableMapOf<String, Long>()

    fun resetSaveState() {
        _saveSuccess.value = null
    }

    /** Call when leaving the entry screen completely to clear draft tracking */
    fun clearDraftTracking() {
        draftEntryIds.clear()
    }

    init {
        loadServiceMen()
    }

    private fun loadServiceMen() {
        viewModelScope.launch {
            serviceManDao.getActiveServiceMen().collect { list ->
                _serviceMen.value = list
            }
        }
    }

    suspend fun getCustomerByMobile(mobile: String): Customer? = customerDao.getCustomerByMobile(mobile)
    suspend fun getDealerByMobile(mobile: String): Dealer? = dealerDao.getDealerByMobile(mobile)

    fun saveEntry(
        photoPath: String,
        photoPath2: String = "",
        name: String,
        mobile: String,
        city: String,
        isDealer: Boolean,
        serviceManId: Long,
        brand: String,
        model: String,
        extraItems: String = "",
        isDraft: Boolean = false
    ) {
        viewModelScope.launch {
            _isSaving.value = true

            // Save/Update contact info
            if (isDealer) {
                dealerDao.insert(Dealer(mobile, name, city))
            } else {
                customerDao.insert(Customer(mobile, name, city))
            }

            // If we already have a draft for this mobile, update it instead of creating duplicate
            val existingDraftId = if (isDraft) draftEntryIds[mobile] else null

            if (existingDraftId != null) {
                val existing = repository.getEntryById(existingDraftId)
                if (existing != null) {
                    repository.update(existing.copy(
                        entryPhotoPath = photoPath.ifEmpty { existing.entryPhotoPath },
                        entryPhotoPath2 = photoPath2.ifEmpty { existing.entryPhotoPath2 },
                        customerName = if (!isDealer) name else existing.customerName,
                        customerMobile = if (!isDealer) mobile else existing.customerMobile,
                        customerCity = city,
                        dealerName = if (isDealer) name else existing.dealerName,
                        dealerMobile = if (isDealer) mobile else existing.dealerMobile,
                        serviceManId = serviceManId,
                        deviceBrand = brand.ifEmpty { existing.deviceBrand },
                        deviceModel = model.ifEmpty { existing.deviceModel },
                        faultDescription = extraItems,
                        isDraft = true
                    ))
                    _isSaving.value = false
                    return@launch
                }
            }

            val entry = RepairEntry(
                entryPhotoPath = photoPath,
                entryPhotoPath2 = photoPath2,
                customerName = if (!isDealer) name else "",
                customerMobile = if (!isDealer) mobile else "",
                customerCity = city,
                dealerName = if (isDealer) name else "",
                dealerMobile = if (isDealer) mobile else "",
                serviceManId = serviceManId,
                deviceBrand = brand,
                deviceModel = model,
                entryDate = System.currentTimeMillis(),
                faultDescription = extraItems, // Storing extra items here for now
                isDraft = isDraft
            )
            val id = repository.insert(entry)

            if (isDraft) {
                draftEntryIds[mobile] = id
            } else {
                draftEntryIds.remove(mobile)
                _saveSuccess.value = id
            }
            _isSaving.value = false
        }
    }
}
