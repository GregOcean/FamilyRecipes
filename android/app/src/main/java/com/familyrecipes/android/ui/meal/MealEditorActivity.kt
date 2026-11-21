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
import com.familyrecipes.android.ui.design.Ui.dp
import com.familyrecipes.android.R
import com.familyrecipes.android.ui.recipe.RecipePickerActivity
import com.google.gson.Gson
import kotlinx.coroutines.*

class MealEditorActivity : AppCompatActivity() {
    private val account = LocalStore.account
    private lateinit var key: String
    private lateinit var root: LinearLayout
    private lateinit var title: EditText
    private lateinit var dishList: LinearLayout
    private lateinit var date: EditText
    private lateinit var textAdd: EditText
    private lateinit var shoppingList: LinearLayout
    private var meal = ""
    private var ready = false
    private var busy = false
    private var saved = false
    private var hasDraft=false
    private lateinit var model: MealArrangement
    private val picker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
            if (r.resultCode == Activity.RESULT_OK) {
                val added =
                    Gson()
                        .fromJson(r.data?.getStringExtra("dishes"), Array<MealDish>::class.java)
                        .orEmpty()
                        .toList()
                lifecycleScope.launch {
                    while(!ready)delay(25)
                    saved=false
                    model=model.copy(dishes=MealDefaults.mergeDishes(model.dishes,added))
                    renderDishes()
                    draft()
                }
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
                kitchenId = intent.getLongExtra("kitchen_id",0).takeIf { it>0 } ?: PreferenceManager.currentKitchenId,
                kind = intent.getStringExtra("kind") ?: "order")
        root = Ui.box(this)
        setContentView(Ui.scroll(this, root))
        root.addView(Ui.text(this, UiStrings.s(com.familyrecipes.android.R.string.ui_fb4ca1cf1b)))
        lifecycleScope.launch {
            try {
                DraftWrites.flush()
                val stored = LocalStore.instance.records().get(account, "draft", key)
                hasDraft=stored!=null
                model =
                    if (stored != null) Gson().fromJson(stored, MealArrangement::class.java)
                    else if (id != null) Ui.data(ApiClient.getService().meal(id)) else model
                if(stored==null && id==null) {
                    val next=MealDefaults.next(java.time.LocalDateTime.now())
                    model=model.copy(title="${next.date} - ${UiStrings.domain(next.meal)}",mealDate=next.date.toString(),meal=next.meal)
                }
                if (id != null && model.ownerId != PreferenceManager.userId && stored == null) {
                    model = Ui.data(ApiClient.getService().meal(id, original = false))
                }
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
                root.addView(Ui.text(this@MealEditorActivity, Ui.errorMessage(this@MealEditorActivity,e)))
                root.addView(Ui.back(this@MealEditorActivity) { finish() })
            }
        }
    }

    override fun onSaveInstanceState(b: Bundle) {
        super.onSaveInstanceState(b)
        b.putString("key", key)
    }

    private fun sectionTitle(value: String): TextView =
        Ui.text(this, value, 20f, true).apply { setPadding(0, dp(14), 0, dp(2)) }

    private fun draw() {
        ready = false
        root.removeAllViews()
        val isPrep = model.kind == "prep"
        val heading=Ui.row(this)
        heading.addView(Ui.back(this){finish()})
        heading.addView(Ui.text(this,if(isPrep)getString(R.string.ui_90e2746c75) else getString(R.string.ui_d85522beda),22f,true))
        root.addView(heading)
        val editable = model.ownerId == null || model.ownerId == PreferenceManager.userId

        title = Ui.field(this, UiStrings.s(com.familyrecipes.android.R.string.ui_8ee9f276a6), model.title)
        root.addView(title)

        // 1. 这一餐：先确认时间，再决定吃什么。
        root.addView(sectionTitle(getString(R.string.meal_when)))
        date = Ui.field(this, getString(R.string.review_date_hint), model.mealDate.orEmpty()).apply {
            isFocusable=false
            setOnClickListener {
                val current=runCatching{java.time.LocalDate.parse(text.toString())}.getOrDefault(java.time.LocalDate.now())
                val picker=android.app.DatePickerDialog(this@MealEditorActivity,{_,year,month,day->
                    setText(java.time.LocalDate.of(year,month+1,day).toString())
                },current.year,current.monthValue-1,current.dayOfMonth)
                picker.setButton(android.content.DialogInterface.BUTTON_NEUTRAL,getString(R.string.review_clear_date)){_,_->setText("")}
                picker.show()
            }
        }
        root.addView(date)
        meal = model.meal.orEmpty()
        val meals = listOf(UiStrings.s(com.familyrecipes.android.R.string.ui_7dc2d8a222), "早餐", "午餐", "晚餐", "加餐")
        val select =
            Ui.select(this, meals.map{UiStrings.domain(it)}, meals.indexOf(meal).coerceAtLeast(0)) {
                meal = if (it == 0) "" else meals[it]
                if(ready)saved=false
                draft()
            }
        root.addView(select)

        // 2. 菜单：备菜按“菜 → 所需食材”展示。
        root.addView(sectionTitle(if(isPrep)getString(R.string.meal_prep_menu) else getString(R.string.meal_dish_list)))
        dishList=Ui.box(this,0);root.addView(dishList)
        renderDishes()

        // 3. 文字添加：一行一道菜，冒号后接食材。
        root.addView(sectionTitle(getString(R.string.meal_add_by_text)))
        textAdd = Ui.field(this, getString(R.string.meal_text_hint), multi = true).apply { minLines = 3 }
        root.addView(textAdd)
        root.addView(Ui.button(this, getString(R.string.meal_parse_add)) { addFromText() })
        val choose =
            Ui.button(this, UiStrings.s(com.familyrecipes.android.R.string.ui_805cb9cde8)) {
                picker.launch(Intent(this, RecipePickerActivity::class.java))
            }
        root.addView(choose)
        if (isPrep)
            root.addView(Ui.button(this, getString(R.string.meal_generate_ingredients)) { generateIngredients() })

        // 4. 备菜的采购清单，按食材逐项打勾。
        if (isPrep) {
            root.addView(sectionTitle(getString(R.string.meal_need_buy)))
            shoppingList = Ui.box(this, 0)
            root.addView(shoppingList)
            renderShopping()
            val addRow = Ui.row(this)
            val itemAdd = Ui.field(this, getString(R.string.meal_add_shopping_hint)).apply {
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(8) }
            }
            addRow.addView(itemAdd)
            addRow.addView(Ui.button(this, getString(R.string.meal_parse_add)) {
                val name = itemAdd.text.toString().trim()
                if (name.isNotBlank()) {
                    saved = false
                    model = model.copy(shopping = mergeShopping(model.shopping, listOf(name)))
                    itemAdd.setText("")
                    renderShopping()
                    draft()
                }
            })
            root.addView(addRow)
        }

        listOf(title, date).forEach {
            it.isEnabled = editable
            it.doAfterTextChanged { if(ready && !busy)saved=false;draft() }
        }
        textAdd.isEnabled = editable
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

    private fun renderDishes() {
        dishList.removeAllViews()
        val editable=model.ownerId==null || model.ownerId==PreferenceManager.userId
        val isPrep = model.kind == "prep"
        if(model.dishes.isEmpty())dishList.addView(Ui.text(this,getString(R.string.meal_dishes_empty)))
        model.dishes.forEachIndexed { index,dish ->
            val card=Ui.card(this)
            val row=Ui.row(this)
            val info=Ui.box(this,0).apply{setBackgroundColor(android.graphics.Color.TRANSPARENT)}
            info.addView(Ui.text(this,"${index+1}. ${dish.name}",18f,true))
            info.addView(Ui.text(this,getString(if(dish.recipeId!=null)R.string.meal_recipe_linked else R.string.meal_manual_dish),14f).apply{setTextColor(Ui.muted)})
            row.addView(info,LinearLayout.LayoutParams(0,-2,1f))
            fun openRecipe(){startActivity(Intent(this,com.familyrecipes.android.ui.recipe.RecipeDetailActivity::class.java).putExtra("recipe_id",dish.recipeId))}
            fun rename(){
                val input=Ui.field(this,getString(R.string.meal_dish_name),dish.name)
                val dialog=androidx.appcompat.app.AlertDialog.Builder(this).setTitle(R.string.meal_rename_dish)
                    .setView(input).setNegativeButton(android.R.string.cancel,null).setPositiveButton(android.R.string.ok,null).create()
                dialog.setOnShowListener {dialog.getButton(-1).setOnClickListener {
                    val name=input.text.toString().trim()
                    if(name.isBlank()){input.error=getString(R.string.meal_dish_name);return@setOnClickListener}
                    model=model.copy(dishes=model.dishes.mapIndexed { i,d->if(i==index)d.copy(name=name) else d })
                    saved=false;renderDishes();draft();dialog.dismiss()
                }};dialog.show()
            }
            fun editIngredients(){
                val input=Ui.field(this,getString(R.string.meal_dish_ingredients),dish.ingredients.orEmpty().joinToString("、"),true)
                val dialog=androidx.appcompat.app.AlertDialog.Builder(this).setTitle(R.string.meal_dish_ingredients)
                    .setView(input).setNegativeButton(android.R.string.cancel,null).setPositiveButton(android.R.string.ok,null).create()
                dialog.setOnShowListener {dialog.getButton(-1).setOnClickListener {
                    val items=parseIngredients(input.text.toString())
                    model=model.copy(
                        dishes=model.dishes.mapIndexed { i,d->if(i==index)d.copy(ingredients=items) else d },
                        shopping=mergeShopping(model.shopping,items))
                    saved=false;renderDishes();if(::shoppingList.isInitialized)renderShopping();draft();dialog.dismiss()
                }};dialog.show()
            }
            if(dish.recipeId!=null){card.isFocusable=true;card.setOnClickListener{openRecipe()}}
            if(editable) {
                val more=androidx.appcompat.widget.AppCompatImageButton(this).apply {
                    setImageResource(R.drawable.ic_more_vert);imageTintList=android.content.res.ColorStateList.valueOf(Ui.blue)
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    contentDescription=getString(R.string.review_meal_edit_dish)+": "+dish.name
                    setPadding(dp(12),dp(12),dp(12),dp(12))
                    setOnClickListener {anchor->PopupMenu(this@MealEditorActivity,anchor).apply {
                        if(dish.recipeId!=null)menu.add(0,0,0,R.string.meal_open_recipe)
                        menu.add(0,1,1,R.string.meal_rename_dish)
                        if(isPrep)menu.add(0,2,2,R.string.meal_dish_ingredients)
                        menu.add(0,3,3,R.string.meal_remove_dish)
                        setOnMenuItemClickListener {item->when(item.itemId){
                            0->openRecipe()
                            1->rename()
                            2->editIngredients()
                            3->{model=model.copy(dishes=model.dishes.filterIndexed { i,_->i!=index });saved=false;renderDishes();if(::shoppingList.isInitialized)renderShopping();draft()}
                            else->{}
                        };true};show()
                    }}
                }
                row.addView(more,LinearLayout.LayoutParams(dp(48),dp(48)))
            } else if(dish.recipeId!=null)row.addView(Ui.text(this,"›",24f))
            card.addView(row)
            if (!dish.ingredients.isNullOrEmpty()) {
                card.addView(Ui.text(this, getString(R.string.meal_prep_ingredients, dish.ingredients.orEmpty().joinToString("、")), 14f).apply {
                    setTextColor(Ui.muted); setPadding(0, 0, 0, dp(2))
                })
            } else if (isPrep) {
                card.addView(Ui.text(this, getString(R.string.meal_no_ingredients), 13f).apply {
                    setTextColor(Ui.muted); setPadding(0, 0, 0, dp(2))
                })
            }
            dishList.addView(card)
        }
    }

    private fun renderShopping() {
        if (!::shoppingList.isInitialized) return
        shoppingList.removeAllViews()
        if (model.shopping.isEmpty()) {
            shoppingList.addView(Ui.text(this, getString(R.string.meal_shopping_empty), 14f).apply { setTextColor(Ui.muted) })
            return
        }
        model.shopping.forEachIndexed { i, item ->
            val row = Ui.row(this)
            row.addView(
                CheckBox(this).apply {
                    text = item.name
                    isChecked = item.done
                    setOnCheckedChangeListener { _, v ->
                        saved = false
                        model = model.copy(shopping = model.shopping.mapIndexed { j, x -> if (j == i) x.copy(done = v) else x })
                        draft()
                    }
                }, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(Ui.button(this, getString(R.string.meal_remove_item)) {
                saved = false
                model = model.copy(shopping = model.shopping.filterIndexed { j, _ -> j != i })
                renderShopping(); draft()
            }.apply { textSize = 13f; minHeight = (36 * resources.displayMetrics.density).toInt() })
            shoppingList.addView(row)
        }
    }

    private fun parseIngredients(text: String): List<String> =
        text.lines().flatMap { it.split('、', '，', ',', '；', ';') }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(30)

    private fun mergeShopping(existing: List<ShoppingItem>, names: List<String>): List<ShoppingItem> {
        val byName = existing.associateBy { it.name }
        return (existing.map { it.name } + names)
            .distinct()
            .map { byName[it] ?: ShoppingItem(it) }
            .take(200)
    }

    /** 一行一道菜：菜名：食材1、食材2；没有冒号时只添加菜名。 */
    private fun addFromText() {
        val raw = textAdd.text.toString().trim()
        if (raw.isBlank()) {
            Toast.makeText(this, getString(R.string.meal_quick_empty), Toast.LENGTH_SHORT).show()
            return
        }
        val parsed = raw.lines().mapNotNull { line ->
            val value = line.trim().removePrefix("-").trim()
            if (value.isBlank()) return@mapNotNull null
            val parts = value.split('：', ':', limit = 2)
            val name = parts[0].trim().take(100)
            if (name.isBlank()) return@mapNotNull null
            MealDish(name = name, ingredients = if (parts.size > 1) parseIngredients(parts[1]) else emptyList())
        }
        if (parsed.isEmpty()) return
        var dishes = model.dishes
        val addedIngredients = mutableListOf<String>()
        parsed.forEach { dish ->
            val existing = dishes.indexOfFirst { it.name == dish.name }
            if (existing >= 0) {
                dishes = dishes.mapIndexed { i, d ->
                    if (i == existing) d.copy(ingredients = (d.ingredients.orEmpty() + dish.ingredients.orEmpty()).distinct()) else d
                }
            } else if (dishes.size < 100) {
                dishes = dishes + dish
            }
            addedIngredients.addAll(dish.ingredients.orEmpty())
        }
        saved = false
        model = model.copy(dishes = dishes, shopping = mergeShopping(model.shopping, addedIngredients))
        textAdd.setText("")
        renderDishes()
        if (::shoppingList.isInitialized) renderShopping()
        draft()
    }

    private fun value() =
        model.copy(
            title = title.text.toString().trim(),
            mealDate = date.text.toString().trim().ifBlank { null },
            meal = meal.ifBlank { null },
            dishes = model.dishes,
            shopping = model.shopping)

    private fun draft() {
        if (ready && !hasDraft && title.text.isBlank() && model.dishes.isEmpty() && model.shopping.isEmpty()) return
        if (ready &&
            !saved &&
            !busy &&
            (model.ownerId == null || model.ownerId == PreferenceManager.userId))
            {hasDraft=true;DraftWrites.put(account, key, Gson().toJson(value()))}
    }

    override fun onStop() {
        draft()
        super.onStop()
    }

    /** 从关联菜谱和库存生成每道菜要买的食材。 */
    private fun generateIngredients() {
        if (busy) return
        busy = true
        lifecycleScope.launch {
            try {
                val snapshot = value()
                val stock =
                    Ui.data(ApiClient.getService().getFridgeItems(snapshot.kitchenId, original = true))
                        .filter { it.status != "expired" }
                        .map { it.ingredientId }
                        .toSet()
                val dishes = snapshot.dishes.map { dish ->
                    val id = dish.recipeId ?: return@map dish
                    val items = Ui.data(ApiClient.getService().getRecipeDetail(id))
                        .ingredients
                        .orEmpty()
                        .filter { it.ingredientId !in stock }
                        .map { "${it.ingredient?.name.orEmpty()} ${it.amount.orEmpty()}".trim() }
                        .filter { it.isNotBlank() }
                    dish.copy(ingredients = (dish.ingredients.orEmpty() + items).distinct())
                }
                val all = dishes.flatMap { it.ingredients.orEmpty() }.distinct()
                model = snapshot.copy(dishes = dishes, shopping = mergeShopping(snapshot.shopping, all))
                renderDishes(); renderShopping()
                Toast.makeText(this@MealEditorActivity, UiStrings.s(com.familyrecipes.android.R.string.ui_24ea6b09e1), Toast.LENGTH_LONG)
                    .show()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Ui.toast(this@MealEditorActivity, e)
            } finally {
                busy = false
                saved=false
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
                hasDraft=false
                key="meal:${result.id}"
                setResult(RESULT_OK)
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
        if(groups.isEmpty()) {
            Toast.makeText(this,getString(R.string.review_saved_no_group),Toast.LENGTH_LONG).show()
            finish();return
        }
        if (groups.size == 1) {
            // 只有一个群时直接转发，减少一次点击。
            Ui.data(ApiClient.getService().shareMeal(m.id!!, ChatShareRequest(groups.first().id!!)))
            Toast.makeText(this, UiStrings.s(com.familyrecipes.android.R.string.ui_54bba45c94), Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setOnCancelListener { saved=false }
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
