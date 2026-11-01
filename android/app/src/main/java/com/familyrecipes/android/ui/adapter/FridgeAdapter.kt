package com.familyrecipes.android.ui.adapter

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
            binding.tvName.text = item.ingredient?.name ?: "未知食材"
            binding.tvAmount.text = "数量：" + (item.amount?.takeIf { it.isNotBlank() } ?: "未填写") + (item.ownerUsername?.let { " · 预留给 $it" } ?: "")
            binding.tvExpiryDate.text = "到期：${item.expiryDate}"
            binding.tvStorage.text = "位置：${item.storageLocation ?: "未设置"}"
            binding.tvCategory.text = (item.categoryOverrideName ?: item.ingredient?.category ?: "待分类") + " · " + (item.storageLocation ?: "未设置")
            
            // 根据状态设置颜色
            when (item.status) {
                FridgeItem.STATUS_EXPIRED -> {
                    binding.tvStatus.text = "已过期"
                    binding.tvStatus.setTextColor(Color.RED)
                }
                FridgeItem.STATUS_EXPIRING -> {
                    binding.tvStatus.text = "即将过期"
                    binding.tvStatus.setTextColor(Color.parseColor("#FF9800"))
                }
                FridgeItem.STATUS_CONSUMED -> { binding.tvStatus.text="已吃完";binding.tvStatus.setTextColor(com.familyrecipes.android.ui.design.Ui.muted) }
                else -> {
                    binding.tvStatus.text = "正常"
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

