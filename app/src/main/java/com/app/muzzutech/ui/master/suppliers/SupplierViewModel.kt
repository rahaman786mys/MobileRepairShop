package com.app.muzzutech.ui.master.suppliers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.data.model.Supplier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SupplierViewModel : ViewModel() {

    private val dao = MobileRepairApp.instance.database.supplierDao()

    private val _suppliers = MutableStateFlow<List<Supplier>>(emptyList())
    val suppliers: StateFlow<List<Supplier>> = _suppliers

    init {
        viewModelScope.launch {
            dao.getAllSuppliers().collect { list ->
                _suppliers.value = list
            }
        }
    }

    fun save(name: String, company: String, mobile: String, email: String, address: String, city: String, gst: String) {
        viewModelScope.launch {
            dao.insert(Supplier(
                name = name,
                companyName = company,
                mobile = mobile,
                email = email,
                address = address,
                city = city,
                gstNo = gst
            ))
        }
    }

    suspend fun getSupplierByMobile(mobile: String): Supplier? = dao.getSupplierByMobile(mobile)
}
