package com.mobilerepair.shop.ui.master.faults

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilerepair.shop.MobileRepairApp
import com.mobilerepair.shop.data.model.CommonFault
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CommonFaultsViewModel : ViewModel() {

    private val dao = MobileRepairApp.instance.database.commonFaultDao()

    private val _faults = MutableStateFlow<List<CommonFault>>(emptyList())
    val faults: StateFlow<List<CommonFault>> = _faults

    init {
        viewModelScope.launch {
            dao.getAllFaults().collect { list ->
                _faults.value = list
            }
        }
    }

    fun addFault(name: String, defaultCharge: Double, category: String) {
        viewModelScope.launch {
            dao.insert(CommonFault(
                faultName = name,
                defaultCharge = defaultCharge,
                category = category
            ))
        }
    }

    fun deleteFault(fault: CommonFault) {
        viewModelScope.launch {
            dao.delete(fault)
        }
    }
}
