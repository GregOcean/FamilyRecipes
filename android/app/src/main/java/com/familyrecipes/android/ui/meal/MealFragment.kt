package com.familyrecipes.android.ui.meal

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
        listOf("prep" to "备菜", "order" to "点菜", "draft" to "草稿").forEach { (key, title) ->
            val button=Ui.button(requireContext(), title) {showTab(key)}
            tabButtons[key]=button
            row.addView(
                button,
                LinearLayout.LayoutParams(0, -2, 1f))
        }
        root.addView(row)
        val section=Ui.row(requireContext())
        heading=Ui.text(requireContext(),"准备做的菜",21f,true)
        section.addView(heading,LinearLayout.LayoutParams(0,-2,1f))
        addButton=Ui.button(requireContext(),"＋ 新建备菜") {
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
        heading.text=when(tab){"prep"->"准备做的菜";"order"->"想吃的菜单";else->"随时接着写"}
        addButton.text=if(tab=="prep")"＋ 新建备菜" else "＋ 新建点菜"
        addButton.visibility=if(tab=="draft")View.GONE else View.VISIBLE
        tabButtons.forEach{(key,b)->b.setTextColor(if(key==tab)android.graphics.Color.WHITE else Ui.blue);b.backgroundTintList=android.content.res.ColorStateList.valueOf(if(key==tab)Ui.blue else Ui.pale)}
        job?.cancel()
        job =
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    list.removeAllViews()
                    list.addView(Ui.text(requireContext(), "加载中…"))
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
                                    Ui.button(requireContext(), title.ifBlank { "未命名草稿" }) {
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
                            list.addView(Ui.text(requireContext(), "还没有草稿，编辑时会自动保存。"))
                    } else {
                        val kitchen = PreferenceManager.currentKitchenId ?: error("请先选择厨房")
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
                                        .ifBlank { "时间未定" },
                                    14f))
                            card.addView(
                                Ui.text(requireContext(), m.dishes.joinToString("、") { it.name }))
                            card.addView(
                                Ui.text(
                                    requireContext(),
                                    if (m.ownerId == PreferenceManager.userId)
                                        "我创建 · 采购 ${m.shopping.size} 项"
                                    else "分享给我的",
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
                                    "还没有${if(tab=="prep")"备菜计划" else "点菜菜单"}，可以独自创建，也可以之后分享。"))
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    list.removeAllViews()
                    list.addView(Ui.button(requireContext(), "${e.message} · 重试") { load() })
                }
            }
    }

    override fun onDestroyView() {
        job?.cancel()
        super.onDestroyView()
    }
}
