package com.mobilerepair.shop.ui.master.servicemen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilerepair.shop.MobileRepairApp
import com.mobilerepair.shop.data.model.ServiceMan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ServiceManViewModel : ViewModel() {

    private val dao = MobileRepairApp.instance.database.serviceManDao()

    private val _serviceMen = MutableStateFlow<List<ServiceMan>>(emptyList())
    val serviceMen: StateFlow<List<ServiceMan>> = _serviceMen

    init {
        viewModelScope.launch {
            dao.getAllServiceMen().collect { list ->
                _serviceMen.value = list
            }
        }
    }

    fun save(name: String, mobile: String, email: String, empId: String, designation: String) {
        viewModelScope.launch {
            dao.insert(ServiceMan(
                name = name,
                mobile = mobile,
                email = email,
                employeeId = empId,
                designation = designation
            ))
        }
    }

    fun delete(serviceMan: ServiceMan) {
        viewModelScope.launch {
            dao.delete(serviceMan)
        }
    }
}
