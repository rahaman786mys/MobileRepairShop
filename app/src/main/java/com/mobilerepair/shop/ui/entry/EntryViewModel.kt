package com.mobilerepair.shop.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilerepair.shop.MobileRepairApp
import com.mobilerepair.shop.data.model.RepairEntry
import com.mobilerepair.shop.data.model.ServiceMan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EntryViewModel : ViewModel() {

    private val repository = MobileRepairApp.instance.repairRepository
    private val serviceManDao = MobileRepairApp.instance.database.serviceManDao()

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

    fun saveEntry(
        photoPath: String,
        customerName: String,
        customerMobile: String,
        customerCity: String,
        dealerName: String,
        dealerMobile: String,
        serviceManId: Long
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val entry = RepairEntry(
                entryPhotoPath = photoPath,
                customerName = customerName,
                customerMobile = customerMobile,
                customerCity = customerCity,
                dealerName = dealerName,
                dealerMobile = dealerMobile,
                serviceManId = serviceManId,
                entryDate = System.currentTimeMillis()
            )
            val id = repository.insert(entry)
            _saveSuccess.value = id
            _isSaving.value = false
        }
    }
}
