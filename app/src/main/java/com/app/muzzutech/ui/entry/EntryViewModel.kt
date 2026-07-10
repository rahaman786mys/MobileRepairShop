package com.app.muzzutech.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
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

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _photo1Path = MutableStateFlow<String?>(null)
    val photo1Path: StateFlow<String?> = _photo1Path

    private val _photo2Path = MutableStateFlow<String?>(null)
    val photo2Path: StateFlow<String?> = _photo2Path

    fun setPhoto1(path: String?) { _photo1Path.value = path }
    fun setPhoto2(path: String?) { _photo2Path.value = path }

    // Track draft entry IDs by mobile to prevent duplicates from autoSaveDraft
    private val draftEntryIds = mutableMapOf<String, Long>()

    fun resetSaveState() {
        _saveSuccess.value = null
        _saveError.value = null
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

    sealed class ContactResult {
        data class CustomerContact(val name: String?, val city: String?) : ContactResult()
        data class DealerContact(val name: String?, val city: String?) : ContactResult()
        object NotFound : ContactResult()
        data class Ambiguous(val customerName: String?, val dealerName: String?) : ContactResult()
    }

    suspend fun lookupContact(mobile: String, preferDealer: Boolean): ContactResult {
        val customer = customerDao.getCustomerByMobile(mobile)
        val dealer = dealerDao.getDealerByMobile(mobile)
        return when {
            customer != null && dealer != null -> ContactResult.Ambiguous(customer.name, dealer.name)
            dealer != null -> ContactResult.DealerContact(dealer.name, dealer.city)
            customer != null -> ContactResult.CustomerContact(customer.name, customer.city)
            else -> ContactResult.NotFound
        }
    }

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
        chargeAmount: Long = 0L,
        advanceAmount: Long = 0L,
        advanceMode: String = "CASH",
        advCash: Long = 0L,
        advOnline: Long = 0L,
        isDraft: Boolean = false
    ) {
        if (mobile.isBlank()) {
            _saveError.value = "Mobile number is required"
            return
        }
        val safeName = name.take(100)
        val safeCity = city.take(100)
        val safeBrand = brand.take(50)
        val safeModel = model.take(50)
        val safeExtraItems = extraItems.take(200)

        viewModelScope.launch {
            _isSaving.value = true
            _saveError.value = null
            var savedId: Long? = null
            try {
                db.withTransaction {
                    // Save/Update contact info
                    if (isDealer) {
                        dealerDao.insert(Dealer(mobile, safeName, safeCity))
                    } else {
                        customerDao.insert(Customer(mobile, safeName, safeCity))
                    }

                    // If we already have a draft for this mobile, update it instead of creating duplicate
                    val existingDraftId = if (isDraft) draftEntryIds[mobile] else null

                    if (existingDraftId != null) {
                        val existing = repository.getEntryById(existingDraftId)
                        if (existing != null) {
                            repository.update(existing.copy(
                                entryPhotoPath = photoPath.ifEmpty { existing.entryPhotoPath },
                                entryPhotoPath2 = photoPath2.ifEmpty { existing.entryPhotoPath2 },
                                customerName = if (!isDealer) safeName else existing.customerName,
                                customerMobile = if (!isDealer) mobile else existing.customerMobile,
                                customerCity = safeCity,
                                dealerName = if (isDealer) safeName else existing.dealerName,
                                dealerMobile = if (isDealer) mobile else existing.dealerMobile,
                                serviceManId = serviceManId,
                                deviceBrand = safeBrand.ifEmpty { existing.deviceBrand },
                                deviceModel = safeModel.ifEmpty { existing.deviceModel },
                                faultDescription = safeExtraItems,
                                isDraft = true
                            ))
                            return@withTransaction
                        }
                    }

                    val entry = RepairEntry(
                        entryPhotoPath = photoPath,
                        entryPhotoPath2 = photoPath2,
                        customerName = if (!isDealer) safeName else "",
                        customerMobile = if (!isDealer) mobile else "",
                        customerCity = safeCity,
                        dealerName = if (isDealer) safeName else "",
                        dealerMobile = if (isDealer) mobile else "",
                        serviceManId = serviceManId,
                        deviceBrand = safeBrand,
                        deviceModel = safeModel,
                        entryDate = System.currentTimeMillis(),
                        faultDescription = safeExtraItems,
                        chargeAmount = chargeAmount,
                        advanceAmount = advanceAmount,
                        isDraft = isDraft
                    )
                    val id = repository.insert(entry)

                    if (advanceAmount > 0L) {
                        val personMobile = mobile
                        val personName = safeName
                        val personType = if (!isDealer) "CUSTOMER" else "DEALER"

                        val paymentId = db.paymentDao().insert(
                            com.app.muzzutech.data.model.Payment(
                                personType = personType,
                                personMobile = personMobile,
                                personName = personName,
                                description = "Repair: $safeBrand $safeModel",
                                totalAmount = chargeAmount,
                                paidAmount = advanceAmount,
                                dueAmount = (chargeAmount - advanceAmount).coerceAtLeast(0L),
                                status = if (advanceAmount >= chargeAmount) "PAID" else "PARTIAL",
                                linkedEntryId = id
                            )
                        )

                        if (advanceMode == "BOTH") {
                            if (advCash > 0) {
                                db.paymentTransactionDao().insert(
                                    PaymentTransaction(
                                        paymentId = paymentId,
                                        personType = personType,
                                        personMobile = personMobile,
                                        personName = personName,
                                        amount = advCash,
                                        paymentMode = "CASH",
                                        note = "Advance (Cash) for $safeBrand $safeModel"
                                    )
                                )
                            }
                            if (advOnline > 0) {
                                db.paymentTransactionDao().insert(
                                    PaymentTransaction(
                                        paymentId = paymentId,
                                        personType = personType,
                                        personMobile = personMobile,
                                        personName = personName,
                                        amount = advOnline,
                                        paymentMode = "ONLINE",
                                        note = "Advance (Online) for $safeBrand $safeModel"
                                    )
                                )
                            }
                        } else {
                            db.paymentTransactionDao().insert(
                                PaymentTransaction(
                                    paymentId = paymentId,
                                    personType = personType,
                                    personMobile = personMobile,
                                    personName = personName,
                                    amount = advanceAmount,
                                    paymentMode = advanceMode,
                                    note = "Advance ($advanceMode) for $safeBrand $safeModel"
                                )
                            )
                        }
                    }

                    if (isDraft) {
                        draftEntryIds[mobile] = id
                    } else {
                        draftEntryIds.remove(mobile)
                        savedId = id
                    }
                }
                // Update UI state outside the transaction (after commit)
                if (!isDraft && savedId != null) {
                    _saveSuccess.value = savedId
                }
            } catch (e: Exception) {
                _saveError.value = e.message ?: "Failed to save entry"
                android.util.Log.e("EntryViewModel", "saveEntry failed", e)
            } finally {
                _isSaving.value = false
            }
        }
    }
}
