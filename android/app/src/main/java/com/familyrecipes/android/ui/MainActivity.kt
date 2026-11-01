package com.familyrecipes.android.ui

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.familyrecipes.android.R
import com.familyrecipes.android.data.local.PreferenceManager
import com.familyrecipes.android.data.model.Kitchen
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.ui.design.Ui
import com.familyrecipes.android.ui.fridge.*
import com.familyrecipes.android.ui.meal.*
import com.familyrecipes.android.ui.message.MessageFragment
import com.familyrecipes.android.ui.profile.ProfileFragment
import com.familyrecipes.android.ui.recipe.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var drawer: DrawerLayout
    private lateinit var kitchenLabel: Button
    private lateinit var container: FrameLayout
    private lateinit var bottom: LinearLayout
    private var selected = "recipes"
    private var currentKitchenName = "我的厨房"

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        drawer = DrawerLayout(this)
        val shell = Ui.box(this, 0)
        val top =
            Ui.row(this).apply {
                setPadding(12, 8, 12, 8)
                setBackgroundColor(android.graphics.Color.WHITE)
            }
        top.addView(
            Ui.button(this, "☰") { drawer.openDrawer(GravityCompat.START) }
                .apply {
                    contentDescription = "打开工具栏"
                    backgroundTintList =
                        android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.TRANSPARENT)
                },
            LinearLayout.LayoutParams(64.dp(), -2))
        kitchenLabel =
            Ui.button(this, PreferenceManager.currentKitchenName ?: "我的厨房") {
                    showSwitchKitchenDialog()
                }
                .apply {
                    backgroundTintList =
                        android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.TRANSPARENT)
                }
        top.addView(kitchenLabel, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(
            Ui.button(this, "🌻") { showPage("profile") }
                .apply {
                    contentDescription = "个人头像，进入我的"
                    backgroundTintList =
                        android.content.res.ColorStateList.valueOf(0xfffff1bd.toInt())
                },
            LinearLayout.LayoutParams(64.dp(), -2))
        shell.addView(top)
        container = FrameLayout(this).apply { id = R.id.fragment_container }
        shell.addView(container, LinearLayout.LayoutParams(-1, 0, 1f))
        bottom = Ui.row(this).apply { setBackgroundColor(android.graphics.Color.WHITE) }
        shell.addView(bottom)
        drawer.addView(shell, DrawerLayout.LayoutParams(-1, -1))
        val panel = Ui.box(this)
        panel.addView(Ui.text(this, "常用工具", 24f, true))
        listOf(
                "厨房与群组" to { showSwitchKitchenDialog() },
                "创建群聊" to
                    {
                        startActivity(
                            Intent(
                                this,
                                com.familyrecipes.android.ui.social.CreateGroupActivity::class
                                    .java))
                    },
                "扫一扫" to
                    {
                        startActivity(
                            Intent(
                                this,
                                com.familyrecipes.android.ui.social.ScanQRActivity::class.java))
                    },
                "我的二维码" to
                    {
                        startActivity(
                            Intent(
                                this,
                                com.familyrecipes.android.ui.social.MyQRCodeActivity::class.java))
                    },
                "通知、字体与系统设置" to { showPage("profile") })
            .forEach { (label, action) ->
                panel.addView(
                    Ui.button(this, label) {
                        drawer.closeDrawers()
                        action()
                    })
            }
        drawer.addView(
            Ui.scroll(this, panel), DrawerLayout.LayoutParams(300.dp(), -1, Gravity.START))
        setContentView(drawer)
        selected =
            state?.getString("page")
                ?: when (intent.getStringExtra("navigate_to")) {
                    "fridge" -> "stock"
                    "message" -> "messages"
                    "profile" -> "profile"
                    else -> "recipes"
                }
        drawNavigation()
        if (state == null) showPage(selected)
        initializeKitchen()
    }

    private fun Int.dp() = (this * resources.displayMetrics.density).toInt()

    override fun onSaveInstanceState(out: Bundle) {
        super.onSaveInstanceState(out)
        out.putString("page", selected)
    }

    fun showPage(page: String) {
        selected = page
        val fragment: Fragment =
            when (page) {
                "stock" -> FridgeFragment()
                "meals" -> MealFragment()
                "messages" -> MessageFragment()
                "profile" -> ProfileFragment()
                else -> RecipeListFragment()
            }
        val tag = "main:$page"
        val transaction = supportFragmentManager.beginTransaction()
        supportFragmentManager.fragments.forEach { transaction.hide(it) }
        val found = supportFragmentManager.findFragmentByTag(tag)
        if (found == null) transaction.add(container.id, fragment, tag) else transaction.show(found)
        transaction.commit()
        drawNavigation()
    }

    private fun drawNavigation() {
        bottom.removeAllViews()
        listOf(
                Triple("recipes", "菜谱", R.drawable.ic_book_outline),
                Triple("stock", "库存", R.drawable.ic_inventory_outline),
                Triple("add", "＋", R.drawable.ic_add_circle),
                Triple("meals", "安排", R.drawable.ic_calendar),
                Triple("messages", "消息", R.drawable.ic_message))
            .forEach { (page, label, icon) ->
                val b =
                    androidx.appcompat.widget.AppCompatButton(this).apply {
                        text = label
                        isAllCaps = false
                        textSize = if (page == "add") 28f else 16f
                        minimumWidth = 0
                        minWidth = 0
                        setTextColor(
                            if (page == "add") android.graphics.Color.WHITE
                            else if (page == selected) Ui.blue else Ui.muted)
                        background =
                            android.graphics.drawable.GradientDrawable().apply {
                                cornerRadius = 16.dp().toFloat()
                                setColor(
                                    if (page == "add") Ui.blue
                                    else if (page == selected) Ui.pale
                                    else android.graphics.Color.TRANSPARENT)
                            }
                        if (page != "add") {
                            val d =
                                androidx.core.content.ContextCompat.getDrawable(
                                        this@MainActivity, icon)!!
                                    .mutate()
                            d.setTint(if (page == selected) Ui.blue else Ui.muted)
                            d.setBounds(0, 0, 27.dp(), 27.dp())
                            setCompoundDrawables(null, d, null, null)
                        }
                        setPadding(0, 6.dp(), 0, 4.dp())
                        setOnClickListener { if (page == "add") showAddMenu() else showPage(page) }
                    }
                bottom.addView(
                    b,
                    LinearLayout.LayoutParams(0, if (page == "add") 56.dp() else 64.dp(), 1f)
                        .apply { setMargins(4.dp(), 6.dp(), 4.dp(), 8.dp()) })
            }
    }

    private fun showAddMenu() {
        val dialog = BottomSheetDialog(this)
        val box = Ui.box(this)
        box.addView(Ui.text(this, "新建", 23f, true))
        listOf(
                "食材" to AddIngredientActivity::class.java,
                "菜谱" to EditRecipeActivity::class.java,
                "点菜" to MealEditorActivity::class.java,
                "备菜" to MealEditorActivity::class.java)
            .chunked(2)
            .forEach { pair ->
                val row = Ui.row(this)
                pair.forEach { (label, clazz) ->
                    val b =
                        Ui.tile(this, label, when(label){
                            "食材" -> R.drawable.ic_ui_produce
                            "菜谱" -> R.drawable.ic_book_outline
                            "点菜" -> R.drawable.ic_ui_order
                            else -> R.drawable.ic_ui_prep
                        }, when(label){"食材"->0xffe9f5e9.toInt();"点菜"->0xfffff1cf.toInt();"备菜"->0xffeeeafb.toInt();else->Ui.pale}) {
                            dialog.dismiss()
                            if (com.familyrecipes.android.util.AuthUtil.requireLogin(
                                this, "创建$label"))
                                startActivity(
                                    Intent(this, clazz)
                                        .putExtra("kind", if (label == "备菜") "prep" else "order"))
                        }
                    row.addView(
                        b,
                        LinearLayout.LayoutParams(0, -2, 1f).apply {
                            setMargins(4.dp(), 4.dp(), 4.dp(), 4.dp())
                        })
                }
                box.addView(row)
            }
        box.addView(
            Ui.button(this, "继续草稿") {
                dialog.dismiss()
                showPage("meals")
                supportFragmentManager.executePendingTransactions()
                (supportFragmentManager.findFragmentByTag("main:meals") as? MealFragment)
                    ?.showDrafts()
            })
        dialog.setContentView(box)
        dialog.show()
    }

    private fun updateCurrentKitchen(name: String) {
        currentKitchenName = name
        kitchenLabel.text = "$name ▾"
    }

    private fun showSwitchKitchenDialog() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.getService().getKitchens()
                check(response.isSuccessful && response.body()?.code == 200)
                val kitchens = response.body()?.data.orEmpty()
                val labels = kitchens.map { "${it.groupName ?: "群聊"} · ${it.name}" }.toTypedArray()
                android.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle("选择厨房")
                    .setItems(labels) { _, index -> selectKitchen(kitchens[index]) }
                    .setPositiveButton("新增厨房") { _, _ -> createKitchen() }
                    .setNeutralButton("重命名当前厨房") { _, _ ->
                        kitchens
                            .find { it.id == PreferenceManager.currentKitchenId }
                            ?.let { editKitchenName(it) }
                    }
                    .setNegativeButton("取消", null)
                    .show()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "厨房加载失败，请登录后重试", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun selectKitchen(kitchen: Kitchen) {
        PreferenceManager.saveCurrentKitchen(kitchen.id, kitchen.name)
        PreferenceManager.currentGroupId = kitchen.groupId
        updateCurrentKitchen(kitchen.name)
        // Recreate the visible screen, cancelling requests for the previous kitchen.
        recreate()
    }

    private fun createKitchen() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.getService().getGroupsList()
                check(response.isSuccessful && response.body()?.code == 200)
                val groups =
                    response.body()?.data.orEmpty().filter {
                        it.managerId == PreferenceManager.userId
                    }
                if (groups.isEmpty()) {
                    editKitchenName(Kitchen(name = ""))
                    return@launch
                }
                if (groups.size == 1) {
                    editKitchenName(Kitchen(groupId = groups.single().id!!, name = ""))
                    return@launch
                }
                android.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle("选择所属群组")
                    .setItems(groups.map { it.name }.toTypedArray()) { _, index ->
                        editKitchenName(Kitchen(groupId = groups[index].id!!, name = ""))
                    }
                    .show()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                Toast.makeText(this@MainActivity, "群组加载失败，请重试", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun editKitchenName(kitchen: Kitchen) {
        val field =
            android.widget.EditText(this).apply {
                setText(kitchen.name)
                hint = "例如：老家的厨房"
            }
        val dialog =
            android.app.AlertDialog.Builder(this)
                .setTitle("厨房名称（群管理员可修改）")
                .setView(field)
                .setPositiveButton("保存", null)
                .setNegativeButton("取消", null)
                .create()
        dialog.show()
        dialog.getButton(-1).setOnClickListener {
            val name = field.text.toString().trim()
            if (name.isBlank()) {
                field.error = "请输入名称"
                return@setOnClickListener
            }
            lifecycleScope.launch {
                try {
                    val response =
                        if (kitchen.id == null)
                            ApiClient.getService().createKitchen(kitchen.copy(name = name))
                        else
                            ApiClient.getService()
                                .renameKitchen(kitchen.id, kitchen.copy(name = name))
                    check(response.isSuccessful && response.body()?.code == 200)
                    dialog.dismiss()
                    selectKitchen(response.body()!!.data!!)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (_: Exception) {
                    field.error = "保存失败，请检查权限和网络后重试"
                }
            }
        }
    }

    fun initializeKitchen() {
        if (!PreferenceManager.isLoggedIn()) return
        val userId = PreferenceManager.userId
        val selectedId = PreferenceManager.currentKitchenId
        lifecycleScope.launch {
            try {
                val response = ApiClient.getService().getKitchens()
                check(response.isSuccessful && response.body()?.code == 200)
                // Ignore a stale response after switching account or kitchen.
                if (PreferenceManager.userId != userId ||
                    !PreferenceManager.isLoggedIn() ||
                    PreferenceManager.currentKitchenId != selectedId)
                    return@launch
                val kitchens = response.body()?.data.orEmpty()
                val kitchen =
                    kitchens.find { it.id == selectedId }
                        ?: kitchens.firstOrNull()
                        ?: error("没有可用厨房")
                if (kitchen.id != selectedId) selectKitchen(kitchen)
                else {
                    PreferenceManager.saveCurrentKitchen(kitchen.id, kitchen.name)
                    PreferenceManager.currentGroupId = kitchen.groupId
                    updateCurrentKitchen(kitchen.name)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                Toast.makeText(this@MainActivity, "厨房初始化失败，请点击顶部厨房入口重试", Toast.LENGTH_LONG).show()
            }
        }
    }
}
