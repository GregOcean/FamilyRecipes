package com.familyrecipes.android.ui

/**
 * 可搜索的Fragment接口
 */
interface SearchableFragment {
    /**
     * 执行搜索
     * @param keyword 搜索关键词
     */
    fun performSearch(keyword: String)
}

