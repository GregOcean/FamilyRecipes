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
import com.familyrecipes.android.ui.recipe.EditRecipeActivity
import com.google.gson.Gson
import kotlinx.coroutines.*

class MealFragment : Fragment() {
    private lateinit var list: LinearLayout
    private lateinit var drafts: LinearLayout
    private lateinit var heading: TextView
    private lateinit var addButton: com.google.android.material.button.MaterialButton
    private var tab = "order"
    private val tabButtons=mutableMapOf<String,com.google.android.material.button.MaterialButton>()
    private var job: Job? = null

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        tab = if ((s?.getString("tab") ?: tab)=="prep") "prep" else "order"
        val root = Ui.box(requireContext(),12)
        val row = Ui.row(requireContext())
        listOf("order" to UiStrings.s(com.familyrecipes.android.R.string.ui_d85522beda), "prep" to UiStrings.s(com.familyrecipes.android.R.string.ui_90e2746c75)).forEach { (key, title) ->
            val button=Ui.button(requireContext(), title) {showTab(key)}
            tabButtons[key]=button
            row.addView(
                button,
                LinearLayout.LayoutParams(0, -2, 1f))
        }
        root.addView(row)
        val section=Ui.row(requireContext())
        heading=Ui.text(requireContext(),UiStrings.s(com.familyrecipes.android.R.string.ui_e92d29c51a),17f,true)
        section.addView(heading,LinearLayout.LayoutParams(0,-2,1f))
        addButton=Ui.button(requireContext(),UiStrings.s(com.familyrecipes.android.R.string.ui_f9616f9a11),true) {
            startActivity(Intent(requireContext(),MealEditorActivity::class.java).putExtra("kind",if(tab=="prep")"prep" else "order"))
        }
        section.addView(addButton,LinearLayout.LayoutParams(-2,-2));root.addView(section)
        list = Ui.box(requireContext(), 0)
        drafts = Ui.box(requireContext(), 0)
        val content=Ui.box(requireContext(),0)
        content.addView(list);content.addView(drafts)
        root.addView(Ui.scroll(requireContext(), content), LinearLayout.LayoutParams(-1, 0, 1f))
        return root
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

    fun showTab(value:String) { tab=if(value=="prep")"prep" else "order";load() }

    private fun load() {
        if (!::list.isInitialized) return
        heading.text=when(tab){"prep"->UiStrings.s(com.familyrecipes.android.R.string.ui_e92d29c51a);"order"->UiStrings.s(com.familyrecipes.android.R.string.ui_3368ec1127);else->UiStrings.s(com.familyrecipes.android.R.string.ui_82bb31b616)}
        addButton.text=if(tab=="prep")UiStrings.s(com.familyrecipes.android.R.string.ui_f9616f9a11) else UiStrings.s(com.familyrecipes.android.R.string.ui_880468380a)
        tabButtons.forEach{(key,b)->
            b.setTextColor(if(key==tab)Ui.blue else Ui.muted)
            b.backgroundTintList=android.content.res.ColorStateList.valueOf(if(key==tab)Ui.pale else Ui.bg)
            b.strokeWidth=if(key==tab)requireContext().dp(1) else 0
            b.strokeColor=android.content.res.ColorStateList.valueOf(Ui.blue)
        }
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
                            card.addView(
                                Ui.text(
                                    requireContext(),
                                    if (m.ownerId == PreferenceManager.userId)
                                        UiStrings.f(com.familyrecipes.android.R.string.my_shopping_count, m.shopping.size)
                                    else UiStrings.s(com.familyrecipes.android.R.string.ui_11d0c6e5bc),
                                    14f))
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
