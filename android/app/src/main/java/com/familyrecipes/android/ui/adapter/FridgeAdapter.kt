package com.familyrecipes.android.ui.adapter

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
            binding.tvName.text = item.displayName?.takeIf{it.isNotBlank()} ?: item.ingredient?.name ?: binding.root.context.getString(com.familyrecipes.android.R.string.ui_5ab356bba3)
            com.bumptech.glide.Glide.with(binding.ivIcon).clear(binding.ivIcon)
            val icon=com.familyrecipes.android.util.IngredientIcon.drawable(item.ingredient?.name.orEmpty(),item.categoryOverrideName?:item.ingredient?.category.orEmpty())
            binding.ivIcon.setImageDrawable(icon)
            if(!item.imageUrl.isNullOrBlank())com.bumptech.glide.Glide.with(binding.ivIcon).load(com.familyrecipes.android.util.MediaImages.model(item.imageUrl)).placeholder(icon).error(icon).centerCrop().into(binding.ivIcon)
            val storage=when(item.storageLocation){"常温","Room temperature"->binding.root.context.getString(com.familyrecipes.android.R.string.receipt_room);"冷藏","冰箱冷藏","Refrigerated"->binding.root.context.getString(com.familyrecipes.android.R.string.receipt_cold);"冷冻","冰箱冷冻","Frozen"->binding.root.context.getString(com.familyrecipes.android.R.string.receipt_frozen);else->item.storageLocation?:binding.root.context.getString(com.familyrecipes.android.R.string.receipt_unspecified)}
            binding.tvAmount.text = listOfNotNull(
                item.amount?.takeIf{it.isNotBlank()},
                item.categoryOverrideName ?: item.ingredient?.category ?: binding.root.context.getString(com.familyrecipes.android.R.string.inventory_unclassified),
                storage,
                item.ownerUsername?.let{binding.root.context.getString(com.familyrecipes.android.R.string.reserved_value,it).trim()}
            ).joinToString(" · ")
            val status = when(item.status) {
                FridgeItem.STATUS_EXPIRED -> com.familyrecipes.android.R.string.ui_4d5ccd0118
                FridgeItem.STATUS_EXPIRING -> com.familyrecipes.android.R.string.ui_ac148b4c87
                FridgeItem.STATUS_CONSUMED -> com.familyrecipes.android.R.string.ui_d813eea6b6
                else -> null
            }
            val expiry = item.expiryDate?.let{if(status!=null)it else binding.root.context.getString(com.familyrecipes.android.R.string.expiry_value,it)}
                ?: binding.root.context.getString(com.familyrecipes.android.R.string.inventory_unknown_expiry)
            binding.tvExpiryDate.text = listOfNotNull(status?.let{binding.root.context.getString(it)},expiry).joinToString(" · ")
            binding.tvExpiryDate.setTextColor(binding.root.context.getColor(
                if(item.status==FridgeItem.STATUS_EXPIRED || item.status==FridgeItem.STATUS_EXPIRING)com.familyrecipes.android.R.color.primary else com.familyrecipes.android.R.color.text_secondary))

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

