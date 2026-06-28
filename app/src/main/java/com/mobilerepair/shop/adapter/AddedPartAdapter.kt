package com.mobilerepair.shop.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mobilerepair.shop.R
import com.mobilerepair.shop.data.model.SparePartPurchase
import com.mobilerepair.shop.databinding.ItemAddedPartBinding
import java.io.File

class AddedPartAdapter(private val onDeleteClick: (SparePartPurchase) -> Unit) :
    ListAdapter<SparePartPurchase, AddedPartAdapter.PartViewHolder>(PartDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PartViewHolder {
        val binding = ItemAddedPartBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PartViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PartViewHolder(private val binding: ItemAddedPartBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(part: SparePartPurchase) {
            binding.tvPartName.text = part.partName
            binding.tvSupplier.text = "Supplier: ${part.supplierName}"
            binding.tvPrice.text = "₹${part.purchasePrice}"

            if (part.partPhotoPath.isNotEmpty()) {
                Glide.with(binding.ivPartImage.context)
                    .load(File(part.partPhotoPath))
                    .placeholder(R.drawable.ic_add)
                    .into(binding.ivPartImage)
            } else {
                binding.ivPartImage.setImageResource(R.drawable.ic_add)
            }

            binding.btnDeletePart.setOnClickListener {
                onDeleteClick(part)
            }
        }
    }

    class PartDiffCallback : DiffUtil.ItemCallback<SparePartPurchase>() {
        override fun areItemsTheSame(oldItem: SparePartPurchase, newItem: SparePartPurchase): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SparePartPurchase, newItem: SparePartPurchase): Boolean {
            return oldItem == newItem
        }
    }
}
