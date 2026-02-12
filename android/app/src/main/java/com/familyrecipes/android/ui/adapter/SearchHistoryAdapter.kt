package com.familyrecipes.android.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.familyrecipes.android.databinding.ItemSearchHistoryBinding

/**
 * 搜索历史适配器
 */
class SearchHistoryAdapter(
    private val history: List<String>,
    private val onItemClick: (String) -> Unit,
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<SearchHistoryAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemSearchHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(keyword: String) {
            binding.tvKeyword.text = keyword
            
            binding.root.setOnClickListener {
                onItemClick(keyword)
            }
            
            binding.ivDelete.setOnClickListener {
                onDeleteClick(keyword)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSearchHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(history[position])
    }

    override fun getItemCount() = history.size
}

