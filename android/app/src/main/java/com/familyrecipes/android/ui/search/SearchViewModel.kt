package com.familyrecipes.android.ui.search
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyrecipes.android.data.model.GlobalSearchResult
import com.familyrecipes.android.data.local.*
import com.familyrecipes.android.data.remote.ApiClient
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SearchState(val keyword: String="",val result: GlobalSearchResult?=null,val loading:Boolean=false,val error:String?=null)
class SearchViewModel : ViewModel() {
    private val mutable=MutableStateFlow(SearchState())
    val state=mutable.asStateFlow()
    private var request: Job?=null
    private var generation=0
    private val account=LocalStore.account
    private data class Cached(val keyword:String,val priority:String,val result:GlobalSearchResult)
    fun search(keyword:String,priority:String,force:Boolean=false) {
        if(!force && mutable.value.keyword==keyword && (mutable.value.loading || mutable.value.result!=null))return
        val revision=++generation
        request?.cancel()
        mutable.value=SearchState(keyword,if(mutable.value.keyword==keyword) mutable.value.result else null,true)
        request=viewModelScope.launch {
            try {
                val dao=LocalStore.instance.records()
                val cached=dao.get(account,"search","latest")?.let { Gson().fromJson(it,Cached::class.java) }
                if(revision!=generation)return@launch
                if(cached?.keyword==keyword && cached.priority==priority) mutable.value=mutable.value.copy(result=cached.result)
                val response=ApiClient.getService().globalSearch(keyword=keyword,priorityType=priority)
                check(response.isSuccessful && response.body()?.code==200)
                val result=response.body()!!.data!!
                if(revision!=generation || account!=LocalStore.account)return@launch
                dao.put(LocalRecord(account,"search","latest",Gson().toJson(Cached(keyword,priority,result))))
                mutable.value=SearchState(keyword,result)
            } catch(e:CancellationException) { throw e }
              catch(_:Exception) { if(revision==generation) mutable.value=mutable.value.copy(loading=false,error="搜索失败，已保留结果，下拉重试") }
        }
    }
}
