package com.familyrecipes.android.ui.adapter

import com.familyrecipes.android.ui.design.UiStrings
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.familyrecipes.android.data.model.FridgeItem
import com.familyrecipes.android.databinding.ItemFridgeBinding

/**
 * 冰箱食材适配器
 */
class FridgeAdapter(
    private val items: List<FridgeItem>,
    private val onConsumeClick: (FridgeItem) -> Unit,
    private val onDeleteClick: (FridgeItem) -> Unit,
    private val onItemClick: (FridgeItem) -> Unit = {},
    private val onManage: (FridgeItem) -> Unit = {}
) : RecyclerView.Adapter<FridgeAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemFridgeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FridgeItem) {
            binding.tvName.text = item.ingredient?.name ?: UiStrings.s(com.familyrecipes.android.R.string.ui_5ab356bba3)
            binding.tvAmount.text = UiStrings.s(com.familyrecipes.android.R.string.ui_4b123f5f93) + (item.amount?.takeIf { it.isNotBlank() } ?: UiStrings.s(com.familyrecipes.android.R.string.ui_5c6881399f)) + (item.ownerUsername?.let { UiStrings.f(com.familyrecipes.android.R.string.reserved_value, it) } ?: "")
            binding.tvExpiryDate.text = UiStrings.f(com.familyrecipes.android.R.string.expiry_value, item.expiryDate)
            binding.tvStorage.text = "位置：${item.storageLocation ?: UiStrings.s(com.familyrecipes.android.R.string.ui_fe2d26a257)}"
            binding.tvCategory.text = (item.categoryOverrideName ?: item.ingredient?.category ?: "待分类") + " · " + (item.storageLocation ?: UiStrings.s(com.familyrecipes.android.R.string.ui_fe2d26a257))
            
            // 根据状态设置颜色
            when (item.status) {
                FridgeItem.STATUS_EXPIRED -> {
                    binding.tvStatus.text = UiStrings.s(com.familyrecipes.android.R.string.ui_4d5ccd0118)
                    binding.tvStatus.setTextColor(Color.RED)
                }
                FridgeItem.STATUS_EXPIRING -> {
                    binding.tvStatus.text = UiStrings.s(com.familyrecipes.android.R.string.ui_ac148b4c87)
                    binding.tvStatus.setTextColor(Color.parseColor("#FF9800"))
                }
                FridgeItem.STATUS_CONSUMED -> { binding.tvStatus.text=UiStrings.s(com.familyrecipes.android.R.string.ui_d813eea6b6);binding.tvStatus.setTextColor(com.familyrecipes.android.ui.design.Ui.muted) }
                else -> {
                    binding.tvStatus.text = UiStrings.s(com.familyrecipes.android.R.string.ui_fd6e80f1e0)
                    binding.tvStatus.setTextColor(Color.parseColor("#4CAF50"))
                }
            }
            
            binding.btnMore.setOnClickListener { onManage(item) }
            binding.root.setOnLongClickListener { onManage(item);true }
            // 点击食材卡片，跳转到相关菜谱
            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFridgeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size
}

