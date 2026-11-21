package com.familyrecipes.android.ui.search
import com.familyrecipes.android.ui.design.UiStrings
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
    private var currentScope=""
    private data class Cached(val keyword:String,val priority:String,val result:GlobalSearchResult)
    fun search(keyword:String,priority:String,force:Boolean=false) {
        val account=LocalStore.account
        val language=ApiClient.contentLanguage()
        val scope="$account:$language:$priority"
        if(currentScope!=scope){currentScope=scope;mutable.value=SearchState()}
        if(!force && mutable.value.keyword==keyword && (mutable.value.loading || mutable.value.result!=null))return
        val revision=++generation
        request?.cancel()
        mutable.value=SearchState(keyword,if(mutable.value.keyword==keyword) mutable.value.result else null,true)
        request=viewModelScope.launch {
            try {
                val dao=LocalStore.instance.records()
                val cached=dao.get(account,"search","latest:$language")?.let { runCatching{Gson().fromJson(it,Cached::class.java)}.getOrNull() }
                if(revision!=generation)return@launch
                if(cached?.keyword==keyword && cached.priority==priority) mutable.value=mutable.value.copy(result=cached.result)
                val response=ApiClient.getService().globalSearch(keyword=keyword,priorityType=priority)
                val result=com.familyrecipes.android.ui.design.Ui.data(response)
                if(revision!=generation || account!=LocalStore.account || language!=ApiClient.contentLanguage())return@launch
                dao.put(LocalRecord(account,"search","latest:$language",Gson().toJson(Cached(keyword,priority,result))))
                mutable.value=SearchState(keyword,result)
            } catch(e:CancellationException) { throw e }
              catch(_:Exception) { if(revision==generation) mutable.value=mutable.value.copy(loading=false,error=UiStrings.s(com.familyrecipes.android.R.string.ui_487fcd5d2e)) }
        }
    }
}
