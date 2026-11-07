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
    private lateinit var discoveryButton: com.google.android.material.button.MaterialButton
    private lateinit var mineButton: com.google.android.material.button.MaterialButton
    private lateinit var mineFilters: View
    private lateinit var shuffle: View
    private lateinit var more: View
    private lateinit var confirm: Button
    private var scope = "discovery"
    private var origin = "all"
    private var taste = ""
    private var minutes: Int? = null
    private var missing: Int? = null
    private var known = false
    private var keyword = ""
    private var seed = "0"
    private var page = 1
    private var total = 0L
    private var job: Job? = null
    private var generation = 0

    private data class BrowserParams(
        val scope: String,
        val origin: String,
        val keyword: String,
        val taste: String,
        val minutes: Int?,
        val missing: Int?,
        val known: Boolean,
        val seed: String
    )

    private data class BrowserCache(
        val params: BrowserParams,
        val rows: List<Recipe>,
        val total: Long,
        val page: Int
    )

    private fun params() =
        BrowserParams(scope, origin, keyword, taste, minutes, missing, known, seed)

    private val cacheKey
        get() = "${PreferenceManager.currentKitchenId}:${if(picking)"picker" else "main"}"

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
        minutes = state?.getInt("minutes")?.takeIf { it > 0 }
        missing = state?.getInt("missing", -1)?.takeIf { it >= 0 }
        seed = state?.getString("seed") ?: "0"
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
                    keyword = p.keyword
                    taste = ""
                    minutes = null
                    missing = null
                    known = p.known
                    seed = p.seed
                }
        }
        state?.getString("picked")?.let {
            Gson().fromJson(it, Array<MealDish>::class.java).forEach { d ->
                d.recipeId?.let { id -> picked[id] = d }
            }
        }
    }

    override fun onSaveInstanceState(out: Bundle) {
        super.onSaveInstanceState(out)
        out.putString("scope", scope)
        out.putString("origin", origin)
        out.putString("query", keyword)
        out.putString("taste", taste)
        out.putInt("minutes", minutes ?: 0)
        out.putInt("missing", missing ?: -1)
        out.putBoolean("known", known)
        out.putString("seed", seed)
        out.putString("picked", Gson().toJson(picked.values))
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        val ctx = requireContext()
        val root = Ui.box(ctx)
        val tabs = Ui.row(ctx)
        discoveryButton =
            Ui.button(ctx, UiStrings.s(com.familyrecipes.android.R.string.ui_fc4d75f6cb)) {
                scope = "discovery"
                load()
            }
        mineButton =
            Ui.button(ctx, UiStrings.s(com.familyrecipes.android.R.string.ui_2f8ba39ad4)) {
                scope = "mine"
                load()
            }
        tabs.addView(discoveryButton, LinearLayout.LayoutParams(0, -2, 1f))
        tabs.addView(mineButton, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(tabs)
        search = Ui.field(ctx, UiStrings.s(com.familyrecipes.android.R.string.ui_5981cc43fb), keyword)
        root.addView(search)
        val filters = Ui.row(ctx)
        filters.addView(
            Ui.select(
                ctx,
                listOf(UiStrings.s(com.familyrecipes.android.R.string.ui_568d968eee), "酸", "辣", "酸辣", "清淡", "鲜香").map{UiStrings.domain(it)},
                listOf("", "酸", "辣", "酸辣", "清淡", "鲜香").indexOf(taste).coerceAtLeast(0)) { n ->
                    val v = listOf("", "酸", "辣", "酸辣", "清淡", "鲜香")[n]
                    if (v != taste) {
                        taste = v
                        load()
                    }
                },
            LinearLayout.LayoutParams(0, -2, 1f))
        filters.addView(
            Ui.select(
                ctx,
                listOf(UiStrings.s(com.familyrecipes.android.R.string.ui_1c6a1a8b39), UiStrings.s(com.familyrecipes.android.R.string.ui_a996573fd0), UiStrings.s(com.familyrecipes.android.R.string.ui_1826fa4a7b)),
                when (minutes) {
                    15 -> 1
                    30 -> 2
                    else -> 0
                }) { n ->
                    val v = listOf(null, 15, 30)[n]
                    if (v != minutes) {
                        minutes = v
                        load()
                    }
                },
            LinearLayout.LayoutParams(0, -2, 1f))
        filters.addView(
            Ui.select(
                ctx,
                listOf(UiStrings.s(com.familyrecipes.android.R.string.ui_2722a62702), UiStrings.s(com.familyrecipes.android.R.string.ui_f92d315989), UiStrings.s(com.familyrecipes.android.R.string.ui_0402920ca9)),
                when (missing) {
                    0 -> 1
                    2 -> 2
                    else -> 0
                }) { n ->
                    val v = listOf(null, 0, 2)[n]
                    if (v != missing) {
                        missing = v
                        load()
                    }
                },
            LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(filters)
        val mine = Ui.row(ctx)
        mineFilters = mine
        mine.tag = "mine"
        mine.addView(
            Ui.select(
                ctx,
                listOf(UiStrings.s(com.familyrecipes.android.R.string.ui_99b7a390f3), UiStrings.s(com.familyrecipes.android.R.string.ui_1b7ffa2f43), UiStrings.s(com.familyrecipes.android.R.string.ui_47b582adb2), UiStrings.s(com.familyrecipes.android.R.string.ui_752589b25d)),
                listOf("all", "created", "favorites", "shared").indexOf(origin).coerceAtLeast(0)) {
                    n ->
                    val v = listOf("all", "created", "favorites", "shared")[n]
                    if (v != origin) {
                        origin = v
                        load()
                    }
                },
            LinearLayout.LayoutParams(0, -2, 1f))
        mine.addView(
            CheckBox(ctx).apply {
                text = UiStrings.s(com.familyrecipes.android.R.string.ui_d3617a614a)
                isChecked = known
                setOnCheckedChangeListener { _, v ->
                    known = v
                    load()
                }
            })
        root.addView(mine)
        status = Ui.text(ctx, UiStrings.s(com.familyrecipes.android.R.string.ui_37dc7bfce2), 16f)
        shuffle =
            Ui.button(ctx, UiStrings.s(com.familyrecipes.android.R.string.ui_0e0b0ddfeb)) {
                seed = java.util.UUID.randomUUID().toString()
                load()
            }
        val heading=Ui.row(ctx)
        heading.addView(status,LinearLayout.LayoutParams(0,-2,1f))
        heading.addView(shuffle)
        root.addView(heading)
        list = Ui.box(ctx, 0)
        root.addView(Ui.scroll(ctx, list), LinearLayout.LayoutParams(-1, 0, 1f))
        more = Ui.button(ctx, UiStrings.s(com.familyrecipes.android.R.string.ui_7728154995)) { load(true) }
        root.addView(more)
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
            load()
        }
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
                    draw()
                    status.text = UiStrings.s(com.familyrecipes.android.R.string.ui_2df6c46856)
                }
            } finally {
                cacheReady = true
                load()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::list.isInitialized && cacheReady) load()
    }

    private fun load(append: Boolean = false) {
        if (!::list.isInitialized) return
        job?.cancel()
        val revision = ++generation
        val kitchen = PreferenceManager.currentKitchenId
        shuffle.visibility =
            if (scope == "discovery" && keyword.isBlank()) View.VISIBLE else View.GONE
        mineFilters.visibility = if (scope == "mine") View.VISIBLE else View.GONE
        listOf(discoveryButton to (scope == "discovery"), mineButton to (scope == "mine"))
            .forEach { (b, active) ->
                b.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(if (active) Ui.blue else Ui.pale)
                b.setTextColor(if (active) Ui.onBlue else Ui.blue)
            }
        status.text = (if (scope == "mine") UiStrings.s(com.familyrecipes.android.R.string.ui_2f8ba39ad4) else UiStrings.s(com.familyrecipes.android.R.string.ui_fc4d75f6cb)) + UiStrings.s(com.familyrecipes.android.R.string.ui_fa2393258d)
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
                                    taste,
                                    minutes,
                                    missing,
                                    scope == "mine" && known,
                                    seed,
                                    next))
                    if (revision != generation || kitchen != PreferenceManager.currentKitchenId)
                        return@launch
                    if (!append) rows.clear()
                    rows.addAll(result.list)
                    page = next
                    total = result.total
                    draw()
                    val snapshot = BrowserCache(params(), rows.toList(), total, page)
                    LocalStore.instance
                        .records()
                        .put(
                            LocalRecord(
                                LocalStore.account,
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
                    status.text =
                        UiStrings.f(com.familyrecipes.android.R.string.recipe_results_count, UiStrings.s(if(keyword.isNotBlank()) com.familyrecipes.android.R.string.search_results_title else if(scope=="mine") com.familyrecipes.android.R.string.ui_2f8ba39ad4 else com.familyrecipes.android.R.string.discover_title), total)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    status.text = UiStrings.f(com.familyrecipes.android.R.string.retry_error_value, e.message) + if (rows.isNotEmpty()) UiStrings.s(com.familyrecipes.android.R.string.ui_57bcca8c05) else ""
                    status.setOnClickListener { load() }
                }
            }
    }

    private fun draw() {
        val c = requireContext()
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
                card.addView(Ui.text(c, r.name, 18f, true).apply { setPadding(c.dp(12),c.dp(10),c.dp(12),c.dp(4)) })
                card.addView(
                    Ui.text(
                        c,
                        listOfNotNull(
                                r.cookingTime?.let { UiStrings.f(com.familyrecipes.android.R.string.minutes_value, it) },
                                r.tags
                                    ?.filter { it.tagType == "taste" }
                                    ?.joinToString("、") { it.tagValue }
                                    ?.takeIf { it.isNotBlank() },
                                if (r.isFavorite == true) UiStrings.s(com.familyrecipes.android.R.string.ui_934ebd87e6) else null)
                            .joinToString(" · "),
                        14f).apply { setPadding(c.dp(12),c.dp(4),c.dp(12),c.dp(12));setTextColor(Ui.muted) })
                card.setOnClickListener {
                    startActivity(
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
        more.visibility = if (rows.size < total) View.VISIBLE else View.GONE
    }

    override fun performSearch(keyword: String) {
        search.setText(keyword)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && ::list.isInitialized) load()
    }

    override fun onDestroyView() {
        job?.cancel()
        super.onDestroyView()
    }
}
