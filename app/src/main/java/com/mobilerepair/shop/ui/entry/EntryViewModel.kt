package com.mobilerepair.shop.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilerepair.shop.MobileRepairApp
import com.mobilerepair.shop.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EntryViewModel : ViewModel() {

    private val repository = MobileRepairApp.instance.repairRepository
    private val db = MobileRepairApp.instance.database
    private val serviceManDao = db.serviceManDao()
    private val customerDao = db.customerDao()
    private val dealerDao = db.dealerDao()

    private val _serviceMen = MutableStateFlow<List<ServiceMan>>(emptyList())
    val serviceMen: StateFlow<List<ServiceMan>> = _serviceMen

    private val _saveSuccess = MutableStateFlow<Long?>(null)
    val saveSuccess: StateFlow<Long?> = _saveSuccess

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

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
        name: String,
        mobile: String,
        city: String,
        isDealer: Boolean,
        serviceManId: Long,
        brand: String,
        model: String
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            
            // Save/Update contact info
            if (isDealer) {
                dealerDao.insert(Dealer(mobile, name, city))
            } else {
                customerDao.insert(Customer(mobile, name, city))
            }

            val entry = RepairEntry(
                entryPhotoPath = photoPath,
                customerName = if (!isDealer) name else "",
                customerMobile = if (!isDealer) mobile else "",
                customerCity = city,
                dealerName = if (isDealer) name else "",
                dealerMobile = if (isDealer) mobile else "",
                serviceManId = serviceManId,
                deviceBrand = brand,
                deviceModel = model,
                entryDate = System.currentTimeMillis()
            )
            val id = repository.insert(entry)
            _saveSuccess.value = id
            _isSaving.value = false
        }
    }
}
