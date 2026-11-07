package com.familyrecipes.android.ui.meal

import com.familyrecipes.android.ui.design.UiStrings
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.familyrecipes.android.data.local.*
import com.familyrecipes.android.data.model.*
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.ui.design.Ui
import com.familyrecipes.android.ui.recipe.RecipePickerActivity
import com.google.gson.Gson
import kotlinx.coroutines.*

class MealEditorActivity : AppCompatActivity() {
    private val account = LocalStore.account
    private lateinit var key: String
    private lateinit var root: LinearLayout
    private lateinit var title: EditText
    private lateinit var dishes: EditText
    private lateinit var date: EditText
    private lateinit var shopping: EditText
    private var meal = ""
    private var ready = false
    private var busy = false
    private var saved = false
    private lateinit var model: MealArrangement
    private val picker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
            if (r.resultCode == Activity.RESULT_OK) {
                val added =
                    Gson()
                        .fromJson(r.data?.getStringExtra("dishes"), Array<MealDish>::class.java)
                        .orEmpty()
                        .toList()
                model = model.copy(dishes = (model.dishes + added).distinctBy { it.name })
                dishes.setText(
                    (dishes.text.toString().split("、").filter { it.isNotBlank() } +
                            added.map { it.name })
                        .distinct()
                        .joinToString("、"))
                draft()
            }
        }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val id =
            intent.getLongExtra("meal_id", 0).takeIf { it > 0 }
                ?: intent.data?.lastPathSegment?.toLongOrNull()
        key =
            intent.getStringExtra("draft_key")
                ?: state?.getString("key")
                ?: "meal:${id?:java.util.UUID.randomUUID()}"
        model =
            MealArrangement(
                id = id,
                kitchenId = PreferenceManager.currentKitchenId,
                kind = intent.getStringExtra("kind") ?: "order")
        root = Ui.box(this)
        setContentView(Ui.scroll(this, root))
        root.addView(Ui.text(this, UiStrings.s(com.familyrecipes.android.R.string.ui_fb4ca1cf1b)))
        lifecycleScope.launch {
            try {
                DraftWrites.flush()
                val stored = LocalStore.instance.records().get(account, "draft", key)
                model =
                    if (stored != null) Gson().fromJson(stored, MealArrangement::class.java)
                    else if (id != null) Ui.data(ApiClient.getService().meal(id)) else model
                intent.getStringExtra("recipe_name")?.let {
                    if (model.dishes.isEmpty())
                        model =
                            model.copy(
                                dishes =
                                    listOf(
                                        MealDish(
                                            intent.getLongExtra("recipe_id", 0).takeIf { it > 0 },
                                            it)))
                }
                draw()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                root.removeAllViews()
                root.addView(Ui.text(this@MealEditorActivity, e.message ?: UiStrings.s(com.familyrecipes.android.R.string.ui_866b795eae)))
                root.addView(Ui.back(this@MealEditorActivity) { finish() })
            }
        }
    }

    override fun onSaveInstanceState(b: Bundle) {
        super.onSaveInstanceState(b)
        b.putString("key", key)
    }

    private fun draw() {
        ready = false
        root.removeAllViews()
        root.addView(Ui.back(this) { finish() })
        root.addView(Ui.text(this, if (model.kind == "prep") UiStrings.s(com.familyrecipes.android.R.string.ui_90e2746c75) else UiStrings.s(com.familyrecipes.android.R.string.ui_d85522beda), 24f, true))
        val editable = model.ownerId == null || model.ownerId == PreferenceManager.userId
        title = Ui.field(this, UiStrings.s(com.familyrecipes.android.R.string.ui_8ee9f276a6), model.title)
        root.addView(title)
        dishes = Ui.field(this, UiStrings.s(com.familyrecipes.android.R.string.ui_e4cfb95fab), model.dishes.joinToString("、") { it.name }, true)
        root.addView(dishes)
        val choose =
            Ui.button(this, UiStrings.s(com.familyrecipes.android.R.string.ui_805cb9cde8)) {
                picker.launch(Intent(this, RecipePickerActivity::class.java))
            }
        root.addView(choose)
        date = Ui.field(this, UiStrings.s(com.familyrecipes.android.R.string.ui_f0f2a7ce59), model.mealDate.orEmpty())
        root.addView(date)
        meal = model.meal.orEmpty()
        val meals = listOf(UiStrings.s(com.familyrecipes.android.R.string.ui_7dc2d8a222), "早餐", "午餐", "晚餐", "加餐")
        val select =
            Ui.select(this, meals.map{UiStrings.domain(it)}, meals.indexOf(meal).coerceAtLeast(0)) {
                meal = if (it == 0) "" else meals[it]
                draft()
            }
        root.addView(select)
        shopping = Ui.field(this, UiStrings.s(com.familyrecipes.android.R.string.ui_be850c21d5), model.shopping.joinToString("\n") { it.name }, true)
        if (model.kind == "prep") {
            root.addView(Ui.text(this, UiStrings.s(com.familyrecipes.android.R.string.ui_ca89d33725), 20f, true))
            root.addView(shopping)
            root.addView(Ui.button(this, UiStrings.s(com.familyrecipes.android.R.string.ui_6d58daf417)) { generateShopping() })
            model.shopping.forEachIndexed { i, item ->
                root.addView(
                    CheckBox(this).apply {
                        text = item.name
                        isChecked = item.done
                        isEnabled = editable
                        setOnCheckedChangeListener { _, v ->
                            model =
                                model.copy(
                                    shopping =
                                        model.shopping.mapIndexed { j, x ->
                                            if (j == i) x.copy(done = v) else x
                                        })
                            draft()
                        }
                    })
            }
        }
        listOf(title, dishes, date, shopping).forEach {
            it.isEnabled = editable
            it.doAfterTextChanged { draft() }
        }
        choose.isEnabled = editable
        select.isEnabled = editable
        if (editable) {
            root.addView(Ui.button(this, UiStrings.s(com.familyrecipes.android.R.string.ui_be5fbbe34c), true) { save(false) })
            root.addView(Ui.button(this, UiStrings.s(com.familyrecipes.android.R.string.ui_72d32d3460)) { save(true) })
        }
        if (model.kind == "order")
            root.addView(
                Ui.button(this, UiStrings.s(com.familyrecipes.android.R.string.ui_954408d0e5)) {
                    val snapshot = value()
                    model =
                        snapshot.copy(
                            id = null,
                            ownerId = null,
                            revision = null,
                            kind = "prep",
                            sourceId = snapshot.id,
                            kitchenId = PreferenceManager.currentKitchenId)
                    key = "meal:${java.util.UUID.randomUUID()}"
                    saved = false
                    draw()
                    draft()
                })
        ready = true
    }

    private fun value() =
        model.copy(
            title = title.text.toString().trim(),
            mealDate = date.text.toString().trim().ifBlank { null },
            meal = meal.ifBlank { null },
            dishes =
                dishes.text
                    .toString()
                    .split(Regex("[、，,\\n]"))
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .map { n -> model.dishes.find { it.name == n } ?: MealDish(name = n) },
            shopping =
                shopping.text
                    .toString()
                    .lines()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .map { n -> model.shopping.find { it.name == n } ?: ShoppingItem(n) })

    private fun draft() {
        if (ready && title.text.isBlank() && dishes.text.isBlank()) return
        if (ready &&
            !saved &&
            !busy &&
            (model.ownerId == null || model.ownerId == PreferenceManager.userId))
            DraftWrites.put(account, key, Gson().toJson(value()))
    }

    override fun onStop() {
        draft()
        super.onStop()
    }

    private fun generateShopping() {
        if (busy) return
        busy = true
        lifecycleScope.launch {
            try {
                val snapshot = value()
                val stock =
                    Ui.data(ApiClient.getService().getFridgeItems(snapshot.kitchenId))
                        .filter { it.status != "expired" }
                        .map { it.ingredientId }
                        .toSet()
                val extra = mutableListOf<String>()
                for (d in snapshot.dishes) {
                    d.recipeId?.let { id ->
                        Ui.data(ApiClient.getService().getRecipeDetail(id))
                            .ingredients
                            .orEmpty()
                            .filter { it.ingredientId !in stock }
                            .forEach {
                                extra.add(
                                    "${it.ingredient?.name.orEmpty()} ${it.amount.orEmpty()}"
                                        .trim())
                            }
                    }
                }
                shopping.setText(
                    (shopping.text.toString().lines() + extra)
                        .filter { it.isNotBlank() }
                        .distinct()
                        .joinToString("\n"))
                Toast.makeText(this@MealEditorActivity, UiStrings.s(com.familyrecipes.android.R.string.ui_24ea6b09e1), Toast.LENGTH_LONG)
                    .show()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Ui.toast(this@MealEditorActivity, e)
            } finally {
                busy = false
                draft()
            }
        }
    }

    private fun enableEditing(enabled: Boolean) {
        fun change(v: android.view.View) {
            v.isEnabled = enabled
            if (v is android.view.ViewGroup) for (i in 0 until v.childCount) change(v.getChildAt(i))
        }
        change(root)
    }

    private fun save(share: Boolean) {
        if (busy) return
        val v = value()
        if (v.title.isBlank()) {
            title.error = UiStrings.s(com.familyrecipes.android.R.string.ui_856d0df5f2)
            return
        }
        if (v.mealDate != null && runCatching { java.time.LocalDate.parse(v.mealDate) }.isFailure) {
            date.error = UiStrings.s(com.familyrecipes.android.R.string.ui_17f50e0655)
            return
        }
        draft()
        busy = true
        enableEditing(false)
        lifecycleScope.launch {
            try {
                val result = Ui.data(ApiClient.getService().saveMeal(v))
                model = result
                saved = true
                DraftWrites.clear(account, key)
                if (share) share(result)
                else {
                    Toast.makeText(this@MealEditorActivity, UiStrings.s(com.familyrecipes.android.R.string.ui_f8dfedcd8a), Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Ui.toast(this@MealEditorActivity, e)
            } finally {
                busy = false
                enableEditing(true)
            }
        }
    }

    private suspend fun share(m: MealArrangement) {
        val groups = Ui.data(ApiClient.getService().getGroupsList())
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(UiStrings.s(com.familyrecipes.android.R.string.ui_c809061b3c))
            .setItems(groups.map { it.name }.toTypedArray()) { _, index ->
                lifecycleScope.launch {
                    try {
                        Ui.ok(
                            ApiClient.getService()
                                .shareMeal(m.id!!, ChatShareRequest(groups[index].id!!)))
                        Toast.makeText(this@MealEditorActivity, UiStrings.s(com.familyrecipes.android.R.string.ui_54bba45c94), Toast.LENGTH_SHORT).show()
                        finish()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Ui.toast(this@MealEditorActivity, e)
                    }
                }
            }
            .setNegativeButton(UiStrings.s(com.familyrecipes.android.R.string.ui_84e2805b5c)) { _, _ -> finish() }
            .show()
    }
}
