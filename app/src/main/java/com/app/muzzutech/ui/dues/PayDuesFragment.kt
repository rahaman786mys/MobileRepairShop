package com.app.muzzutech.ui.dues

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.app.muzzutech.R
import com.app.muzzutech.data.model.PaymentTransaction
import com.app.muzzutech.databinding.FragmentPayDuesBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PayDuesFragment : Fragment(R.layout.fragment_pay_dues) {

    private var _binding: FragmentPayDuesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DuesViewModel by viewModels()

    private var paymentId: Long = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPayDuesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        paymentId = arguments?.getLong("paymentId", 0) ?: 0

        if (paymentId == 0L) {
            Snackbar.make(binding.root, "Invalid payment", Snackbar.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        setupPaymentModeSpinner()
        loadPaymentDetails()
        setupListeners()
        observePaymentHistory()
    }

    private fun setupPaymentModeSpinner() {
        val modes = listOf("Cash", "Online (UPI)", "Online (Bank)")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, modes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPaymentMode.adapter = adapter
    }

    private fun loadPaymentDetails() {
        viewLifecycleOwner.lifecycleScope.launch {
            val db = com.app.muzzutech.MobileRepairApp.instance.database
            val payment = db.paymentDao().getPaymentById(paymentId)
            payment?.let { p ->
                binding.tvPersonName.text = p.personName.ifEmpty { p.personMobile }
                binding.tvPersonMobile.text = p.personMobile
                binding.tvTotalAmount.text = com.app.muzzutech.utils.PriceUtils.formatPrice(p.totalAmount)
                binding.tvPaidAmount.text = com.app.muzzutech.utils.PriceUtils.formatPrice(p.paidAmount)
                binding.tvDueAmount.text = com.app.muzzutech.utils.PriceUtils.formatPrice(p.dueAmount)
                binding.tvDescription.text = p.description
                binding.etAmount.setText(p.dueAmount.toInt().toString())
            }
        }
    }

    private fun setupListeners() {
        binding.btnRecordPayment.setOnClickListener {
            val amountStr = binding.etAmount.text.toString().trim()
            val amount = amountStr.toDoubleOrNull() ?: 0.0
            val note = binding.etNote.text.toString().trim()
            val mode = binding.spinnerPaymentMode.selectedItemPosition

            if (amount <= 0) {
                Snackbar.make(binding.root, "Enter valid amount", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                val db = com.app.muzzutech.MobileRepairApp.instance.database
                val payment = db.paymentDao().getPaymentById(paymentId)
                if (payment != null) {
                    val modeStr = when (mode) { 1 -> "ONLINE"; 2 -> "ONLINE"; else -> "CASH" }
                    viewModel.recordPayment(payment, amount, modeStr, note)
                    Snackbar.make(binding.root, "Payment recorded!", Snackbar.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            }
        }

        binding.btnPartReturn.setOnClickListener {
            val bundle = Bundle().apply { putLong("paymentId", paymentId) }
            findNavController().navigate(R.id.partReturnFragment, bundle)
        }
    }

    private fun observePaymentHistory() {
        viewLifecycleOwner.lifecycleScope.launch {
            val db = com.app.muzzutech.MobileRepairApp.instance.database
            val payment = db.paymentDao().getPaymentById(paymentId)
            payment?.let {
                viewModel.loadPaymentHistory(it.personMobile)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.paymentHistory.collectLatest { transactions ->
                    if (transactions.isNotEmpty()) {
                        binding.layoutHistory.visibility = View.VISIBLE
                        binding.tvHistory.text = transactions.joinToString("\n") { t ->
                            "${com.app.muzzutech.utils.PriceUtils.formatPrice(t.amount)} - ${t.paymentMode} - ${com.app.muzzutech.utils.DateUtils.formatDateTime(t.transactionDate)}"
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
