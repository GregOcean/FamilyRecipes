package com.familyrecipes.android.ui.fridge.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.familyrecipes.android.R
import com.familyrecipes.android.data.model.InventoryCategory

/**
 * 库存分类侧边栏适配器
 */
class CategorySidebarAdapter(
    private val onCategoryClick: (InventoryCategory?, isExpanded: Boolean) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<CategoryItem>()
    private var selectedCategoryId: Long? = null
    private val expandedCategories = mutableSetOf<Long>() // 记录展开的一级分类

    companion object {
        private const val TYPE_ALL = 0
        private const val TYPE_PARENT = 1
        private const val TYPE_CHILD = 2
    }

    data class CategoryItem(
        val category: InventoryCategory? = null, // null 表示"全部"
        val isChild: Boolean = false,
        val isAll: Boolean = false
    )

    fun submitList(categories: List<InventoryCategory>) {
        items.clear()
        
        // 添加"全部"选项
        items.add(CategoryItem(isAll = true))
        
        // 添加分类
        categories.forEach { parent ->
            items.add(CategoryItem(category = parent, isChild = false))
            
            // 如果该分类已展开，添加子分类
            if (expandedCategories.contains(parent.id)) {
                parent.children?.forEach { child ->
                    items.add(CategoryItem(category = child, isChild = true))
                }
            }
        }
        
        notifyDataSetChanged()
    }

    fun setSelectedCategory(categoryId: Long?) {
        selectedCategoryId = categoryId
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            items[position].isAll -> TYPE_ALL
            items[position].isChild -> TYPE_CHILD
            else -> TYPE_PARENT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_ALL, TYPE_PARENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_category_parent, parent, false)
                ParentViewHolder(view)
            }
            TYPE_CHILD -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_category_child, parent, false)
                ChildViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        
        when (holder) {
            is ParentViewHolder -> {
                if (item.isAll) {
                    holder.bind("全部", null, selectedCategoryId == null)
                } else {
                    val isExpanded = item.category?.let { expandedCategories.contains(it.id) } ?: false
                    holder.bind(
                        item.category?.icon + " " + item.category?.name,
                        item.category?.id,
                        selectedCategoryId == item.category?.id
                    )
                }
                
                holder.itemView.setOnClickListener {
                    if (item.isAll) {
                        // 点击"全部"
                        selectedCategoryId = null
                        notifyDataSetChanged()
                        onCategoryClick(null, false)
                    } else {
                        item.category?.let { category ->
                            // 切换展开/收起状态
                            val wasExpanded = expandedCategories.contains(category.id)
                            if (wasExpanded) {
                                expandedCategories.remove(category.id)
                            } else {
                                expandedCategories.add(category.id)
                            }
                            
                            // 选中该分类
                            selectedCategoryId = category.id
                            
                            // 重新构建列表
                            val currentCategories = items
                                .filter { !it.isAll && !it.isChild }
                                .mapNotNull { it.category }
                            submitList(currentCategories)
                            
                            // 通知点击
                            onCategoryClick(category, !wasExpanded)
                        }
                    }
                }
            }
            is ChildViewHolder -> {
                holder.bind(
                    item.category?.name ?: "",
                    item.category?.id,
                    selectedCategoryId == item.category?.id
                )
                
                holder.itemView.setOnClickListener {
                    item.category?.let { category ->
                        selectedCategoryId = category.id
                        notifyDataSetChanged()
                        onCategoryClick(category, false)
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class ParentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textView: TextView = view.findViewById(R.id.tv_category_name)
        
        fun bind(name: String, categoryId: Long?, isSelected: Boolean) {
            textView.text = name
            
            if (isSelected) {
                itemView.setBackgroundColor(itemView.context.getColor(R.color.category_parent_selected_bg))
                textView.setTextColor(itemView.context.getColor(R.color.category_text_selected))
                textView.textSize = 15f
            } else {
                itemView.setBackgroundColor(itemView.context.getColor(R.color.category_parent_bg))
                textView.setTextColor(itemView.context.getColor(R.color.category_text_normal))
                textView.textSize = 14f
            }
        }
    }

    class ChildViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textView: TextView = view.findViewById(R.id.tv_category_name)
        
        fun bind(name: String, categoryId: Long?, isSelected: Boolean) {
            textView.text = name
            
            if (isSelected) {
                itemView.setBackgroundColor(itemView.context.getColor(R.color.category_child_selected_bg))
                textView.setTextColor(itemView.context.getColor(R.color.category_text_selected))
                textView.textSize = 13f
            } else {
                itemView.setBackgroundColor(itemView.context.getColor(R.color.category_child_bg))
                textView.setTextColor(itemView.context.getColor(R.color.category_text_normal))
                textView.textSize = 12f
            }
        }
    }
}

