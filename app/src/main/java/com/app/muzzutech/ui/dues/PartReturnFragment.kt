package com.app.muzzutech.ui.dues

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.R
import com.app.muzzutech.data.model.PartReturn
import com.app.muzzutech.databinding.FragmentPartReturnBinding
import androidx.room.withTransaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

class PartReturnFragment : Fragment(R.layout.fragment_part_return) {

    private var _binding: FragmentPartReturnBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPartReturnBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupReasonSpinner()
        loadSupplierParts()
        setupListeners()
    }

    private fun setupReasonSpinner() {
        val reasons = listOf("Defective", "Wrong Item", "Not Needed", "Damaged", "Other")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, reasons)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerReturnReason.adapter = adapter
    }

    private fun loadSupplierParts() {
        viewLifecycleOwner.lifecycleScope.launch {
            val db = MobileRepairApp.instance.database
            val parts = db.sparePartPurchaseDao().getAllPurchases()
            parts.collect { list ->
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    list.map { "${it.partName} (₹${it.purchasePrice}) - ${it.supplierName}" }
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerSelectPart.adapter = adapter
                if (list.isNotEmpty()) {
                    binding.tvSupplierInfo.text = "Supplier: ${list[0].supplierName} (${list[0].supplierId})"
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnReturnPart.setOnClickListener {
            val partName = binding.etPartName.text.toString().trim()
            val refundStr = binding.etRefundAmount.text.toString().trim()
            val refund = ((refundStr.toDoubleOrNull() ?: 0.0) * 100).roundToLong()

            if (partName.isEmpty()) {
                Snackbar.make(binding.root, "Enter part name", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                val db = MobileRepairApp.instance.database
                val partsList = db.sparePartPurchaseDao().getAllPurchases().first()

                val selectedPos = binding.spinnerSelectPart.selectedItemPosition
                if (selectedPos >= 0 && selectedPos < partsList.size) {
                    val selectedPart = partsList[selectedPos]
                    val reason = binding.spinnerReturnReason.selectedItem.toString()

                    val partReturn = PartReturn(
                        supplierId = selectedPart.supplierId,
                        supplierName = selectedPart.supplierName,
                        partName = partName,
                        returnReason = reason,
                        refundAmount = refund
                    )

                    db.withTransaction {
                        val partReturnId = db.partReturnDao().insert(partReturn)

                        val linkedPayment = db.paymentDao().getPaymentByLinkedPartId(selectedPart.id)
                        if (linkedPayment != null && refund > 0L) {
                            val reducedTotal = (linkedPayment.totalAmount - refund).coerceAtLeast(0L)
                            val reducedPaid = (linkedPayment.paidAmount - refund).coerceAtLeast(0L)
                            val reducedDue = (reducedTotal - reducedPaid).coerceAtLeast(0L)
                            val newStatus = when {
                                reducedDue <= 0L -> "PAID"
                                reducedPaid > 0L -> "PARTIAL"
                                else -> "UNPAID"
                            }
                            val updatedPayment = linkedPayment.copy(
                                totalAmount = reducedTotal,
                                paidAmount = reducedPaid,
                                dueAmount = reducedDue,
                                status = newStatus,
                                updatedAt = System.currentTimeMillis()
                            )
                            db.paymentDao().update(updatedPayment)

                            // Record the cash-in refund as a linked transaction so the
                            // auditor and Reports screen reflect the true cash flow.
                            val refundTxnId = db.paymentTransactionDao().insert(
                                com.app.muzzutech.data.model.PaymentTransaction(
                                    paymentId = linkedPayment.id,
                                    personType = "SUPPLIER",
                                    personMobile = linkedPayment.personMobile,
                                    personName = "${linkedPayment.personName} (Refund)",
                                    amount = refund,
                                    paymentMode = "CASH",
                                    note = "Part return refund: $partName (Return #$partReturnId)"
                                )
                            )
                            db.partReturnDao().update(
                                partReturn.copy(id = partReturnId, refundTransactionId = refundTxnId)
                            )
                        }
                    }

                    Snackbar.make(binding.root, "Part return recorded! Supplier due reduced.", Snackbar.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
