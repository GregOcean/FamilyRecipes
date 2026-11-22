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
import kotlinx.coroutines.flow.collectLatest
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import com.familyrecipes.android.data.local.FriendInvitations
import com.familyrecipes.android.data.local.LocalStore

class MainActivity : AppCompatActivity() {
    private lateinit var drawer: DrawerLayout
    private lateinit var recipeSearch: com.google.android.material.button.MaterialButton
    private lateinit var inventorySearch: EditText
    private lateinit var mealOrderTab: com.google.android.material.button.MaterialButton
    private lateinit var mealPrepTab: com.google.android.material.button.MaterialButton
    private lateinit var kitchenLabel: Button
    private lateinit var container: FrameLayout
    private lateinit var bottom: LinearLayout
    private lateinit var avatar: FrameLayout
    private var identityId = PreferenceManager.userId
    private val screenAccount = PreferenceManager.userId
    private var invitationCount = 0
    private var pendingPage: String? = null
    private var pendingLaunch: Intent? = null
    private var pendingNeedsKitchen = false
    private var kitchenJob: Job? = null
    private var selected = "recipes"
    private var mealTab = "order"
    private var currentKitchenName = UiStrings.s(com.familyrecipes.android.R.string.ui_056b3fdd26)

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        pendingPage = state?.getString("pending_page")
        pendingLaunch = state?.getParcelable("pending_launch")
        pendingNeedsKitchen = state?.getBoolean("pending_kitchen") ?: false
        mealTab = if (state?.getString("meal_tab") == "prep") "prep" else "order"
        drawer = DrawerLayout(this)
        val shell = Ui.box(this, 0)
        val top =
            Ui.row(this).apply {
                setPadding(8.dp(), 2.dp(), 8.dp(), 2.dp())
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
            LinearLayout.LayoutParams(48.dp(), 48.dp()))
        kitchenLabel =
            Ui.button(this, PreferenceManager.currentKitchenName ?: UiStrings.s(com.familyrecipes.android.R.string.ui_056b3fdd26)) {
                    showSwitchKitchenDialog()
                }
                .apply {
                    backgroundTintList =
                        android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.TRANSPARENT)
                }
        kitchenLabel.apply {maxLines=1;ellipsize=android.text.TextUtils.TruncateAt.END;textSize=14f}
        top.addView(kitchenLabel, LinearLayout.LayoutParams(96.dp(), -2))
        recipeSearch=Ui.button(this,getString(R.string.recipe_search_short)){openRecipeSearch()}.apply {
            maxLines=1;ellipsize=android.text.TextUtils.TruncateAt.END;textSize=14f
            setIconResource(R.drawable.ic_search);iconSize=18.dp();iconPadding=4.dp()
            iconTint=android.content.res.ColorStateList.valueOf(Ui.blue)
            setPadding(6.dp(),0,6.dp(),0)
            gravity=Gravity.START or Gravity.CENTER_VERTICAL
        }
        top.addView(recipeSearch,LinearLayout.LayoutParams(0,-2,1f).apply{marginEnd=4.dp()})
        inventorySearch = EditText(this).apply {
            hint = getString(R.string.ui_93e4998872)
            textSize = 14f
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            background = androidx.core.content.ContextCompat.getDrawable(this@MainActivity, R.drawable.search_background)
            setPadding(12.dp(), 8.dp(), 12.dp(), 8.dp())
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
            visibility = android.view.View.GONE
            val icon = androidx.core.content.ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_search)
            setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null)
            compoundDrawablePadding = 8.dp()
            androidx.core.widget.TextViewCompat.setCompoundDrawableTintList(
                this, android.content.res.ColorStateList.valueOf(Ui.blue))
        }
        inventorySearch.setOnEditorActionListener { _, action, _ ->
            if (action == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                true
            } else false
        }
        top.addView(inventorySearch, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = 4.dp() })
        inventorySearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val query = s?.toString()?.trim() ?: ""
                val fridge = supportFragmentManager.findFragmentByTag("main:stock") as? FridgeFragment
                fridge?.filterByQuery(query)
            }
        })
        mealOrderTab = Ui.button(this, UiStrings.s(com.familyrecipes.android.R.string.ui_d85522beda)) { switchMealTab("order") }.apply {
            isAllCaps=false;textSize=13f;minWidth=0;minimumWidth=0;minHeight=0;minimumHeight=0
            setPadding(10.dp(),0,10.dp(),0)
            visibility=android.view.View.GONE
        }
        mealPrepTab = Ui.button(this, UiStrings.s(com.familyrecipes.android.R.string.ui_90e2746c75)) { switchMealTab("prep") }.apply {
            isAllCaps=false;textSize=13f;minWidth=0;minimumWidth=0;minHeight=0;minimumHeight=0
            setPadding(10.dp(),0,10.dp(),0)
            visibility=android.view.View.GONE
        }
        top.addView(mealOrderTab, LinearLayout.LayoutParams(0, 36.dp(), 1f).apply { gravity=Gravity.CENTER_VERTICAL; marginEnd=2.dp() })
        top.addView(mealPrepTab, LinearLayout.LayoutParams(0, 36.dp(), 1f).apply { gravity=Gravity.CENTER_VERTICAL; marginEnd=8.dp() })
        avatar=FrameLayout(this).apply {
            contentDescription=getString(R.string.ui_c32eedbf88)
            isFocusable=true
            setOnClickListener {showPage("profile")}
        }
        refreshAvatar()
        top.addView(avatar,LinearLayout.LayoutParams(42.dp(),42.dp()).apply {gravity=Gravity.CENTER_VERTICAL;marginEnd=8.dp()})
        shell.addView(top)
        container = FrameLayout(this).apply { id = R.id.fragment_container }
        shell.addView(container, LinearLayout.LayoutParams(-1, 0, 1f))
        bottom = Ui.row(this).apply { setBackgroundColor(Ui.surface) }
        shell.addView(bottom)
        drawer.addView(shell, DrawerLayout.LayoutParams(-1, -1))
        val panel = Ui.box(this,12)
        panel.addView(Ui.text(this, UiStrings.s(com.familyrecipes.android.R.string.ui_3e90c5117d), 21f, true))
        listOf(
            getString(R.string.learning_beginner) to {startActivity(Intent(this,com.familyrecipes.android.ui.settings.LearningActivity::class.java).putExtra("section","beginner"))},
            getString(R.string.learning_advanced) to {startActivity(Intent(this,com.familyrecipes.android.ui.settings.LearningActivity::class.java).putExtra("section","advanced"))},
            getString(R.string.friends_title) to {openAuthorized(Intent(this,com.familyrecipes.android.ui.social.FriendsListActivity::class.java))},
            UiStrings.s(com.familyrecipes.android.R.string.ui_d46695d56e) to {openAuthorized(Intent(this,com.familyrecipes.android.ui.settings.ManagementActivity::class.java))},
            UiStrings.s(com.familyrecipes.android.R.string.ui_47fdc41702) to {openAuthorized(Intent(this,com.familyrecipes.android.ui.settings.ManagementActivity::class.java).putExtra("groups",true))},
            UiStrings.s(com.familyrecipes.android.R.string.ui_402b79ad66) to {showScanMenu()},
            UiStrings.s(com.familyrecipes.android.R.string.ui_e366ccf155) to {startActivity(Intent(this,com.familyrecipes.android.ui.settings.SettingsActivity::class.java))})
            .forEachIndexed { index, (label, action) ->
                panel.addView(
                    Ui.button(this, label) {
                        drawer.closeDrawers()
                        action()
                    }.apply {
                        val icons=listOf(R.drawable.ic_book_outline,R.drawable.ic_recommend,R.drawable.ic_friends,R.drawable.ic_ui_kitchen,R.drawable.ic_groups,R.drawable.ic_scan_square,R.drawable.ic_settings)
                        layoutParams=LinearLayout.LayoutParams(-1,-2)
                        setIconResource(icons[index]);iconSize=20.dp();iconPadding=12.dp()
                        iconGravity=com.google.android.material.button.MaterialButton.ICON_GRAVITY_START
                        gravity=Gravity.START or Gravity.CENTER_VERTICAL
                        setPadding(12.dp(),2.dp(),12.dp(),2.dp())
                    })
            }
        drawer.addView(
            Ui.scroll(this, panel), DrawerLayout.LayoutParams(minOf(220.dp(),(resources.displayMetrics.widthPixels*0.68f).toInt()), -1, Gravity.START))
        setContentView(drawer)
        selected =
            state?.getString("page")
                ?: when (intent.getStringExtra("navigate_to")) {
                    "fridge" -> "stock"
                    "message" -> "messages"
                    "profile" -> "profile"
                    else -> "recipes"
                }
        updateHeaderLayout()
        drawNavigation()
        val accountChanged=state!=null && state.getLong("screen_account",screenAccount)!=screenAccount
        if(accountChanged) {
            val clear=supportFragmentManager.beginTransaction()
            supportFragmentManager.fragments.forEach {clear.remove(it)}
            clear.commitNow()
        }
        if (state == null || accountChanged) showPage(selected)
        applySearchIntent(intent)
        initializeKitchen()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    PreferenceManager.sessionRevision.collectLatest {
                        if(identityId!=PreferenceManager.userId) {
                            val previous=identityId
                            identityId=PreferenceManager.userId
                            if(previous>0) {
                                // Dispose account-specific fragments and their in-flight requests.
                                if(!PreferenceManager.isLoggedIn()) {
                                    selected="profile"
                                    Toast.makeText(this@MainActivity,getString(R.string.session_expired),Toast.LENGTH_LONG).show()
                                }
                                recreate()
                                return@collectLatest
                            }
                            refreshAvatar()
                        }
                        val account=LocalStore.account
                        kotlinx.coroutines.flow.combine(
                            LocalStore.instance.records().observe(account,"friend-invitations"),
                            LocalStore.instance.records().observe(account,"favorite-notifications"),
                            com.familyrecipes.android.data.local.GroupCache.observe(account)
                        ) { a,b,groups -> Triple(a,b,groups) }.collect { (a,b,groups) ->
                            val count=if(PreferenceManager.isLoggedIn() && account==LocalStore.account)
                                FriendInvitations.incoming(FriendInvitations.cached(account),PreferenceManager.userId).size +
                                com.familyrecipes.android.data.local.FavoriteNotifications.cached(account).unread +
                                groups.sumOf { it.unreadCount ?: 0 } else 0
                            if(invitationCount!=count){invitationCount=count;drawNavigation()}
                        }
                    }
                }
                launch {
                    var wasLoggedIn=PreferenceManager.isLoggedIn()
                    while(isActive) {
                        wasLoggedIn=PreferenceManager.isLoggedIn()
                        if(wasLoggedIn) {
                            try { com.familyrecipes.android.data.local.ConversationSync.refresh() } catch(e:CancellationException){throw e} catch(_:Exception){}
                            try { com.familyrecipes.android.data.local.FavoriteNotifications.sync() } catch(e:CancellationException){throw e} catch(_:Exception){}
                            try { FriendInvitations.sync() } catch(e:CancellationException){throw e} catch(_:Exception){}
                        }
                        delay(5_000)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        when(intent.getStringExtra("navigate_to")) {
            "profile" -> showPage("profile")
            "message" -> showPage("messages")
            "fridge" -> showPage("stock")
            "recipes" -> showPage("recipes")
        }
        applySearchIntent(intent)
    }

    /** 从标签等处打开时，直接按搜索框同样的方式展示菜谱搜索结果。 */
    private fun applySearchIntent(intent: Intent?) {
        val query = intent?.getStringExtra("search_keyword")?.trim().orEmpty()
        if (query.isEmpty()) return
        intent?.removeExtra("search_keyword")
        showPage("recipes")
        supportFragmentManager.executePendingTransactions()
        (supportFragmentManager.findFragmentByTag("main:recipes") as? RecipeListFragment)
            ?.performSearch(query)
        setRecipeSearchQuery(query)
    }

    override fun onResume(){super.onResume();if(::kitchenLabel.isInitialized)initializeKitchen()}

    private fun Int.dp() = (this * resources.displayMetrics.density).toInt()

    override fun onSaveInstanceState(out: Bundle) {
        super.onSaveInstanceState(out)
        out.putString("page", selected)
        out.putLong("screen_account",screenAccount)
        out.putString("pending_page", pendingPage)
        out.putParcelable("pending_launch", pendingLaunch)
        out.putBoolean("pending_kitchen", pendingNeedsKitchen)
        out.putString("meal_tab", mealTab)
    }

    fun showPage(page: String) {
        if (page in setOf("stock", "meals", "messages") && !PreferenceManager.isLoggedIn()) {
            pendingPage = page
            showPage("profile")
            return
        }
        selected = page
        updateHeaderLayout()
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
        supportFragmentManager.fragments.forEach { transaction.hide(it);transaction.setMaxLifecycle(it,Lifecycle.State.STARTED) }
        val found = supportFragmentManager.findFragmentByTag(tag)
        if (found == null) transaction.add(container.id, fragment, tag) else transaction.show(found)
        transaction.setMaxLifecycle(found ?: fragment,Lifecycle.State.RESUMED)
        transaction.commit()
        if(page=="meals") {
            supportFragmentManager.executePendingTransactions()
            (supportFragmentManager.findFragmentByTag(tag) as? MealFragment)?.showTab(mealTab)
        }
        if(page=="recipes")setRecipeSearchQuery((found as? RecipeListFragment)?.currentQuery().orEmpty())
        if(page=="stock") {
            supportFragmentManager.executePendingTransactions()
            val fridge = supportFragmentManager.findFragmentByTag(tag) as? FridgeFragment
            inventorySearch.setText(fridge?.externalQueryText().orEmpty())
        } else hideKeyboard()
        drawNavigation()
    }

    private fun hideKeyboard() {
        val input = currentFocus ?: return
        val manager = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        manager.hideSoftInputFromWindow(input.windowToken, 0)
        input.clearFocus()
    }

    private fun updateHeaderLayout() {
        recipeSearch.visibility=if(selected=="recipes")android.view.View.VISIBLE else android.view.View.GONE
        inventorySearch.visibility=if(selected=="stock")android.view.View.VISIBLE else android.view.View.GONE
        val showMealTabs = selected == "meals"
        mealOrderTab.visibility=if(showMealTabs)android.view.View.VISIBLE else android.view.View.GONE
        mealPrepTab.visibility=if(showMealTabs)android.view.View.VISIBLE else android.view.View.GONE
        // Show kitchen label for both recipes and stock pages
        val showKitchen = selected=="recipes" || selected=="stock"
        kitchenLabel.visibility = if(showKitchen) android.view.View.VISIBLE else android.view.View.INVISIBLE
        kitchenLabel.layoutParams=LinearLayout.LayoutParams(if(showKitchen)96.dp() else 0,-2,0f)
        refreshMealTabStyle()
    }

    /** 点菜/备菜由顶部按钮和页面左右滑动共同控制。 */
    fun switchMealTab(value: String) {
        mealTab = if (value == "prep") "prep" else "order"
        refreshMealTabStyle()
        supportFragmentManager.executePendingTransactions()
        (supportFragmentManager.findFragmentByTag("main:meals") as? MealFragment)?.showTab(mealTab)
    }

    private fun refreshMealTabStyle() {
        if (!::mealOrderTab.isInitialized) return
        listOf(mealOrderTab to "order", mealPrepTab to "prep").forEach { (button, key) ->
            val active = key == mealTab
            button.setTextColor(if (active) Ui.blue else Ui.muted)
            button.backgroundTintList = android.content.res.ColorStateList.valueOf(if (active) Ui.pale else android.graphics.Color.TRANSPARENT)
            button.strokeWidth = if (active) 1.dp() else 0
            button.strokeColor = android.content.res.ColorStateList.valueOf(Ui.blue)
        }
    }

    fun setRecipeSearchQuery(query:String) {
        recipeSearch.text=query.ifBlank{getString(R.string.recipe_search_short)}
        recipeSearch.contentDescription=getString(R.string.recipe_search_short)+if(query.isBlank())"" else ": $query"
    }

    private fun openRecipeSearch() {
        supportFragmentManager.executePendingTransactions()
        val recipes=supportFragmentManager.findFragmentByTag("main:recipes") as? RecipeListFragment ?: return
        val input=Ui.field(this,getString(R.string.recipe_search_short),recipes.currentQuery()).apply {
            setSingleLine(true);imeOptions=android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
            filters=arrayOf(android.text.InputFilter.LengthFilter(100))
            selectAll()
        }
        val box=Ui.box(this,16).apply{addView(input)}
        val dialog=androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.recipe_search_short).setView(box)
            .setPositiveButton(R.string.recipe_search_short){_,_->recipes.performSearch(input.text.toString().trim())}
            .setNeutralButton(R.string.recipe_search_clear){_,_->recipes.performSearch("")}
            .setNegativeButton(android.R.string.cancel,null).create()
        input.setOnEditorActionListener {_,action,_->
            if(action==android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH){recipes.performSearch(input.text.toString().trim());dialog.dismiss();true}else false
        }
        dialog.setOnShowListener{input.requestFocus();dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)}
        dialog.show()
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
                        text = if(page=="messages" && invitationCount>0) "$label · ${if(invitationCount>99) "99+" else invitationCount}" else label
                        isAllCaps = false
                        textSize = if (page == "add") 28f else 14f
                        minimumWidth = 0
                        minWidth = 0
                        minimumHeight = 52.dp()
                        minHeight = 52.dp()
                        includeFontPadding = false
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
                            d.setBounds(0, 0, 24.dp(), 24.dp())
                            setCompoundDrawables(null, d, null, null)
                        }
                        setPadding(0, 2.dp(), 0, 2.dp())
                        setOnClickListener { if (page == "add") showAddMenu() else showPage(page) }
                    }
                bottom.addView(
                    b,
                    LinearLayout.LayoutParams(0, -2, 1f)
                        .apply { setMargins(4.dp(), 2.dp(), 4.dp(), 2.dp()) })
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
                            openAuthorized(Intent(this,clazz).putExtra("kind",
                                if(label==UiStrings.s(com.familyrecipes.android.R.string.ui_90e2746c75)) "prep" else "order"),true)
                        }
                    row.addView(
                        b,
                        LinearLayout.LayoutParams(0, -2, 1f).apply {
                            setMargins(4.dp(), 4.dp(), 4.dp(), 4.dp())
                        })
                }
                box.addView(row)
            }
        dialog.setContentView(box)
        dialog.show()
    }

    fun refreshAvatar() {
        avatar.removeAllViews()
        avatar.addView(com.familyrecipes.android.ui.design.UserAvatar.current(this,this),FrameLayout.LayoutParams(-1,-1))
    }

    private fun updateCurrentKitchen(name: String) {
        currentKitchenName = name
        kitchenLabel.text = "$name ▾"
        kitchenLabel.contentDescription=name
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

    fun openAuthorized(target:Intent, needsKitchen:Boolean=false) {
        pendingLaunch=target;pendingNeedsKitchen=needsKitchen
        if(!PreferenceManager.isLoggedIn()){showPage("profile");return}
        if(needsKitchen && PreferenceManager.currentKitchenId==null) initializeKitchen() else continuePendingAction()
    }

    fun onAuthenticated() {
        if(intent.getBooleanExtra("return_to_caller",false)){finish();return}
        if(PreferenceManager.currentKitchenId==null) initializeKitchen() else continuePendingAction()
    }

    private fun continuePendingAction() {
        if(!PreferenceManager.isLoggedIn())return
        if(pendingNeedsKitchen && PreferenceManager.currentKitchenId==null)return
        pendingLaunch?.let { target -> pendingLaunch=null;pendingNeedsKitchen=false;startActivity(target);return }
        pendingPage?.let { page -> pendingPage=null;showPage(page) }
    }

    private fun showScanMenu() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(R.string.scan_choose_title)
            .setItems(arrayOf(getString(R.string.scan_friend),getString(R.string.scan_receipt),getString(R.string.scan_food))) { _,which ->
                if(which==0)openAuthorized(Intent(this,com.familyrecipes.android.ui.social.ScanQRActivity::class.java))
                else openAuthorized(Intent(this,ReceiptActivity::class.java).putExtra("mode",if(which==1) "receipt" else "item"),true)
            }.show()
    }

    fun initializeKitchen() {
        if (!PreferenceManager.isLoggedIn() || kitchenJob?.isActive == true) return
        val userId = PreferenceManager.userId
        val selectedId = PreferenceManager.currentKitchenId
        kitchenJob = lifecycleScope.launch {
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
                if (kitchen.id != selectedId && selectedId != null) selectKitchen(kitchen)
                else {
                    PreferenceManager.saveCurrentKitchen(kitchen.id, kitchen.name)
                    PreferenceManager.currentGroupId = kitchen.groupId
                    updateCurrentKitchen(kitchen.name)
                    continuePendingAction()
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                if(pendingLaunch!=null || pendingPage!=null) {
                    com.google.android.material.dialog.MaterialAlertDialogBuilder(this@MainActivity)
                        .setMessage(R.string.kitchen_retry_message).setPositiveButton(R.string.ui_729a79fdf0){_,_-> kitchenJob=null;initializeKitchen()}
                        .setNegativeButton(android.R.string.cancel,null).show()
                }
            }
        }
    }
}
