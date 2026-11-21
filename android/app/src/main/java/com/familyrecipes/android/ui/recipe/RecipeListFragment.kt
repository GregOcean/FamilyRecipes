package com.familyrecipes.android.ui.recipe

import com.familyrecipes.android.ui.design.UiStrings
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.familyrecipes.android.data.local.*
import com.familyrecipes.android.data.model.*
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.ui.SearchableFragment
import com.familyrecipes.android.ui.design.Ui
import com.familyrecipes.android.ui.design.Ui.dp
import com.google.gson.Gson
import kotlinx.coroutines.*

class RecipeListFragment : Fragment(), SearchableFragment {
    private lateinit var list: LinearLayout
    private lateinit var status: TextView
    private lateinit var search: EditText
    internal var selectionDialog: androidx.appcompat.app.AlertDialog? = null
        private set
    private var sources = ""
    private var times = ""
    private var stocks = ""
    private lateinit var refresh: androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    private lateinit var scroll: ScrollView
    private lateinit var more: TextView
    private var loadedParams: BrowserParams? = null
    private lateinit var confirm: Button
    private var scope = "discovery"
    private var origin = "all"
    private var taste = ""
    private var cuisine = ""
    private var minutes: Int? = null
    private var missing: Int? = null
    private var known = false
    private var keyword = ""
    private var seed = "0"
    private var page = 1
    private var total = 0L
    private var job: Job? = null
    private var generation = 0
    private var scrollPosition = 0
    private var loadedContext: String? = null
    private val contextKey get() = "${LocalStore.account}:${PreferenceManager.currentKitchenId}:${ApiClient.contentLanguage()}"
    private val detail = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
        if(result.resultCode == Activity.RESULT_OK) {
            val updated=runCatching {Gson().fromJson(result.data?.getStringExtra("updated_recipe"),Recipe::class.java)}.getOrNull()
            if(updated!=null) {
                val index=rows.indexOfFirst{it.id==updated.id}
                if(index>=0){
                    rows[index]=updated
                    if(sources.isNotEmpty())load()
                    if(::list.isInitialized)draw()
                }
            }
        }
    }

    private data class BrowserParams(
        val scope: String,
        val origin: String,
        val keyword: String,
        val taste: String,
        val minutes: Int?,
        val missing: Int?,
        val known: Boolean,
        val seed: String,
        val cuisine: String? = null,
        val sources: String? = null,
        val times: String? = null,
        val stocks: String? = null
    )

    private data class BrowserCache(
        val params: BrowserParams,
        val rows: List<Recipe>,
        val total: Long,
        val page: Int
    )

    private fun params() =
        BrowserParams(scope, origin, keyword, taste, minutes, missing, known, seed, cuisine, sources, times, stocks)

    private val cacheKey
        get() = "${PreferenceManager.currentKitchenId}:${if(picking)"picker" else "main"}:${ApiClient.contentLanguage()}"

    private var cacheReady = false
    private val rows = mutableListOf<Recipe>()
    private val picked = linkedMapOf<Long, MealDish>()
    private val picking
        get() = arguments?.getBoolean("picker") == true

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        scope = state?.getString("scope") ?: arguments?.getString("scope") ?: "discovery"
        origin = state?.getString("origin") ?: arguments?.getString("origin") ?: "all"
        known = state?.getBoolean("known") ?: arguments?.getBoolean("known") ?: false
        keyword = state?.getString("query").orEmpty()
        taste = state?.getString("taste").orEmpty()
        cuisine = state?.getString("cuisine").orEmpty()
        minutes = state?.getInt("minutes")?.takeIf { it > 0 || it == -60 }
        missing = state?.getInt("missing", -999)?.takeIf { it >= -1 }
        times=state?.getString("times") ?: minutes?.toString().orEmpty()
        stocks=state?.getString("stocks") ?: missing?.toString().orEmpty()
        seed = state?.getString("seed") ?: "0"
        scrollPosition=state?.getInt("scroll_y") ?: 0
        sources=state?.getString("sources") ?: RecipeSources.fromLegacy(scope,origin,known)
        if (state == null && arguments == null) {
            val raw =
                requireContext()
                    .getSharedPreferences("recipe-browser", 0)
                    .getString(LocalStore.account + cacheKey, null)
            runCatching { Gson().fromJson(raw, BrowserParams::class.java) }
                .getOrNull()
                ?.let { p ->
                    scope = p.scope
                    origin = p.origin
                    sources=p.sources ?: RecipeSources.fromLegacy(p.scope,p.origin,p.known)
                    keyword = p.keyword
                    taste = p.taste.orEmpty()
                    cuisine = p.cuisine.orEmpty()
                    times = p.times ?: p.minutes?.toString().orEmpty()
                    stocks = p.stocks ?: p.missing?.toString().orEmpty()
                    known = p.known
                    seed = p.seed
                }
        }
        scope="discovery";origin="all";known=false;minutes=null;missing=null
        state?.getString("picked")?.let {
            Gson().fromJson(it, Array<MealDish>::class.java).forEach { d ->
                d.recipeId?.let { id -> picked[id] = d }
            }
        }
    }

    override fun onSaveInstanceState(out: Bundle) {
        super.onSaveInstanceState(out)
        out.putString("times",times)
        out.putString("stocks",stocks)
        out.putString("sources",sources)
        out.putString("scope", scope)
        out.putString("origin", origin)
        out.putString("cuisine", cuisine)
        out.putString("query", keyword)
        out.putString("taste", taste)
        out.putInt("minutes", minutes ?: 0)
        out.putInt("missing", missing ?: -999)
        out.putBoolean("known", known)
        out.putString("seed", seed)
        out.putString("picked", Gson().toJson(picked.values))
        out.putInt("scroll_y",if(::scroll.isInitialized)scroll.scrollY else scrollPosition)
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        val ctx = requireContext()
        val root = Ui.box(ctx,8)
        search = Ui.field(ctx, UiStrings.s(com.familyrecipes.android.R.string.ui_5981cc43fb), keyword)
        search.filters=arrayOf(android.text.InputFilter.LengthFilter(100))
        if(activity !is com.familyrecipes.android.ui.MainActivity)root.addView(search)
        val filters = Ui.row(ctx).apply { gravity=Gravity.TOP }
        fun filter(title:Int, value:String, choose:(com.familyrecipes.android.ui.design.FilterButton)->Unit):com.familyrecipes.android.ui.design.FilterButton {
            val button=com.familyrecipes.android.ui.design.FilterButton(ctx).apply {
                setLabel(getString(title));setValue(value);setOnClickListener { choose(this) }
            }
            filters.addView(button,LinearLayout.LayoutParams(ctx.dp(92),-1).apply { marginStart=ctx.dp(2);marginEnd=ctx.dp(2) })
            return button
        }
        fun summary(selected:String,choices:List<Pair<String,String>>,empty:String=getString(com.familyrecipes.android.R.string.filter_any)):String {
            val labels=choices.filter{it.first in selected.split(",")}.map{it.second.trim()}
            return when(labels.size){0->empty;1->labels.first();else->getString(com.familyrecipes.android.R.string.source_selected_count,labels.size)}
        }
        fun chooseMany(button:com.familyrecipes.android.ui.design.FilterButton,title:Int,choices:List<Pair<String,String>>,current:String,empty:String=getString(com.familyrecipes.android.R.string.filter_any),apply:(String)->Unit) {
            val selected=current.split(",").filter{it.isNotBlank()}.toMutableSet()
            fun save(value:String) {
                apply(value);button.setValue(summary(value,choices,empty))
                button.contentDescription=getString(title)+": "+choices.filter{it.first in value.split(",")}.joinToString(", "){it.second.trim()}.ifBlank{empty}
                load()
            }
            selectionDialog?.dismiss()
            selectionDialog=androidx.appcompat.app.AlertDialog.Builder(ctx).setTitle(title)
                .setMultiChoiceItems(choices.map{it.second}.toTypedArray(),choices.map{it.first in selected}.toBooleanArray()){_,index,checked->if(checked)selected.add(choices[index].first) else selected.remove(choices[index].first)}
                .setNeutralButton(empty){_,_->save("")}.setNegativeButton(android.R.string.cancel,null)
                .setPositiveButton(android.R.string.ok){_,_->save(choices.filter{it.first in selected}.joinToString(","){it.first})}.show()
        }
        val sourceChoices=RecipeSources.keys.zip(listOf(getString(com.familyrecipes.android.R.string.source_created),getString(com.familyrecipes.android.R.string.source_known),getString(com.familyrecipes.android.R.string.source_favorites),getString(com.familyrecipes.android.R.string.source_shared)))
        val all=getString(com.familyrecipes.android.R.string.source_all)
        filter(com.familyrecipes.android.R.string.filter_source,summary(sources,sourceChoices,all)) { button ->
            if(com.familyrecipes.android.util.AuthUtil.requireLogin(ctx))chooseMany(button,com.familyrecipes.android.R.string.filter_source,sourceChoices,sources,all){sources=it}
        }
        var cuisineTree:List<com.familyrecipes.android.data.model.CuisineOption>?=null
        fun cuisineChoices(nodes:List<com.familyrecipes.android.data.model.CuisineOption>,depth:Int=0):List<Pair<String,String>> =
            nodes.flatMap{listOf(it.value to ("　".repeat(depth)+it.name))+cuisineChoices(it.children,depth+1)}
        val cuisineButton=filter(com.familyrecipes.android.R.string.filter_cuisine,if(cuisine.isBlank())getString(com.familyrecipes.android.R.string.filter_any) else if(cuisine.contains(","))getString(com.familyrecipes.android.R.string.source_selected_count,cuisine.split(",").size) else UiStrings.domain(cuisine)) { button ->
            viewLifecycleOwner.lifecycleScope.launch {
                button.isEnabled=false
                try {
                    val tree=cuisineTree ?: Ui.data(ApiClient.getService().getCuisineTree()).also{cuisineTree=it}
                    chooseMany(button,com.familyrecipes.android.R.string.filter_cuisine,cuisineChoices(tree),cuisine){cuisine=it}
                }catch(e:CancellationException){throw e}catch(e:Exception){Ui.toast(ctx,e)}finally{button.isEnabled=true}
            }
        }
        if(cuisine.isNotBlank())root.post {
            if(view!==root)return@post
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val tree=Ui.data(ApiClient.getService().getCuisineTree());cuisineTree=tree
                    cuisineButton.setValue(summary(cuisine,cuisineChoices(tree)))
                }catch(e:CancellationException){throw e}catch(_:Exception){}
            }
        }
        val tasteChoices=listOf("酸","甜","苦","辣","鲜香","清淡","麻","咸","臭").map{it to UiStrings.domain(it)}
        filter(com.familyrecipes.android.R.string.filter_taste,summary(taste,tasteChoices)) { button ->
            chooseMany(button,com.familyrecipes.android.R.string.filter_taste,tasteChoices,taste){taste=it}
        }
        val timeChoices=listOf("15" to getString(com.familyrecipes.android.R.string.ui_a996573fd0),"30" to getString(com.familyrecipes.android.R.string.ui_1826fa4a7b),"60" to getString(com.familyrecipes.android.R.string.time_under_hour),"-60" to getString(com.familyrecipes.android.R.string.time_over_hour))
        filter(com.familyrecipes.android.R.string.filter_time,summary(times,timeChoices)) { button ->
            chooseMany(button,com.familyrecipes.android.R.string.filter_time,timeChoices,times){times=it}
        }
        val stockChoices=listOf("0" to getString(com.familyrecipes.android.R.string.ui_f92d315989),"-1" to getString(com.familyrecipes.android.R.string.stock_any_short))
        filter(com.familyrecipes.android.R.string.filter_stock,summary(stocks,stockChoices)) { button ->
            if(com.familyrecipes.android.util.AuthUtil.requireLogin(ctx))chooseMany(button,com.familyrecipes.android.R.string.filter_stock,stockChoices,stocks){stocks=it}
        }
        root.addView(HorizontalScrollView(ctx).apply {
            isHorizontalScrollBarEnabled=false
            clipToPadding=false
            addView(filters)
        },LinearLayout.LayoutParams(-1,-2))
        status = Ui.text(ctx, UiStrings.s(com.familyrecipes.android.R.string.ui_37dc7bfce2), 13f).apply { setPadding(0,ctx.dp(2),0,ctx.dp(2)) }
        status.visibility=View.GONE
        root.addView(status)
        list = Ui.box(ctx, 0)
        scroll=com.familyrecipes.android.ui.design.LoadMoreScrollView(ctx).apply {
            addView(list)
            onLoadMore={
                val ready=job?.isActive!=true && rows.isNotEmpty() && rows.size<total && loadedContext==contextKey && loadedParams==params()
                if(ready)load(true)
                ready
            }
        }
        refresh=androidx.swiperefreshlayout.widget.SwipeRefreshLayout(ctx).apply {
            setColorSchemeColors(Ui.blue)
            addView(scroll)
            setOnChildScrollUpCallback { _,_->scroll.canScrollVertically(-1) }
            setOnRefreshListener {seed=java.util.UUID.randomUUID().toString();scrollPosition=0;scroll.scrollTo(0,0);load()}
        }
        root.addView(refresh, LinearLayout.LayoutParams(-1, 0, 1f))
        more = Ui.text(ctx,getString(com.familyrecipes.android.R.string.feed_swipe_more),13f).apply {gravity=Gravity.CENTER;setTextColor(Ui.muted)}

        confirm =
            Ui.button(ctx, UiStrings.f(com.familyrecipes.android.R.string.selected_count, picked.size), true) {
                requireActivity()
                    .setResult(
                        Activity.RESULT_OK,
                        Intent().putExtra("dishes", Gson().toJson(picked.values)))
                requireActivity().finish()
            }
        confirm.visibility = if (picking) View.VISIBLE else View.GONE
        root.addView(confirm)
        search.doAfterTextChanged {
            keyword = it.toString()
            (activity as? com.familyrecipes.android.ui.MainActivity)?.setRecipeSearchQuery(keyword)
            load()
        }
        root.post {(activity as? com.familyrecipes.android.ui.MainActivity)?.setRecipeSearchQuery(keyword)}
        if(rows.isNotEmpty())draw()
        return root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        val revision = generation
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val raw =
                    LocalStore.instance
                        .records()
                        .get(LocalStore.account, "recipe_browser", cacheKey)
                val cache =
                    runCatching { Gson().fromJson(raw, BrowserCache::class.java) }.getOrNull()
                if (revision == generation && cache?.params == params()) {
                    rows.clear()
                    rows.addAll(cache.rows)
                    total = cache.total
                    page = cache.page
                    loadedContext=contextKey
                    loadedParams=cache.params
                    draw()
                    status.text = UiStrings.s(com.familyrecipes.android.R.string.ui_2df6c46856)
                }
            } finally {
                cacheReady = true
                if(rows.isEmpty())load()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::list.isInitialized && cacheReady && loadedContext!=contextKey) load()
    }

    private fun load(append: Boolean = false) {
        if (!::list.isInitialized) return
        if(append && (job?.isActive==true || loadedParams!=params() || loadedContext!=contextKey || rows.size>=total))return
        job?.cancel()
        val revision = ++generation
        val kitchen = PreferenceManager.currentKitchenId
        val account = LocalStore.account
        val requestContext = contextKey
        val requestParams=params()
        status.setOnClickListener(null)
        status.visibility=View.GONE
        refresh.isRefreshing=!append
        if(append)more.text=getString(com.familyrecipes.android.R.string.ui_fb4ca1cf1b)
        job =
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    delay(180)
                    val next = if (append) page + 1 else 1
                    val result =
                        Ui.data(
                            ApiClient.getService()
                                .browseRecipes(
                                    kitchen,
                                    scope,
                                    if (scope == "mine") origin else "all",
                                    keyword,
                                    "",
                                    null,
                                    null,
                                    scope == "mine" && known,
                                    seed,
                                    next, "", sources, taste, cuisine, times, stocks))
                    if (revision != generation || requestContext!=contextKey)
                        return@launch
                    if (!append) rows.clear()
                    rows.addAll(result.list)
                    page = next
                    total = if(result.list.isEmpty())rows.size.toLong() else result.total
                    loadedContext=requestContext
                    loadedParams=requestParams
                    if(!append) {scrollPosition=0;scroll.scrollTo(0,0)}
                    draw()
                    val snapshot = BrowserCache(requestParams, rows.toList(), total, page)
                    LocalStore.instance
                        .records()
                        .put(
                            LocalRecord(
                                account,
                                "recipe_browser",
                                cacheKey,
                                Gson().toJson(snapshot)))
                    if (!picking && arguments == null)
                        requireContext()
                            .getSharedPreferences("recipe-browser", 0)
                            .edit()
                            .putString(LocalStore.account + cacheKey, Gson().toJson(params()))
                            .apply()
                    if (revision != generation) return@launch
                    status.visibility=View.GONE
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if(revision!=generation)return@launch
                    if(append) {more.text=getString(com.familyrecipes.android.R.string.feed_retry_swipe);return@launch}
                    status.visibility=View.VISIBLE
                    status.text = Ui.errorMessage(requireContext(),e) + if (rows.isNotEmpty()) UiStrings.s(com.familyrecipes.android.R.string.ui_57bcca8c05) else ""
                    status.setOnClickListener { load() }
                } finally {
                    if(revision==generation)refresh.isRefreshing=false
                }
            }
    }

    private fun draw() {
        val c = requireContext()
        val previousScroll=if(list.childCount>0)scroll.scrollY else scrollPosition
        list.removeAllViews()
        if (rows.isEmpty()) list.addView(Ui.text(c, UiStrings.s(com.familyrecipes.android.R.string.ui_705ea281fb)))
        rows.chunked(2).forEach { pair ->
            val line = Ui.row(c)
            pair.forEach { r ->
                val card = Ui.card(c).apply { setPadding(0,0,0,0); clipToOutline=true }
                val image =
                    ImageView(c).apply {
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        setImageResource(com.familyrecipes.android.R.drawable.placeholder_recipe)
                    }
                if (r.coverImage.isNullOrBlank()) {
                    val art =
                        Ui.text(
                                c,
                                when {
                                    r.name.contains("番茄") -> "🍅"
                                    r.name.contains("虾") -> "🦐"
                                    r.name.contains("鸡") -> "🍗"
                                    r.name.contains("豆腐") -> "🥘"
                                    r.name.contains("西兰花") -> "🥦"
                                    r.name.contains("面") -> "🍜"
                                    r.name.contains("土豆") -> "🥔"
                                    else -> "🍲"
                                },
                                48f)
                            .apply {
                                gravity = android.view.Gravity.CENTER
                                setBackgroundColor(listOf(Ui.pale,Ui.warm,Ui.green,Ui.warm)[(r.id?:0L).rem(4).toInt()])
                            }
                    card.addView(
                        art,
                        LinearLayout.LayoutParams(
                            -1, (100 * resources.displayMetrics.density).toInt()))
                } else {
                    card.addView(
                        image,
                        LinearLayout.LayoutParams(
                            -1, (100 * resources.displayMetrics.density).toInt()))
                    com.bumptech.glide.Glide.with(this)
                        .load(com.familyrecipes.android.util.MediaImages.model(r.coverImage))
                        .into(image)
                }
                card.addView(Ui.text(c, r.name, 17f, true).apply { setPadding(c.dp(10),c.dp(8),c.dp(10),c.dp(2));maxLines=2;ellipsize=android.text.TextUtils.TruncateAt.END })
                card.addView(
                    Ui.text(
                        c,
                        listOfNotNull(
                                r.cookingTime?.let { UiStrings.f(com.familyrecipes.android.R.string.minutes_value, it) },
                                r.tags
                                    ?.filter { it.tagType == "taste" }
                                    ?.distinctBy{it.displayValue ?: it.tagValue}?.take(2)?.joinToString(" · ") { UiStrings.domain(it.displayValue ?: it.tagValue) }
                                    ?.takeIf { it.isNotBlank() },
                                if (r.isFavorite == true) UiStrings.s(com.familyrecipes.android.R.string.ui_934ebd87e6) else null)
                            .joinToString(" · "),
                        14f).apply { setPadding(c.dp(10),c.dp(2),c.dp(10),c.dp(8));setTextColor(Ui.muted);maxLines=2;ellipsize=android.text.TextUtils.TruncateAt.END })
                card.setOnClickListener {
                    detail.launch(
                        Intent(c, RecipeDetailActivity::class.java).putExtra("recipe_id", r.id))
                }
                if (picking)
                    card.addView(
                        CheckBox(c).apply {
                            text = UiStrings.s(com.familyrecipes.android.R.string.ui_153fa67a7f)
                            isChecked = picked.containsKey(r.id)
                            setOnCheckedChangeListener { _, v ->
                                r.id?.let {
                                    if (v) picked[it] = MealDish(it, r.name) else picked.remove(it)
                                }
                                confirm.text = UiStrings.f(com.familyrecipes.android.R.string.selected_count, picked.size)
                            }
                        })
                line.addView(
                    card, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(c.dp(4),c.dp(4),c.dp(4),c.dp(8)) })
            }
            if (pair.size == 1) line.addView(Space(c), LinearLayout.LayoutParams(0, 1, 1f))
            list.addView(line)
        }
        if(more.parent==null) list.addView(more)
        more.text=getString(com.familyrecipes.android.R.string.feed_swipe_more)
        more.visibility = if (rows.size < total) View.VISIBLE else View.GONE
        scroll.post {if(isAdded){scroll.scrollTo(0,previousScroll);scrollPosition=previousScroll}}
    }

    fun currentQuery():String=keyword

    override fun performSearch(keyword: String) {
        search.setText(keyword)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && ::list.isInitialized && cacheReady && loadedContext!=contextKey) load()
    }

    override fun onDestroyView() {
        selectionDialog?.dismiss();selectionDialog=null
        if(::scroll.isInitialized)scrollPosition=scroll.scrollY
        job?.cancel()
        super.onDestroyView()
    }
}
