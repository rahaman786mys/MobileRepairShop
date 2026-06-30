package com.app.muzzutech.ui.inspection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.data.model.CommonFault
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class InspectionViewModel : ViewModel() {

    private val faultDao = MobileRepairApp.instance.database.commonFaultDao()

    private val _commonFaults = MutableStateFlow<List<CommonFault>>(emptyList())
    val commonFaults: StateFlow<List<CommonFault>> = _commonFaults

    init {
        loadFaults()
    }

    private fun loadFaults() {
        viewModelScope.launch {
            faultDao.getActiveFaults().collect { faults ->
                _commonFaults.value = faults
            }
        }
    }
}
