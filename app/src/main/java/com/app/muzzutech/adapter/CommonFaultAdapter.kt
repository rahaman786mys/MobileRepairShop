package com.app.muzzutech.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.muzzutech.data.model.CommonFault
import com.app.muzzutech.databinding.ItemCommonFaultBinding

class CommonFaultAdapter(private val onFaultClick: (CommonFault) -> Unit) :
    ListAdapter<CommonFault, CommonFaultAdapter.FaultViewHolder>(FaultDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaultViewHolder {
        val binding = ItemCommonFaultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FaultViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FaultViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FaultViewHolder(private val binding: ItemCommonFaultBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(fault: CommonFault) {
            binding.tvFaultName.text = fault.faultName
            binding.tvCategory.text = fault.category
            binding.tvDefaultCharge.text = "₹${fault.defaultCharge}"
            
            binding.root.setOnClickListener {
                onFaultClick(fault)
            }
        }
    }

    class FaultDiffCallback : DiffUtil.ItemCallback<CommonFault>() {
        override fun areItemsTheSame(oldItem: CommonFault, newItem: CommonFault): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CommonFault, newItem: CommonFault): Boolean {
            return oldItem == newItem
        }
    }
}
