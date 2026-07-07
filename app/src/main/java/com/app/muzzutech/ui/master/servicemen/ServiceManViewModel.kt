package com.app.muzzutech.ui.master.servicemen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.data.model.ServiceMan
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

    fun save(
        name: String,
        mobile: String,
        email: String,
        empId: String,
        designation: String,
        monthlySalary: Long = 0L,
        perDaySalary: Long = 0L
    ) {
        viewModelScope.launch {
            dao.insert(ServiceMan(
                name = name,
                mobile = mobile,
                email = email,
                employeeId = empId,
                designation = designation,
                monthlySalary = monthlySalary,
                perDaySalary = perDaySalary
            ))
        }
    }

    fun update(id: Long, name: String, mobile: String, email: String, empId: String, designation: String, monthlySalary: Long, perDaySalary: Long) {
        viewModelScope.launch {
            dao.update(ServiceMan(
                id = id,
                name = name,
                mobile = mobile,
                email = email,
                employeeId = empId,
                designation = designation,
                monthlySalary = monthlySalary,
                perDaySalary = perDaySalary
            ))
        }
    }

    fun delete(serviceMan: ServiceMan) {
        viewModelScope.launch {
            dao.delete(serviceMan)
        }
    }
}
