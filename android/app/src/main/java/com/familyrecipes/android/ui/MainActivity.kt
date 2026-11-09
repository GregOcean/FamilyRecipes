package com.familyrecipes.android.ui

import com.familyrecipes.android.ui.design.UiStrings
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
import kotlinx.coroutines.*
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import com.familyrecipes.android.data.local.FriendInvitations
import com.familyrecipes.android.data.local.LocalStore

class MainActivity : AppCompatActivity() {
    private lateinit var drawer: DrawerLayout
    private lateinit var kitchenLabel: Button
    private lateinit var container: FrameLayout
    private lateinit var bottom: LinearLayout
    private var invitationCount = 0
    private var selected = "recipes"
    private var currentKitchenName = UiStrings.s(com.familyrecipes.android.R.string.ui_056b3fdd26)

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val languagePrefs=getSharedPreferences("ui-language",0)
        if(!languagePrefs.getBoolean("initialized",false)) {
            languagePrefs.edit().putBoolean("initialized",true).apply()
            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(androidx.core.os.LocaleListCompat.forLanguageTags("zh-CN"))
        }
        drawer = DrawerLayout(this)
        val shell = Ui.box(this, 0)
        val top =
            Ui.row(this).apply {
                setPadding(12, 8, 12, 8)
                setBackgroundColor(Ui.surface)
            }
        top.addView(
            Ui.button(this, "☰") { drawer.openDrawer(GravityCompat.START) }
                .apply {
                    contentDescription = UiStrings.s(com.familyrecipes.android.R.string.ui_a9e2bd537b)
                    backgroundTintList =
                        android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.TRANSPARENT)
                },
            LinearLayout.LayoutParams(64.dp(), -2))
        kitchenLabel =
            Ui.button(this, PreferenceManager.currentKitchenName ?: UiStrings.s(com.familyrecipes.android.R.string.ui_056b3fdd26)) {
                    showSwitchKitchenDialog()
                }
                .apply {
                    backgroundTintList =
                        android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.TRANSPARENT)
                }
        top.addView(kitchenLabel, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(
            com.familyrecipes.android.ui.design.UserAvatar.current(this, this).apply {
                contentDescription = UiStrings.s(R.string.ui_c32eedbf88)
                isFocusable = true
                setOnClickListener { showPage("profile") }
            },
            LinearLayout.LayoutParams(48.dp(), 48.dp()).apply { gravity = Gravity.CENTER_VERTICAL; marginEnd = 8.dp() })
        shell.addView(top)
        container = FrameLayout(this).apply { id = R.id.fragment_container }
        shell.addView(container, LinearLayout.LayoutParams(-1, 0, 1f))
        bottom = Ui.row(this).apply { setBackgroundColor(Ui.surface) }
        shell.addView(bottom)
        shell.addView(Ui.text(this,"${getString(R.string.app_version)} ${com.familyrecipes.android.BuildConfig.VERSION_NAME}",12f).apply {
            gravity=Gravity.CENTER;setTextColor(Ui.muted)
            setPadding(0,0,0,4.dp())
        })
        drawer.addView(shell, DrawerLayout.LayoutParams(-1, -1))
        val panel = Ui.box(this)
        panel.addView(Ui.text(this, UiStrings.s(com.familyrecipes.android.R.string.ui_3e90c5117d), 24f, true))
        listOf(
            getString(R.string.learning_beginner) to {startActivity(Intent(this,com.familyrecipes.android.ui.settings.LearningActivity::class.java).putExtra("section","beginner"))},
            getString(R.string.learning_advanced) to {startActivity(Intent(this,com.familyrecipes.android.ui.settings.LearningActivity::class.java).putExtra("section","advanced"))},
            getString(R.string.friends_title) to {if(com.familyrecipes.android.util.AuthUtil.requireLogin(this)) startActivity(Intent(this,com.familyrecipes.android.ui.social.FriendsListActivity::class.java))},
            UiStrings.s(com.familyrecipes.android.R.string.ui_d46695d56e) to {if(com.familyrecipes.android.util.AuthUtil.requireLogin(this)) startActivity(Intent(this,com.familyrecipes.android.ui.settings.ManagementActivity::class.java))},
            UiStrings.s(com.familyrecipes.android.R.string.ui_47fdc41702) to {if(com.familyrecipes.android.util.AuthUtil.requireLogin(this)) startActivity(Intent(this,com.familyrecipes.android.ui.settings.ManagementActivity::class.java).putExtra("groups",true))},
            UiStrings.s(com.familyrecipes.android.R.string.ui_402b79ad66) to {startActivity(Intent(this,com.familyrecipes.android.ui.social.ScanQRActivity::class.java))},
            UiStrings.s(com.familyrecipes.android.R.string.ui_e366ccf155) to {startActivity(Intent(this,com.familyrecipes.android.ui.settings.SettingsActivity::class.java))})
            .forEachIndexed { index, (label, action) ->
                panel.addView(
                    Ui.button(this, label) {
                        drawer.closeDrawers()
                        action()
                    }.apply {
                        val icons=listOf(R.drawable.ic_book_outline,R.drawable.ic_recommend,R.drawable.ic_friends,R.drawable.ic_ui_kitchen,R.drawable.ic_groups,R.drawable.ic_scan_square,R.drawable.ic_settings)
                        setIconResource(icons[index]);iconSize=24.dp();iconPadding=16.dp()
                        iconGravity=com.google.android.material.button.MaterialButton.ICON_GRAVITY_START
                        gravity=Gravity.START or Gravity.CENTER_VERTICAL
                        setPadding(16.dp(),8.dp(),16.dp(),8.dp())
                    })
            }
        drawer.addView(
            Ui.scroll(this, panel), DrawerLayout.LayoutParams(minOf(260.dp(),(resources.displayMetrics.widthPixels*0.78f).toInt()), -1, Gravity.START))
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
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val account=LocalStore.account
                launch {
                    LocalStore.instance.records().observe(account,"friend-invitations").collect {
                        val count=if(PreferenceManager.isLoggedIn() && account==LocalStore.account)
                            FriendInvitations.incoming(FriendInvitations.cached(account),PreferenceManager.userId).size else 0
                        if(invitationCount!=count){invitationCount=count;drawNavigation()}
                    }
                }
                launch {
                    while(isActive) {
                        try { FriendInvitations.sync() } catch(e:CancellationException){throw e} catch(_:Exception){}
                        delay(10_000)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getStringExtra("navigate_to") == "profile") showPage("profile")
    }

    override fun onResume(){super.onResume();if(::kitchenLabel.isInitialized)initializeKitchen()}

    private fun Int.dp() = (this * resources.displayMetrics.density).toInt()

    override fun onSaveInstanceState(out: Bundle) {
        super.onSaveInstanceState(out)
        out.putString("page", selected)
    }

    fun showPage(page: String) {
        if (page in setOf("stock", "meals", "messages") && !PreferenceManager.isLoggedIn()) {
            showPage("profile")
            return
        }
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
                Triple("recipes", UiStrings.s(com.familyrecipes.android.R.string.ui_ad237159d7), R.drawable.ic_book_outline),
                Triple("stock", UiStrings.s(com.familyrecipes.android.R.string.ui_0eac8802de), R.drawable.ic_inventory_outline),
                Triple("add", "＋", R.drawable.ic_add_circle),
                Triple("meals", UiStrings.s(com.familyrecipes.android.R.string.ui_769ef5ab61), R.drawable.ic_calendar),
                Triple("messages", UiStrings.s(com.familyrecipes.android.R.string.ui_ff692f04ac), R.drawable.ic_message))
            .forEach { (page, label, icon) ->
                val b =
                    androidx.appcompat.widget.AppCompatButton(this).apply {
                        text = if(page=="messages" && invitationCount>0) "$label ($invitationCount)" else label
                        isAllCaps = false
                        textSize = if (page == "add") 28f else 16f
                        minimumWidth = 0
                        minWidth = 0
                        setTextColor(
                            if (page == "add") Ui.onBlue
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
        box.addView(Ui.text(this, UiStrings.s(com.familyrecipes.android.R.string.ui_26bb841878), 23f, true))
        listOf(
                UiStrings.s(com.familyrecipes.android.R.string.ui_43589f00eb) to AddIngredientActivity::class.java,
                UiStrings.s(com.familyrecipes.android.R.string.ui_ad237159d7) to EditRecipeActivity::class.java,
                UiStrings.s(com.familyrecipes.android.R.string.ui_d85522beda) to MealEditorActivity::class.java,
                UiStrings.s(com.familyrecipes.android.R.string.ui_90e2746c75) to MealEditorActivity::class.java)
            .chunked(2)
            .forEach { pair ->
                val row = Ui.row(this)
                pair.forEach { (label, clazz) ->
                    val b =
                        Ui.tile(this, label, when(label){
                            UiStrings.s(com.familyrecipes.android.R.string.ui_43589f00eb) -> R.drawable.ic_ui_produce
                            UiStrings.s(com.familyrecipes.android.R.string.ui_ad237159d7) -> R.drawable.ic_book_outline
                            UiStrings.s(com.familyrecipes.android.R.string.ui_d85522beda) -> R.drawable.ic_ui_order
                            else -> R.drawable.ic_ui_prep
                        }, when(label){UiStrings.s(com.familyrecipes.android.R.string.ui_43589f00eb)->Ui.green;UiStrings.s(com.familyrecipes.android.R.string.ui_d85522beda)->Ui.warm;UiStrings.s(com.familyrecipes.android.R.string.ui_90e2746c75)->Ui.purple;else->Ui.pale}) {
                            dialog.dismiss()
                            if (com.familyrecipes.android.util.AuthUtil.requireLogin(
                                this, UiStrings.f(com.familyrecipes.android.R.string.create_label, label)))
                                startActivity(
                                    Intent(this, clazz)
                                        .putExtra("kind", if (label == UiStrings.s(com.familyrecipes.android.R.string.ui_90e2746c75)) "prep" else "order"))
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
            Ui.button(this, UiStrings.s(com.familyrecipes.android.R.string.ui_09a97f905e)) {
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
        if (!com.familyrecipes.android.util.AuthUtil.requireLogin(this)) return
        lifecycleScope.launch {
            try {
                val response = ApiClient.getService().getKitchens()
                check(response.isSuccessful && response.body()?.code == 200)
                val kitchens = response.body()?.data.orEmpty()
                val popup=androidx.appcompat.widget.ListPopupWindow(this@MainActivity)
                popup.anchorView=kitchenLabel
                popup.width=(resources.displayMetrics.widthPixels-32.dp()).coerceAtLeast(200.dp())
                val anchorLocation=IntArray(2)
                kitchenLabel.getLocationOnScreen(anchorLocation)
                popup.horizontalOffset=16.dp()-anchorLocation[0]
                popup.isModal=true
                popup.setBackgroundDrawable(android.graphics.drawable.GradientDrawable().apply {
                    setColor(Ui.surface)
                    cornerRadius=18.dp().toFloat()
                    setStroke(1.dp(), Ui.pale)
                })
                popup.verticalOffset=6.dp()
                popup.setAdapter(object : ArrayAdapter<Kitchen>(this@MainActivity, 0, kitchens) {
                    override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                        val kitchen=getItem(position)!!
                        val active=kitchen.id==PreferenceManager.currentKitchenId
                        return Ui.row(this@MainActivity).apply {
                            setPadding(14.dp(),10.dp(),14.dp(),10.dp())
                            minimumHeight=72.dp()
                            setBackgroundColor(if(active) Ui.pale else Ui.surface)
                            val icon=ImageView(context).apply {
                                setImageResource(R.drawable.ic_ui_kitchen)
                                imageTintList=android.content.res.ColorStateList.valueOf(Ui.blue)
                                setPadding(8.dp(),8.dp(),8.dp(),8.dp())
                                background=android.graphics.drawable.GradientDrawable().apply {
                                    setColor(if(active) Ui.surface else Ui.pale)
                                    cornerRadius=12.dp().toFloat()
                                }
                            }
                            addView(icon, LinearLayout.LayoutParams(40.dp(),40.dp()))
                            val words=Ui.box(context,0).apply {
                                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                setPadding(12.dp(),0,8.dp(),0)
                                addView(Ui.text(context,kitchen.name,18f,true).apply {setPadding(0,0,0,2.dp());setTextColor(Ui.blue)})
                                addView(Ui.text(context,kitchen.groupName.orEmpty(),14f).apply {setPadding(0,0,0,0);setTextColor(Ui.muted)})
                            }
                            addView(words,LinearLayout.LayoutParams(0,-2,1f))
                            if(active) addView(Ui.text(context,"✓",22f,true).apply {setTextColor(Ui.blue)})
                        }
                    }
                })
                popup.setOnItemClickListener{_,_,index,_->popup.dismiss();if(kitchens[index].id!=PreferenceManager.currentKitchenId)selectKitchen(kitchens[index])}
                popup.show()
                popup.listView?.apply {
                    background=android.graphics.drawable.GradientDrawable().apply {
                        setColor(Ui.surface)
                        cornerRadius=18.dp().toFloat()
                    }
                    clipToOutline=true
                }

            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, UiStrings.s(com.familyrecipes.android.R.string.ui_6610d96cd4), Toast.LENGTH_SHORT).show()
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
                        ?: error(UiStrings.s(com.familyrecipes.android.R.string.ui_d2ff29c1f6))
                if (kitchen.id != selectedId) selectKitchen(kitchen)
                else {
                    PreferenceManager.saveCurrentKitchen(kitchen.id, kitchen.name)
                    PreferenceManager.currentGroupId = kitchen.groupId
                    updateCurrentKitchen(kitchen.name)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                Toast.makeText(this@MainActivity, UiStrings.s(com.familyrecipes.android.R.string.ui_6f3664cf20), Toast.LENGTH_LONG).show()
            }
        }
    }
}
