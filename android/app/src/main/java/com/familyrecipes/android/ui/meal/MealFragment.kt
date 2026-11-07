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
import com.familyrecipes.android.ui.recipe.EditRecipeActivity
import com.google.gson.Gson
import kotlinx.coroutines.*

class MealFragment : Fragment() {
    private lateinit var list: LinearLayout
    private lateinit var heading: TextView
    private lateinit var addButton: com.google.android.material.button.MaterialButton
    private var tab = "prep"
    private val tabButtons=mutableMapOf<String,com.google.android.material.button.MaterialButton>()
    private var job: Job? = null

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        tab = s?.getString("tab") ?: tab
        val root = Ui.box(requireContext())
        val row = Ui.row(requireContext())
        listOf("prep" to UiStrings.s(com.familyrecipes.android.R.string.ui_90e2746c75), "order" to UiStrings.s(com.familyrecipes.android.R.string.ui_d85522beda), "draft" to UiStrings.s(com.familyrecipes.android.R.string.ui_22b4334f91)).forEach { (key, title) ->
            val button=Ui.button(requireContext(), title) {showTab(key)}
            tabButtons[key]=button
            row.addView(
                button,
                LinearLayout.LayoutParams(0, -2, 1f))
        }
        root.addView(row)
        val section=Ui.row(requireContext())
        heading=Ui.text(requireContext(),UiStrings.s(com.familyrecipes.android.R.string.ui_e92d29c51a),21f,true)
        section.addView(heading,LinearLayout.LayoutParams(0,-2,1f))
        addButton=Ui.button(requireContext(),UiStrings.s(com.familyrecipes.android.R.string.ui_f9616f9a11)) {
            startActivity(Intent(requireContext(),MealEditorActivity::class.java).putExtra("kind",if(tab=="prep")"prep" else "order"))
        }
        section.addView(addButton);root.addView(section)
        list = Ui.box(requireContext(), 0)
        root.addView(Ui.scroll(requireContext(), list), LinearLayout.LayoutParams(-1, 0, 1f))
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

    fun showDrafts() = showTab("draft")
    fun showTab(value:String) { tab=value;load() }

    private fun load() {
        if (!::list.isInitialized) return
        heading.text=when(tab){"prep"->UiStrings.s(com.familyrecipes.android.R.string.ui_e92d29c51a);"order"->UiStrings.s(com.familyrecipes.android.R.string.ui_3368ec1127);else->UiStrings.s(com.familyrecipes.android.R.string.ui_82bb31b616)}
        addButton.text=if(tab=="prep")UiStrings.s(com.familyrecipes.android.R.string.ui_f9616f9a11) else UiStrings.s(com.familyrecipes.android.R.string.ui_880468380a)
        addButton.visibility=if(tab=="draft")View.GONE else View.VISIBLE
        tabButtons.forEach{(key,b)->b.setTextColor(if(key==tab)Ui.onBlue else Ui.blue);b.backgroundTintList=android.content.res.ColorStateList.valueOf(if(key==tab)Ui.blue else Ui.pale)}
        job?.cancel()
        job =
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    list.removeAllViews()
                    list.addView(Ui.text(requireContext(), UiStrings.s(com.familyrecipes.android.R.string.ui_fb4ca1cf1b)))
                    if (tab == "draft") {
                        DraftWrites.flush()
                        val records =
                            LocalStore.instance.records().list(LocalStore.account, "draft")
                        list.removeAllViews()
                        records
                            .filter {
                                it.key.startsWith("meal:") ||
                                    it.key.startsWith("recipe:") ||
                                    it.key.startsWith("cooking:")
                            }
                            .forEach { d ->
                                val title =
                                    runCatching {
                                            Gson()
                                                .fromJson(
                                                    d.json, com.google.gson.JsonObject::class.java)
                                                .get("title")
                                                ?.asString
                                        }
                                        .getOrNull()
                                        .orEmpty()
                                list.addView(
                                    Ui.button(requireContext(), title.ifBlank { UiStrings.s(com.familyrecipes.android.R.string.ui_91b34afa6c) }) {
                                        val target =
                                            if (d.key.startsWith("meal:"))
                                                MealEditorActivity::class.java
                                            else if (d.key.startsWith("cooking:"))
                                                com.familyrecipes.android.ui.recipe
                                                        .CookingRecordActivity::class
                                                    .java
                                            else EditRecipeActivity::class.java
                                        val intent =
                                            Intent(requireContext(), target)
                                                .putExtra("draft_key", d.key)
                                        if (target != MealEditorActivity::class.java)
                                            d.key.split(":").getOrNull(1)?.toLongOrNull()?.let {
                                                intent.putExtra("recipe_id", it)
                                            }
                                        startActivity(intent)
                                    })
                            }
                        if (list.childCount == 0)
                            list.addView(Ui.text(requireContext(), UiStrings.s(com.familyrecipes.android.R.string.ui_0b67983743)))
                    } else {
                        val kitchen = PreferenceManager.currentKitchenId ?: error(UiStrings.s(com.familyrecipes.android.R.string.ui_eb006cd6f1))
                        val rows =
                            Ui.data(ApiClient.getService().meals(kitchen)).filter { it.kind == tab }
                        list.removeAllViews()
                        rows.forEach { m ->
                            val card = Ui.card(requireContext())
                            card.addView(Ui.text(requireContext(), m.title, 20f, true))
                            card.addView(
                                Ui.text(
                                    requireContext(),
                                    listOfNotNull(m.mealDate, m.meal?.takeIf { it.isNotBlank() })
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
                    list.addView(Ui.button(requireContext(), UiStrings.f(com.familyrecipes.android.R.string.retry_error_value, e.message)) { load() })
                }
            }
    }

    override fun onDestroyView() {
        job?.cancel()
        super.onDestroyView()
    }
}
