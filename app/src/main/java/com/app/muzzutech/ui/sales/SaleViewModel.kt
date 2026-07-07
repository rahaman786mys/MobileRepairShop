package com.app.muzzutech.ui.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.data.model.Payment
import com.app.muzzutech.data.model.PaymentTransaction
import com.app.muzzutech.data.model.Sale
import com.app.muzzutech.data.model.Supplier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ViewModel for the direct sale flow.
 *
 * Responsibilities:
 *  1. Load active suppliers for the supplier picker.
 *  2. Persist a sale atomically with its accounting side-effects:
 *     - The Sale row itself.
 *     - A SUPPLIER Payment row with [Payment.linkedSaleId] populated (this is the gap
 *       that was previously missing: supplier dues from direct sales were never visible
 *       in the Dues / Reports screens because no Payment record was created).
 *     - A CUSTOMER cash-in PaymentTransaction (revenue).
 *     - A SUPPLIER cash-out PaymentTransaction (expense) if purchasePrice > 0.
 *
 * All writes happen inside a single [androidx.room.withTransaction] block.
 */
class SaleViewModel : ViewModel() {

    private val database = MobileRepairApp.instance.database
    private val supplierDao = database.supplierDao()
    private val saleDao = database.saleDao()
    private val paymentDao = database.paymentDao()
    private val paymentTransactionDao = database.paymentTransactionDao()

    private val _suppliers = MutableStateFlow<List<Supplier>>(emptyList())
    val suppliers: StateFlow<List<Supplier>> = _suppliers

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _saveResult = MutableStateFlow<SaveResult?>(null)
    val saveResult: StateFlow<SaveResult?> = _saveResult

    init {
        loadSuppliers()
    }

    private fun loadSuppliers() {
        viewModelScope.launch {
            supplierDao.getActiveSuppliers().collectLatest { list ->
                _suppliers.value = list
            }
        }
    }

    /**
     * Persist a direct sale. Caller passes the supplier picked by the user.
     *
     * @param supplier the supplier whose item is being sold (must be selected).
     */
    fun saveSale(
        itemName: String,
        purchasePrice: Long,
        salePrice: Long,
        supplier: Supplier
    ) {
        if (_isSaving.value) return
        if (itemName.isBlank()) {
            _saveResult.value = SaveResult.Error("Enter item name")
            return
        }

        val safeItemName = itemName.take(100)
        viewModelScope.launch {
            _isSaving.value = true
            try {
                database.withTransaction {
                    // 1. Sale row
                    val sale = Sale(
                        itemName = safeItemName,
                        supplierId = supplier.mobile,
                        supplierName = supplier.name,
                        purchasePrice = purchasePrice,
                        salePrice = salePrice,
                        paidToSupplier = purchasePrice,
                        supplierDue = 0L,
                        customerPaid = salePrice,
                        customerDue = 0L
                    )
                    val saleId = saleDao.insert(sale)

                    // 2. SUPPLIER Payment row — fixes the linkedSaleId gap.
                    //    Surfaces the supplier obligation in Dues/Reports screens.
                    val supplierPayment = Payment(
                        personType = "SUPPLIER",
                        personMobile = supplier.mobile,
                        personName = supplier.name,
                        description = "Direct Sale: $safeItemName",
                        totalAmount = purchasePrice,
                        paidAmount = purchasePrice,
                        dueAmount = 0L,
                        status = "PAID",
                        linkedSaleId = saleId
                    )
                    val supplierPaymentId = paymentDao.insert(supplierPayment)

                    // 3. Cash inflow (revenue from cash customer)
                    paymentTransactionDao.insert(
                        PaymentTransaction(
                            paymentId = null,
                            personType = "CUSTOMER",
                            personMobile = "DIRECT_SALE",
                            personName = "Cash Customer",
                            amount = salePrice,
                            paymentMode = "CASH",
                            note = "Direct Sale: $safeItemName"
                        )
                    )

                    // 4. Cash outflow (supplier payment) — only if we actually paid
                    if (purchasePrice > 0L) {
                        paymentTransactionDao.insert(
                            PaymentTransaction(
                                paymentId = supplierPaymentId,
                                personType = "SUPPLIER",
                                personMobile = supplier.mobile,
                                personName = supplier.name,
                                amount = purchasePrice,
                                paymentMode = "CASH",
                                note = "Purchase for Direct Sale: $itemName"
                            )
                        )
                    }
                }
                _saveResult.value = SaveResult.Success
            } catch (e: Exception) {
                _saveResult.value = SaveResult.Error(e.message ?: "Failed to save sale")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun consumeResult() {
        _saveResult.value = null
    }

    sealed class SaveResult {
        object Success : SaveResult()
        data class Error(val message: String) : SaveResult()
    }
}
