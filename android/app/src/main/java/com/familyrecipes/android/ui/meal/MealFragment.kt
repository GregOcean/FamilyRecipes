package com.familyrecipes.android.ui.meal

import com.familyrecipes.android.ui.design.UiStrings
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.familyrecipes.android.data.local.*
import com.familyrecipes.android.data.model.*
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.ui.design.Ui
import com.familyrecipes.android.ui.design.Ui.dp
import com.google.gson.Gson
import kotlinx.coroutines.*

class MealFragment : Fragment() {
    private lateinit var list: LinearLayout
    private lateinit var drafts: LinearLayout
    private lateinit var heading: TextView
    private lateinit var addButton: com.google.android.material.button.MaterialButton
    private lateinit var composer: LinearLayout
    private lateinit var quickInput: EditText
    private lateinit var quickSend: com.google.android.material.button.MaterialButton
    private var quickBusy = false
    private var tab = "order"
    private var job: Job? = null

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        tab = if ((s?.getString("tab") ?: tab)=="prep") "prep" else "order"
        val swipe = com.familyrecipes.android.ui.design.SwipeTabLayout(requireContext())
        val root = Ui.box(requireContext(),12)
        swipe.addView(root, FrameLayout.LayoutParams(-1, -1))
        swipe.onSwipeLeft = { switchBySwipe("prep") }
        swipe.onSwipeRight = { switchBySwipe("order") }
        composer = buildOrderComposer()
        root.addView(composer)
        val section=Ui.row(requireContext())
        heading=Ui.text(requireContext(),UiStrings.s(com.familyrecipes.android.R.string.ui_e92d29c51a),17f,true)
        section.addView(heading,LinearLayout.LayoutParams(0,-2,1f))
        addButton=Ui.button(requireContext(),UiStrings.s(com.familyrecipes.android.R.string.ui_f9616f9a11)) {
            startActivity(Intent(requireContext(),MealEditorActivity::class.java).putExtra("kind",if(tab=="prep")"prep" else "order"))
        }
        section.addView(addButton,LinearLayout.LayoutParams(-2,-2));root.addView(section)
        list = Ui.box(requireContext(), 0)
        drafts = Ui.box(requireContext(), 0)
        val content=Ui.box(requireContext(),0)
        content.addView(list);content.addView(drafts)
        root.addView(Ui.scroll(requireContext(), content), LinearLayout.LayoutParams(-1, 0, 1f))
        return swipe
    }

    private fun switchBySwipe(value: String) {
        if (tab == value) return
        val host = activity as? com.familyrecipes.android.ui.MainActivity
        if (host != null) host.switchMealTab(value) else showTab(value)
    }

    /**
     * 点菜默认只保留一个自由输入框，输入后即可转发到群。
     */
    private fun buildOrderComposer(): LinearLayout {
        val ctx = requireContext()
        return Ui.card(ctx).apply {
            addView(Ui.text(ctx, getString(com.familyrecipes.android.R.string.meal_quick_order_title), 20f, true))
            quickInput = Ui.field(ctx, getString(com.familyrecipes.android.R.string.meal_quick_order_hint), multi = true).apply {
                minLines = 2
                maxLines = 4
                setPadding(ctx.dp(12), ctx.dp(10), ctx.dp(12), ctx.dp(10))
            }
            addView(quickInput)
            quickSend = Ui.button(ctx, getString(com.familyrecipes.android.R.string.meal_quick_send), true) { sendQuickOrder() }
            addView(quickSend, LinearLayout.LayoutParams(-1, -2))
        }
    }

    private fun sendQuickOrder() {
        if (quickBusy) return
        val text = quickInput.text.toString().trim()
        if (text.isBlank()) {
            Toast.makeText(requireContext(), getString(com.familyrecipes.android.R.string.meal_quick_empty), Toast.LENGTH_SHORT).show()
            return
        }
        val kitchen = PreferenceManager.currentKitchenId
        if (kitchen == null) {
            Toast.makeText(requireContext(), getString(com.familyrecipes.android.R.string.ui_eb006cd6f1), Toast.LENGTH_SHORT).show()
            return
        }
        val dishes = parseQuickDishes(text)
        val title = text.lineSequence().firstOrNull()?.trim().orEmpty().take(80).ifBlank { text.take(80) }
        quickBusy = true
        quickSend.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val saved = Ui.data(
                    ApiClient.getService().saveMeal(
                        MealArrangement(kitchenId = kitchen, kind = "order", title = title, dishes = dishes)))
                val groups = Ui.data(ApiClient.getService().getGroupsList())
                when {
                    groups.isEmpty() -> Toast.makeText(
                        requireContext(), getString(com.familyrecipes.android.R.string.review_saved_no_group), Toast.LENGTH_LONG).show()
                    groups.size == 1 -> shareAndFinish(saved.id!!, groups.first().id!!)
                    else -> androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle(getString(com.familyrecipes.android.R.string.meal_group_pick))
                        .setItems(groups.map { it.name }.toTypedArray()) { _, index ->
                            viewLifecycleOwner.lifecycleScope.launch {
                                try { shareAndFinish(saved.id!!, groups[index].id!!) }
                                catch (e: CancellationException) { throw e }
                                catch (e: Exception) { Ui.toast(requireContext(), e) }
                            }
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                }
                if (groups.isNotEmpty()) load()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Ui.toast(requireContext(), e)
            } finally {
                quickBusy = false
                if (::quickSend.isInitialized) quickSend.isEnabled = true
            }
        }
    }

    private suspend fun shareAndFinish(mealId: Long, groupId: Long) {
        Ui.data(ApiClient.getService().shareMeal(mealId, ChatShareRequest(groupId)))
        if (!isAdded) return
        Toast.makeText(requireContext(), getString(com.familyrecipes.android.R.string.ui_54bba45c94), Toast.LENGTH_SHORT).show()
        quickInput.setText("")
    }

    /** 发给没装 App、没注册的家人：打开就是菜单和采购清单。 */
    private fun shareWithFamily(mealId: Long, title: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val share = Ui.data(ApiClient.getService().publicShareMeal(mealId))
                if (!isAdded) return@launch
                val url = publicWebUrl() + share.path
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    putExtra(
                        Intent.EXTRA_TEXT,
                        getString(com.familyrecipes.android.R.string.meal_share_family_message, title, url))
                }
                startActivity(Intent.createChooser(send, getString(com.familyrecipes.android.R.string.meal_share_family)))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Toast.makeText(requireContext(), Ui.errorMessage(requireContext(), e), Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** 生产环境 API 是 api.familyrecipes.live，分享页在 familyrecipes.live；本地调试保持原地址。 */
    private fun publicWebUrl(): String {
        var base = PreferenceManager.baseUrl.trimEnd('/')
        if (base.endsWith("/api")) base = base.dropLast(4)
        return base.replace("://api.", "://")
    }

    /** 把自由输入拆成菜品：支持换行、顿号、逗号和分号分隔。 */
    private fun parseQuickDishes(text: String): List<MealDish> {
        val names = text.lines()
            .flatMap { line -> line.split('、', '，', ',', '；', ';') }
            .map { it.trim().removePrefix("-").trim() }
            .filter { it.isNotBlank() }
            .map { it.take(100) }
            .distinct()
            .take(30)
        return (if (names.isEmpty()) listOf(text.take(80)) else names).map { MealDish(name = it) }
    }

    override fun onSaveInstanceState(b: Bundle) {
        super.onSaveInstanceState(b)
        b.putString("tab", tab)
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) load()
    }

    fun currentTab(): String = tab

    fun showTab(value:String) { tab=if(value=="prep")"prep" else "order";load() }

    private fun load() {
        if (!::list.isInitialized) return
        composer.visibility = if (tab == "order") View.VISIBLE else View.GONE
        heading.text=if(tab=="prep")getString(com.familyrecipes.android.R.string.meal_my_prep)
            else getString(com.familyrecipes.android.R.string.meal_my_order)
        addButton.text=if(tab=="prep")getString(com.familyrecipes.android.R.string.meal_prep_new)
            else getString(com.familyrecipes.android.R.string.meal_quick_more)
        job?.cancel()
        val kind=tab
        val account=LocalStore.account
        val kitchen=PreferenceManager.currentKitchenId
        drafts.removeAllViews()
        job =
            viewLifecycleOwner.lifecycleScope.launch {
                launch { loadDrafts(kind,account,kitchen) }
                try {
                    list.removeAllViews()
                    list.addView(Ui.text(requireContext(), UiStrings.s(com.familyrecipes.android.R.string.ui_fb4ca1cf1b)))
                    run {
                        val rows =
                            Ui.data(ApiClient.getService().meals(kitchen ?: error(UiStrings.s(com.familyrecipes.android.R.string.ui_eb006cd6f1)))).filter { it.kind == kind }
                        if(account!=LocalStore.account || kitchen!=PreferenceManager.currentKitchenId)return@launch
                        list.removeAllViews()
                        rows.forEach { m ->
                            val card = Ui.card(requireContext())
                            card.addView(Ui.text(requireContext(), m.title, 17f, true))
                            card.addView(
                                Ui.text(
                                    requireContext(),
                                    listOfNotNull(m.mealDate, m.meal?.takeIf { it.isNotBlank() }?.let{UiStrings.domain(it)})
                                        .joinToString(" · ")
                                        .ifBlank { UiStrings.s(com.familyrecipes.android.R.string.ui_7dc2d8a222) },
                                    14f))
                            card.addView(
                                Ui.text(requireContext(), m.dishes.joinToString("、") { it.name }))
                            if (kind == "prep") {
                                val items = m.dishes.flatMap { it.ingredients.orEmpty() }.filter { it.isNotBlank() }.distinct()
                                if (items.isNotEmpty())
                                    card.addView(Ui.text(requireContext(), getString(com.familyrecipes.android.R.string.meal_prep_ingredients, items.joinToString("、")), 14f).apply { setTextColor(Ui.muted) })
                            }
                            card.addView(
                                Ui.text(
                                    requireContext(),
                                    if (m.ownerId == PreferenceManager.userId)
                                        UiStrings.f(com.familyrecipes.android.R.string.my_shopping_count, m.shopping.size)
                                    else UiStrings.s(com.familyrecipes.android.R.string.ui_11d0c6e5bc),
                                    14f))
                            if (m.ownerId == PreferenceManager.userId) {
                                card.addView(
                                    Ui.button(requireContext(), getString(com.familyrecipes.android.R.string.meal_share_family), false) {
                                        shareWithFamily(m.id!!, m.title)
                                    },
                                    LinearLayout.LayoutParams(-1, -2).apply { topMargin = requireContext().dp(8) })
                            }
                            card.setOnClickListener {
                                startActivity(
                                    Intent(requireContext(), MealEditorActivity::class.java)
                                        .putExtra("meal_id", m.id))
                            }
                            list.addView(card)
                        }
                        if (rows.isEmpty())
                            list.addView(
                                Ui.text(
                                    requireContext(),
                                    UiStrings.s(if(tab=="prep")com.familyrecipes.android.R.string.empty_prep else com.familyrecipes.android.R.string.empty_order)))
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    list.removeAllViews()
                    list.addView(Ui.button(requireContext(), Ui.errorMessage(requireContext(),e)) { load() })
                }
            }
    }

    private suspend fun loadDrafts(kind:String,account:String,kitchen:Long?) {
        try {
            DraftWrites.flush()
            val records=LocalStore.instance.records().list(account,"draft").filter { d ->
                d.key.startsWith("meal:") && runCatching {
                    val meal=Gson().fromJson(d.json,MealArrangement::class.java)
                    MealDraftScope.matches(meal.kind,meal.kitchenId,kind,kitchen)
                }.getOrDefault(false)
            }
            if(account!=LocalStore.account || kitchen!=PreferenceManager.currentKitchenId)return
            if(records.isNotEmpty())drafts.addView(Ui.text(requireContext(),getString(com.familyrecipes.android.R.string.ui_22b4334f91)+" · "+records.size,15f,true))
            records.forEach { d ->
                val title=runCatching {Gson().fromJson(d.json,com.google.gson.JsonObject::class.java).get("title")?.asString}.getOrNull().orEmpty()
                val card=Ui.row(requireContext())
                val entry=Ui.box(requireContext(),4).apply {minimumHeight=requireContext().dp(48);isFocusable=true}
                entry.addView(Ui.text(requireContext(),title.ifBlank{getString(com.familyrecipes.android.R.string.ui_91b34afa6c)},16f,true))
                entry.addView(Ui.text(requireContext(),getString(com.familyrecipes.android.R.string.meal_continue_draft)+" ›",13f).apply{setTextColor(Ui.blue);setPadding(0,0,0,0)})
                entry.setOnClickListener {startActivity(Intent(requireContext(),MealEditorActivity::class.java).putExtra("draft_key",d.key))}
                card.addView(entry,LinearLayout.LayoutParams(0,-2,1f))
                card.addView(Ui.button(requireContext(),getString(com.familyrecipes.android.R.string.draft_delete)) {
                    androidx.appcompat.app.AlertDialog.Builder(requireContext()).setMessage(com.familyrecipes.android.R.string.draft_delete_confirm)
                        .setNegativeButton(android.R.string.cancel,null).setPositiveButton(android.R.string.ok){_,_->
                            viewLifecycleOwner.lifecycleScope.launch {DraftWrites.clear(account,d.key);if(account==LocalStore.account)load()}
                        }.show()
                })
                drafts.addView(card)
            }
        }catch(e:CancellationException){throw e}catch(e:Exception){drafts.addView(Ui.button(requireContext(),Ui.errorMessage(requireContext(),e)){load()})}
    }

    override fun onDestroyView() {
        job?.cancel()
        super.onDestroyView()
    }
}
