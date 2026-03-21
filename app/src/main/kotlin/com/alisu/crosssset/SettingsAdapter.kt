package com.alisu.crosssset

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alisu.crosssset.databinding.ItemSettingBinding

class SettingsAdapter(private val onItemClick: (SettingsItem) -> Unit) :
    ListAdapter<SettingsItem, SettingsAdapter.SettingViewHolder>(SettingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingViewHolder {
        val binding = ItemSettingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SettingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SettingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SettingViewHolder(private val binding: ItemSettingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SettingsItem) {
            binding.settingKey.text = item.key
            binding.settingValue.text = item.value
            
            val context = binding.root.context
            val color = when (item.riskLevel) {
                RiskLevel.SAFE -> context.getColor(R.color.risk_safe)
                RiskLevel.MODERATE -> context.getColor(R.color.risk_moderate)
                RiskLevel.DANGEROUS -> context.getColor(R.color.risk_dangerous)
            }
            
            // Apply color to the indicator dot
            binding.riskIndicator.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
            
            if (item.description != null) {
                binding.settingDescription.text = item.description
                binding.settingDescription.visibility = View.VISIBLE
            } else {
                binding.settingDescription.text = context.getString(R.string.no_description)
                binding.settingDescription.visibility = View.VISIBLE
            }

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    class SettingDiffCallback : DiffUtil.ItemCallback<SettingsItem>() {
        override fun areItemsTheSame(oldItem: SettingsItem, newItem: SettingsItem): Boolean {
            return oldItem.key == newItem.key && oldItem.table == newItem.table
        }

        override fun areContentsTheSame(oldItem: SettingsItem, newItem: SettingsItem): Boolean {
            return oldItem == newItem
        }
    }
}
