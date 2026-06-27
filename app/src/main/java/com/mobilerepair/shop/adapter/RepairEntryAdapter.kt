package com.mobilerepair.shop.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mobilerepair.shop.R
import com.mobilerepair.shop.data.model.RepairEntry
import com.mobilerepair.shop.utils.DateUtils

class RepairEntryAdapter(
    private val onItemClick: (RepairEntry) -> Unit
) : ListAdapter<RepairEntry, RepairEntryAdapter.EntryViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_repair_entry, parent, false)
        return EntryViewHolder(view)
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCustomer: TextView = itemView.findViewById(R.id.tvCustomerName)
        private val tvMobile: TextView = itemView.findViewById(R.id.tvCustomerMobile)
        private val tvFault: TextView = itemView.findViewById(R.id.tvFault)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)

        fun bind(entry: RepairEntry) {
            tvCustomer.text = entry.customerName
            tvMobile.text = entry.customerMobile
            tvFault.text = entry.faultDetected.ifEmpty { "No fault set" }
            tvStatus.text = entry.workStatus
            tvDate.text = DateUtils.formatDateTime(entry.createdAt)

            val amount = if (entry.finalAmount > 0) entry.finalAmount else entry.chargeAmount
            tvAmount.text = "₹ ${String.format("%.0f", amount)}"

            // Status color
            tvStatus.setTextColor(
                when (entry.workStatus) {
                    "Done" -> itemView.context.getColor(R.color.status_done)
                    "InProgress" -> itemView.context.getColor(R.color.status_progress)
                    else -> itemView.context.getColor(R.color.status_pending)
                }
            )

            itemView.setOnClickListener { onItemClick(entry) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<RepairEntry>() {
        override fun areItemsTheSame(oldItem: RepairEntry, newItem: RepairEntry): Boolean =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: RepairEntry, newItem: RepairEntry): Boolean =
            oldItem == newItem
    }
}
