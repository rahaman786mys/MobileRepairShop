package com.app.muzzutech.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.muzzutech.R
import com.app.muzzutech.data.model.RepairEntry
import com.app.muzzutech.utils.DateUtils

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
        private val viewStatusAccent: View = itemView.findViewById(R.id.viewStatusAccent)
        private val cardStatus: com.google.android.material.card.MaterialCardView = itemView.findViewById(R.id.cardStatus)

        fun bind(entry: RepairEntry) {
            tvCustomer.text = entry.customerName
            tvMobile.text = entry.customerMobile
            tvFault.text = entry.faultDetected.ifEmpty { "No fault set" }
            tvStatus.text = entry.workStatus.uppercase()
            tvDate.text = DateUtils.formatDateTime(entry.createdAt)

            val amount = if (entry.finalAmount > 0) entry.finalAmount else entry.chargeAmount
            tvAmount.text = com.app.muzzutech.utils.PriceUtils.formatPrice(amount)

            // Status theme binding
            val context = itemView.context
            val statusColor = when (entry.workStatus) {
                "Done" -> context.getColor(R.color.muzzu_success)
                "InProgress" -> context.getColor(R.color.muzzu_primary)
                "Ready" -> context.getColor(R.color.muzzu_warning)
                else -> context.getColor(R.color.muzzu_text_sub)
            }

            viewStatusAccent.setBackgroundColor(statusColor)
            tvStatus.setTextColor(statusColor)
            cardStatus.setCardBackgroundColor(statusColor.applyAlpha(0.15f))

            itemView.setOnClickListener { onItemClick(entry) }
        }

        private fun Int.applyAlpha(alpha: Float): Int {
            return Color.argb(
                (alpha * 255).toInt(),
                android.graphics.Color.red(this),
                android.graphics.Color.green(this),
                android.graphics.Color.blue(this)
            )
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<RepairEntry>() {
        override fun areItemsTheSame(oldItem: RepairEntry, newItem: RepairEntry): Boolean =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: RepairEntry, newItem: RepairEntry): Boolean =
            oldItem == newItem
    }
}
