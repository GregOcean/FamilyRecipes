package com.familyrecipes.android.ui.recipe

import androidx.core.widget.doAfterTextChanged
import com.familyrecipes.android.data.local.*
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.familyrecipes.android.R
import com.familyrecipes.android.data.model.*
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.databinding.ActivityEditRecipeBinding
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

/**
 * 添加/编辑菜谱页面
 */
class EditRecipeActivity : AppCompatActivity() {

    private lateinit var ingredientField:EditText
    private lateinit var timeField:EditText
    private lateinit var tasteField:EditText
    private lateinit var stepBox:android.widget.LinearLayout
    private val stepDrafts=mutableListOf<StepDraft>()
    private data class StepDraft(var text:String="",var image:String?=null)
    private var pendingStep:Int?=null
    private fun setupStructuredEditor() {
        val parent=binding.etRecipeContent.parent.parent.parent as android.widget.LinearLayout
        val box=com.familyrecipes.android.ui.design.Ui.box(this,0)
        box.addView(com.familyrecipes.android.ui.design.Ui.text(this,"食材与用量",20f,true))
        ingredientField=com.familyrecipes.android.ui.design.Ui.field(this,"每行：食材名称 | 用量，例如 鸡蛋 | 2个",multi=true);box.addView(ingredientField)
        timeField=com.familyrecipes.android.ui.design.Ui.field(this,"耗时（分钟，可选）");timeField.inputType=android.text.InputType.TYPE_CLASS_NUMBER;box.addView(timeField)
        tasteField=com.familyrecipes.android.ui.design.Ui.field(this,"口味（酸、辣、清淡、鲜香等，用顿号分隔）");box.addView(tasteField)
        box.addView(com.familyrecipes.android.ui.design.Ui.text(this,"做法步骤",20f,true));stepBox=com.familyrecipes.android.ui.design.Ui.box(this,0);box.addView(stepBox)
        box.addView(com.familyrecipes.android.ui.design.Ui.button(this,"＋ 添加步骤"){stepDrafts.add(StepDraft());drawSteps();saveDraft()})
        parent.addView(box,parent.indexOfChild(binding.etRecipeContent.parent.parent as View)+1)
        listOf(ingredientField,timeField,tasteField).forEach{it.doAfterTextChanged{saveDraft()}}
    }
    private fun drawSteps(){
        stepBox.removeAllViews()
        stepDrafts.forEachIndexed{index,step->
            val box=com.familyrecipes.android.ui.design.Ui.card(this)
            box.addView(com.familyrecipes.android.ui.design.Ui.text(this,"第 ${index+1} 步",18f,true))
            val field=com.familyrecipes.android.ui.design.Ui.field(this,"写下这一步的做法",step.text,true);box.addView(field);field.doAfterTextChanged{step.text=it.toString();saveDraft()}
            step.image?.let{url->val preview=ImageView(this).apply{adjustViewBounds=true};box.addView(preview,android.widget.LinearLayout.LayoutParams(-1,180));com.bumptech.glide.Glide.with(this).load(com.familyrecipes.android.util.MediaImages.model(url)).into(preview)}
            box.addView(com.familyrecipes.android.ui.design.Ui.button(this,if(step.image==null)"添加步骤图片" else "更换步骤图片"){pendingStep=index;pickImageLauncher.launch("image/*")})
            val actions=com.familyrecipes.android.ui.design.Ui.row(this)
            actions.addView(com.familyrecipes.android.ui.design.Ui.button(this,"上移"){if(index>0){java.util.Collections.swap(stepDrafts,index,index-1);drawSteps();saveDraft()}}.apply{isEnabled=index>0})
            actions.addView(com.familyrecipes.android.ui.design.Ui.button(this,"删除步骤"){stepDrafts.removeAt(index);drawSteps();saveDraft()})
            box.addView(actions);stepBox.addView(box)
        }
    }
    private suspend fun prepareIngredients():List<RecipeIngredient> {
        return ingredientField.text.toString().lines().map{it.trim()}.filter{it.isNotBlank()}.map{line->
            val parts=line.split("|",limit=2);val ingredient=com.familyrecipes.android.ui.design.Ui.data(ApiClient.getService().createIngredient(Ingredient(null,parts[0].trim(),null)))
            RecipeIngredient(null,null,ingredient.id,parts.getOrNull(1)?.trim(),true,ingredient)
        }.distinctBy{it.ingredientId}
    }
    private fun preparedSteps()=stepDrafts.mapIndexed{index,s->CookingStep(null,null,index+1,s.text,s.image,null)}
    private fun tasteTags()=tasteField.text.toString().split(Regex("[、，, ]")).filter{it.isNotBlank()}.distinct().map{RecipeTag(null,null,"taste",it)}

    private lateinit var binding: ActivityEditRecipeBinding
    private val selectedImages = mutableListOf<Uri>()
    private val externalLinks = mutableListOf<ParsedExternalLink>()  // 改为结构化数据
    private lateinit var imageAdapter: ImageGridAdapter
    private lateinit var linkAdapter: ExternalLinkAdapter

    private val account = LocalStore.account
    private val kitchenId = PreferenceManager.currentKitchenId
    private var saved = false
    private var draftReady = false
    private val photoMeta = mutableMapOf<String, RecipeImage>()
    private data class EditorDraft(val title: String, val content: String, val tags: String,
        val images: List<String>, val metadata: Map<String, RecipeImage>, val links: List<ParsedExternalLink>,
        val visibility: String, val groups: List<Long>, val original: Recipe?, val ingredientText:String?=null,val timeText:String?=null,val tasteText:String?=null,val stepData:List<StepDraft>?=null)
    private val draftKey get() = intent.getStringExtra("draft_key") ?: "recipe:${recipeId ?: "new"}:${kitchenId ?: "none"}"
    private var recipeId: Long? = null  // 如果是编辑模式，这里保存菜谱ID
    private var existingRecipe: Recipe? = null  // 保存现有菜谱数据

    // 权限相关
    private var selectedVisibility: String = "private" // public, private, group
    private val selectedGroupIds = mutableListOf<Long>() // 分享的群组ID列表

    /**
     * 解析后的外部链接数据
     */
    data class ParsedExternalLink(
        val title: String,
        val url: String,
        val source: String,
        val thumbnail: String?
    )

    // 选择图片的启动器
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) lifecycleScope.launch {
            try {
                for(uri in uris.take(if(pendingStep!=null)1 else 30-selectedImages.size)) {
                    val local = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        val dir=File(filesDir,"draft-images").apply { mkdirs() }
                        val file=File(dir,java.util.UUID.randomUUID().toString())
                        contentResolver.openInputStream(uri)!!.use { input -> file.outputStream().use { input.copyTo(it) } }
                        Uri.fromFile(file)
                    }
                    selectedImages.add(local)
                    photoMeta[local.toString()]=RecipeImage(local.toString(),if(pendingStep!=null)"step" else "screenshot")
                    pendingStep?.let{stepDrafts.getOrNull(it)?.image=local.toString()}
                    saveDraft()
                }
                pendingStep=null;drawSteps()
                imageAdapter.notifyDataSetChanged()
            } catch(e: kotlinx.coroutines.CancellationException) { throw e }
              catch(_: Exception) { android.widget.Toast.makeText(this@EditRecipeActivity,"图片读取失败，请重新选择",android.widget.Toast.LENGTH_SHORT).show() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditRecipeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 检查是否是编辑模式
        recipeId = intent.getLongExtra("recipe_id", -1L).takeIf { it > 0 }

        setupStructuredEditor()
        setupToolbar()
        setupImageGrid()
        setupExternalLinks()
        setupPermission()
        setupPublishButton()

        listOf(binding.etRecipeTitle,binding.etRecipeContent,binding.etTags).forEach { field ->
            field.doAfterTextChanged { saveDraft() }
        }
        (binding.etRecipeContent.parent.parent as com.google.android.material.textfield.TextInputLayout).hint = "简介与笔记（可选）"
        binding.etRecipeContent.hint=null
        binding.btnPublish.text=if(recipeId==null)"保存菜谱" else "保存"
        setEditingEnabled(false)
        lifecycleScope.launch {
            val stored=LocalStore.instance.records().get(account,"draft",draftKey)
            if(stored!=null) {
                try { restoreDraft(com.google.gson.Gson().fromJson(stored,EditorDraft::class.java)) }
                catch(_: Exception) { android.widget.Toast.makeText(this@EditRecipeActivity,"草稿读取失败",android.widget.Toast.LENGTH_SHORT).show() }
                draftReady=true
                setEditingEnabled(true)
            } else if(recipeId!=null) {
                binding.btnPublish.isEnabled=false
                binding.etRecipeTitle.isEnabled=false;binding.etRecipeContent.isEnabled=false;binding.etTags.isEnabled=false
                loadExistingRecipe(recipeId!!)
            } else { draftReady=true;setEditingEnabled(true) }
        }
        if(recipeId!=null) { binding.toolbar.title="编辑收藏";binding.btnPublish.text="保存" }
    }
    private fun setEditingEnabled(enabled:Boolean) {
        binding.etRecipeTitle.isEnabled=enabled;binding.etRecipeContent.isEnabled=enabled;binding.etTags.isEnabled=enabled
        binding.btnPublish.isEnabled=enabled;binding.btnAddExternalLink.isEnabled=enabled;binding.layoutPermission.isEnabled=enabled
        fun enableTree(view:View) {
            view.isEnabled=enabled
            if(view is android.view.ViewGroup) for(i in 0 until view.childCount) enableTree(view.getChildAt(i))
        }
        enableTree(binding.root)
        enableTree(stepBox);ingredientField.isEnabled=enabled;timeField.isEnabled=enabled;tasteField.isEnabled=enabled
        enableTree(binding.recyclerImages)
        enableTree(binding.recyclerExternalLinks)
    }
    private fun saveDraft() {
        if(!draftReady || saved) return
        if(recipeId==null && binding.etRecipeTitle.text.isNullOrBlank() && binding.etRecipeContent.text.isNullOrBlank() && selectedImages.isEmpty() && externalLinks.isEmpty() && ingredientField.text.isBlank() && stepDrafts.isEmpty())return
        val draft=EditorDraft(binding.etRecipeTitle.text.toString(),binding.etRecipeContent.text.toString(),
            binding.etTags.text.toString(),selectedImages.map { it.toString() },photoMeta.toMap(),externalLinks.toList(),
            selectedVisibility,selectedGroupIds.toList(),existingRecipe,ingredientField.text.toString(),timeField.text.toString(),tasteField.text.toString(),stepDrafts.toList())
        DraftWrites.put(account,draftKey,com.google.gson.Gson().toJson(draft))
    }
    private fun restoreDraft(draft: EditorDraft) {
        binding.etRecipeTitle.setText(draft.title);binding.etRecipeContent.setText(draft.content);binding.etTags.setText(draft.tags)
        ingredientField.setText(draft.ingredientText);timeField.setText(draft.timeText);tasteField.setText(draft.tasteText);stepDrafts.clear();stepDrafts.addAll(draft.stepData.orEmpty());drawSteps()
        selectedImages.clear();selectedImages.addAll(draft.images.map(Uri::parse))
        photoMeta.clear();photoMeta.putAll(draft.metadata)
        externalLinks.clear();externalLinks.addAll(draft.links)
        selectedVisibility=draft.visibility;selectedGroupIds.clear();selectedGroupIds.addAll(draft.groups)
        existingRecipe=draft.original;imageAdapter.notifyDataSetChanged();linkAdapter.notifyDataSetChanged();updatePermissionDisplay()
    }
    override fun onStop() { saveDraft();super.onStop() }
    private fun editImageMetadata(index: Int) {
        val key=selectedImages[index].toString()
        val current=photoMeta[key] ?: RecipeImage(key)
        val categories=arrayOf("cover","step","screenshot","cooking_record","other")
        val labels=arrayOf("封面","步骤图","教程截图","烹饪记录","其他")
        val box=android.widget.LinearLayout(this).apply { orientation=android.widget.LinearLayout.VERTICAL }
        val caption=EditText(this).apply { hint="图片说明 / 步骤文字";setText(current.caption) }
        val spinner=android.widget.Spinner(this).apply {
            adapter=android.widget.ArrayAdapter(this@EditRecipeActivity,android.R.layout.simple_spinner_dropdown_item,labels)
            setSelection(categories.indexOf(current.category).coerceAtLeast(0))
        }
        box.addView(spinner);box.addView(caption)
        AlertDialog.Builder(this).setTitle("图片分类与说明").setView(box).setPositiveButton("保存") { _,_ ->
            photoMeta[key]=current.copy(category=categories[spinner.selectedItemPosition],caption=caption.text.toString())
            saveDraft()
        }.setNegativeButton("取消",null).show()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupImageGrid() {
        imageAdapter = ImageGridAdapter(selectedImages, { saveDraft() }, { if(draftReady && binding.btnPublish.isEnabled) editImageMetadata(it) }) {
            if(draftReady && binding.btnPublish.isEnabled) pickImageLauncher.launch("image/*")
        }

        binding.recyclerImages.apply {
            layoutManager = GridLayoutManager(this@EditRecipeActivity, 3)
            adapter = imageAdapter
        }
    }

    private fun setupExternalLinks() {
        linkAdapter = ExternalLinkAdapter(externalLinks) { position ->
            // 删除链接
            externalLinks.removeAt(position)
            saveDraft()
            linkAdapter.notifyItemRemoved(position)
        }

        binding.recyclerExternalLinks.apply {
            layoutManager = LinearLayoutManager(this@EditRecipeActivity)
            adapter = linkAdapter
        }

        // 添加外部链接按钮
        binding.btnAddExternalLink.setOnClickListener {
            showAddLinkDialog()
        }
    }

    private fun showAddLinkDialog() {
        if(externalLinks.isNotEmpty()) {
            android.widget.Toast.makeText(this,"每篇收藏保留一个来源，请先移除原链接，或新建另一篇收藏",android.widget.Toast.LENGTH_LONG).show(); return
        }
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_link, null)
        val etLinkUrl = dialogView.findViewById<EditText>(R.id.et_link_url)
        val progressParsing = dialogView.findViewById<ProgressBar>(R.id.progress_parsing)
        val tvParsingStatus = dialogView.findViewById<TextView>(R.id.tv_parsing_status)

        val dialog = AlertDialog.Builder(this)
            .setTitle("添加外部链接")
            .setView(dialogView)
            .setPositiveButton("添加", null)  // 先设置为null，稍后手动处理
            .setNegativeButton("取消", null)
            .create()

        dialog.show()

        // 手动处理添加按钮点击
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val pastedText = etLinkUrl.text.toString().trim()

            if (pastedText.isEmpty()) {
                android.widget.Toast.makeText(this, "请粘贴分享链接或网址", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 禁用按钮，显示解析进度
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = false
            etLinkUrl.isEnabled = false
            progressParsing.visibility = View.VISIBLE
            tvParsingStatus.visibility = View.VISIBLE
            tvParsingStatus.text = "正在解析链接..."

            // 使用后端API解析链接
            lifecycleScope.launch {
                try {
                    val request = ParseLinkRequest(pastedText = pastedText)
                    val response = ApiClient.getService().parseExternalLink(request)

                    if (response.isSuccessful && response.body()?.code == 200) {
                        // 解析成功
                        val parsed = response.body()?.data
                        if (parsed != null) {
                            val link = ParsedExternalLink(
                                title = parsed.title,
                                url = parsed.url,
                                source = parsed.source ?: "网络",
                                thumbnail = parsed.thumbnail
                            )
                            externalLinks.add(link)
                            saveDraft()
                            linkAdapter.notifyItemInserted(externalLinks.size - 1)

                            android.widget.Toast.makeText(
                                this@EditRecipeActivity,
                                "添加成功：${parsed.title}",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            dialog.dismiss()
                        } else {
                            // 后端返回null
                            tvParsingStatus.text = "解析失败，请检查链接"
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = true
                            etLinkUrl.isEnabled = true
                            progressParsing.visibility = View.GONE
                        }
                    } else {
                        // API返回错误
                        val errorMsg = response.body()?.message ?: "解析失败"
                        tvParsingStatus.text = errorMsg
                        android.widget.Toast.makeText(
                            this@EditRecipeActivity,
                            errorMsg,
                            android.widget.Toast.LENGTH_SHORT
                        ).show()

                        // 恢复按钮状态
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = true
                        etLinkUrl.isEnabled = true
                        progressParsing.visibility = View.GONE
                    }
                } catch (e: kotlinx.coroutines.CancellationException) { throw e
            } catch (e: Exception) {
                    android.util.Log.e("EditRecipe", "解析链接异常", e)
                    tvParsingStatus.text = "网络错误，请重试"
                    android.widget.Toast.makeText(
                        this@EditRecipeActivity,
                        "网络错误：${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()

                    // 恢复按钮状态
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = true
                    etLinkUrl.isEnabled = true
                    progressParsing.visibility = View.GONE
                }
            }
        }
    }

    /**
     * 设置权限选择
     */
    private fun setupPermission() {
        // 初始化显示当前厨房名称
        updatePermissionDisplay()

        binding.layoutPermission.setOnClickListener {
            showPermissionDialog()
        }
    }

    /**
     * 更新权限显示
     */
    private fun updatePermissionDisplay() {
        val subtitle = when (selectedVisibility) {
            "public" -> "公开可见"
            "private" -> "仅自己可见"
            "group" -> {
                if (selectedGroupIds.isEmpty()) {
                    // 显示当前厨房名称
                    val currentKitchenName = com.familyrecipes.android.data.local.PreferenceManager.currentKitchenName
                    currentKitchenName ?: "当前厨房可见"
                } else {
                    "已分享给 ${selectedGroupIds.size} 个群组"
                }
            }
            else -> "当前厨房可见"
        }

        binding.tvPermissionSubtitle.text = subtitle
        saveDraft()
    }

    /**
     * 显示权限选择对话框
     */
    private fun showPermissionDialog() {
        val options = arrayOf("公开可见", "仅自己可见", "分享给群组")

        AlertDialog.Builder(this)
            .setTitle("可见范围")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // 公开可见
                        selectedVisibility = "public"
                        selectedGroupIds.clear()
                        updatePermissionDisplay()
                    }
                    1 -> {
                        // 仅自己可见
                        selectedVisibility = "private"
                        selectedGroupIds.clear()
                        updatePermissionDisplay()
                    }
                    2 -> {
                        // 分享给群组
                        selectedVisibility = "group"
                        showGroupSelectionDialog()
                    }
                }
            }
            .show()
    }

    /**
     * 显示群组选择对话框（多选）
     */
    private fun showGroupSelectionDialog() {
        lifecycleScope.launch {
            try {
                // 调用API获取用户的群组列表
                val response = ApiClient.getService().getGroupsList()

                if (response.isSuccessful && response.body()?.code == 200) {
                    val groups = response.body()?.data ?: emptyList()

                    if (groups.isEmpty()) {
                        android.widget.Toast.makeText(
                            this@EditRecipeActivity,
                            "您还没有加入任何群组",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }

                    showGroupCheckboxDialog(groups)
                } else {
                    android.widget.Toast.makeText(
                        this@EditRecipeActivity,
                        "获取群组列表失败",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(
                    this@EditRecipeActivity,
                    "获取群组列表失败：${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * 显示群组多选对话框
     */
    private fun showGroupCheckboxDialog(groups: List<GroupChat>) {
        val groupNames = groups.map { it.name }.toTypedArray()
        val checkedItems = groups.map { selectedGroupIds.contains(it.id) }.toBooleanArray()

        AlertDialog.Builder(this)
            .setTitle("选择要分享的群组")
            .setMultiChoiceItems(groupNames, checkedItems) { _, which, isChecked ->
                val groupId = groups[which].id ?: return@setMultiChoiceItems
                if (isChecked) {
                    if (!selectedGroupIds.contains(groupId)) {
                        selectedGroupIds.add(groupId)
                    }
                } else {
                    selectedGroupIds.remove(groupId)
                }
            }
            .setPositiveButton("确定") { _, _ ->
                // 如果没有选择任何群组，默认为当前厨房
                if (selectedGroupIds.isEmpty()) {
                    val currentKitchenId = com.familyrecipes.android.data.local.PreferenceManager.currentGroupId
                    if (currentKitchenId != null) {
                        selectedGroupIds.add(currentKitchenId)
                    }
                }
                updatePermissionDisplay()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun setupPublishButton() {
        binding.btnPublish.setOnClickListener {
            val title = binding.etRecipeTitle.text.toString().trim()

            if (title.isEmpty()) {
                binding.etRecipeTitle.error = "请输入菜名"
                return@setOnClickListener
            }

            if(stepDrafts.any{it.text.isBlank()}){android.widget.Toast.makeText(this,"请填写每一步的做法，或删除空步骤",android.widget.Toast.LENGTH_LONG).show();return@setOnClickListener}
            if(timeField.text.isNotBlank() && (timeField.text.toString().toIntOrNull()?.let{it in 1..10080}!=true)){timeField.error="请输入1至10080分钟";return@setOnClickListener}
            if (recipeId != null) {
                updateRecipe()
            } else {
                publishRecipe()
            }
        }
    }

    private fun publishRecipe() {
        // 禁用发布按钮，防止重复提交
        saveDraft()
        setEditingEnabled(false)
        binding.btnPublish.isEnabled = false
        binding.btnPublish.text = "发布中..."

        lifecycleScope.launch {
            try {
                // 1. 上传图片（如果有）
                val imageUrls = if (selectedImages.isNotEmpty()) {
                    uploadImages()
                } else {
                    emptyList()
                }

                // 2. 准备菜谱数据
                val title = binding.etRecipeTitle.text.toString().trim()
                val content = binding.etRecipeContent.text.toString().trim()
                val tags = parseTags(binding.etTags.text.toString())
                val currentUserId = com.familyrecipes.android.data.local.PreferenceManager.userId

                val recipe = Recipe(
                    id = null,
                    name = title,
                    description = content,
                    coverImage = imageUrls.firstOrNull { photoMeta[it]?.category == "cover" } ?: imageUrls.firstOrNull(),
                    images = imageUrls.mapIndexed { index,url -> (photoMeta[selectedImages.getOrNull(index)?.toString()] ?: RecipeImage(url)).copy(url=url,position=index) }, // 主图
                    cookingTime = timeField.text.toString().toIntOrNull(),
                    difficulty = null,
                    servings = null,
                    creatorId = if (currentUserId > 0) currentUserId else null,
                    viewCount = null,
                    favoriteCount = null,
                    dislikeCount = null,
                    recentlyCookedCount = null,
                    createdAt = null,
                    // 权限信息
                    visibility = selectedVisibility,
                    sharedGroupIds = if (selectedGroupIds.isNotEmpty()) {
                        com.google.gson.Gson().toJson(selectedGroupIds)
                    } else null,
                    kitchenId = existingRecipe?.kitchenId ?: com.familyrecipes.android.data.local.PreferenceManager.currentKitchenId,
                    ownerGroupId = com.familyrecipes.android.data.local.PreferenceManager.currentGroupId,
                    creator = null,
                    tags = null,
                    ingredients = null,
                    steps = null,
                    externalRecipes = null,
                    cooks = null,
                    isFavorite = null,
                    isDisliked = null
                )

                // 3. 准备标签数据
                val tagObjects = tags.map { tagValue ->
                    RecipeTag(
                        id = null,
                        recipeId = null,
                        tagType = "custom", // 自定义标签都是custom类型
                        tagValue = tagValue
                    )
                }

                // 4. 准备外部链接数据
                val externalRecipeObjects = externalLinks.map { link ->
                    ExternalRecipe(
                        id = null,
                        recipeId = null,
                        title = link.title,
                        url = link.url,
                        source = link.source,
                        thumbnail = link.thumbnail,
                        addedBy = if (currentUserId > 0) currentUserId else null,
                        createdAt = null
                    )
                }

                // 5. 创建请求对象
                val request = CreateRecipeRequest(
                    recipe = recipe,
                    tags = tagObjects.filter{it.tagType!="taste"} + tasteTags(),
                    ingredients = prepareIngredients(),
                    steps = preparedSteps(),
                    cookUserIds = null, // 暂时不处理会做的人
                    externalRecipes = externalRecipeObjects  // 添加外部链接
                )

                // 6. 调用API创建菜谱
                val response = ApiClient.getService().createRecipe(request)

                if (response.isSuccessful && response.body()?.code == 200) {
                    android.widget.Toast.makeText(this@EditRecipeActivity, "发布成功！", android.widget.Toast.LENGTH_SHORT).show()
                    saved=true
                    DraftWrites.clear(account,draftKey)
                    setResult(RESULT_OK)
                    finish()
                } else {
                    android.widget.Toast.makeText(
                        this@EditRecipeActivity,
                        "发布失败：${response.body()?.message ?: "未知错误"}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    this@EditRecipeActivity,
                    "发布失败：${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                e.printStackTrace()
            } finally {
                setEditingEnabled(true)
                binding.btnPublish.isEnabled = true
                binding.btnPublish.text = "保存菜谱"
            }
        }
    }

    private fun updateRecipe() {
        // 禁用按钮，防止重复提交
        saveDraft()
        setEditingEnabled(false)
        binding.btnPublish.isEnabled = false
        binding.btnPublish.text = "保存中..."

        lifecycleScope.launch {
            try {
                // 1. 上传新图片（如果有）
                val imageUrls = if (selectedImages.isNotEmpty()) {
                    uploadImages()
                } else {
                    // 保留原有封面
                    emptyList()
                }

                // 2. 准备菜谱数据
                val title = binding.etRecipeTitle.text.toString().trim()
                val content = binding.etRecipeContent.text.toString().trim()
                val tags = parseTags(binding.etTags.text.toString())

                val recipe = Recipe(
                    id = recipeId,
                    name = title,
                    description = content,
                    coverImage = imageUrls.firstOrNull { photoMeta[it]?.category == "cover" } ?: imageUrls.firstOrNull(),
                    images = imageUrls.mapIndexed { index,url -> (photoMeta[selectedImages.getOrNull(index)?.toString()] ?: RecipeImage(url)).copy(url=url,position=index) },
                    cookingTime = timeField.text.toString().toIntOrNull(),
                    difficulty = existingRecipe?.difficulty,
                    servings = existingRecipe?.servings,
                    creatorId = existingRecipe?.creatorId,
                    viewCount = null,
                    favoriteCount = null,
                    dislikeCount = null,
                    recentlyCookedCount = null,
                    createdAt = null,
                    // 权限信息
                    visibility = selectedVisibility,
                    sharedGroupIds = if (selectedGroupIds.isNotEmpty()) {
                        com.google.gson.Gson().toJson(selectedGroupIds)
                    } else null,
                    kitchenId = existingRecipe?.kitchenId ?: com.familyrecipes.android.data.local.PreferenceManager.currentKitchenId,
                    ownerGroupId = existingRecipe?.ownerGroupId
                        ?: com.familyrecipes.android.data.local.PreferenceManager.currentGroupId,
                    creator = null,
                    tags = null,
                    ingredients = null,
                    steps = null,
                    externalRecipes = null,
                    cooks = null,
                    isFavorite = null,
                    isDisliked = null
                )

                // 3. 准备标签数据
                val tagObjects = tags.map { tagValue ->
                    RecipeTag(
                        id = null,
                        recipeId = recipeId,
                        tagType = "custom",
                        tagValue = tagValue
                    )
                }

                // 4. 准备外部链接数据
                val currentUserId = com.familyrecipes.android.data.local.PreferenceManager.userId
                val externalRecipeObjects = externalLinks.map { link ->
                    ExternalRecipe(
                        id = null,
                        recipeId = recipeId,
                        title = link.title,
                        url = link.url,
                        source = link.source,
                        thumbnail = link.thumbnail,
                        addedBy = if (currentUserId > 0) currentUserId else null,
                        createdAt = null
                    )
                }

                // 5. 创建请求对象
                val request = CreateRecipeRequest(
                    recipe = recipe,
                    tags = tagObjects.filter{it.tagType!="taste"} + tasteTags(),
                    ingredients = prepareIngredients(),
                    steps = preparedSteps(),
                    cookUserIds = null,
                    externalRecipes = externalRecipeObjects  // 添加外部链接
                )

                // 6. 调用API更新菜谱
                val response = ApiClient.getService().updateRecipe(recipeId!!, request)

                if (response.isSuccessful && response.body()?.code == 200) {
                    android.widget.Toast.makeText(this@EditRecipeActivity, "保存成功！", android.widget.Toast.LENGTH_SHORT).show()
                    saved=true
                    DraftWrites.clear(account,draftKey)
                    setResult(RESULT_OK)
                    finish()
                } else {
                    android.widget.Toast.makeText(
                        this@EditRecipeActivity,
                        "保存失败：${response.body()?.message ?: "未知错误"}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    this@EditRecipeActivity,
                    "保存失败：${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                e.printStackTrace()
            } finally {
                setEditingEnabled(true)
                binding.btnPublish.isEnabled = true
                binding.btnPublish.text = "保存"
            }
        }
    }

    private suspend fun uploadImages(): List<String> {
        val uploadedUrls = mutableListOf<String>()
        for (uri in selectedImages.toList()) {
            if(uri.scheme=="https" || uri.scheme=="http") { uploadedUrls.add(uri.toString());continue }
            val file = uriToFile(uri)
            try {
                val body = MultipartBody.Part.createFormData("file", file.name,
                    file.asRequestBody("image/*".toMediaTypeOrNull()))
                val response = ApiClient.getService().uploadImage(body,photoMeta[uri.toString()]?.category ?: "screenshot")
                val url = response.body()?.data
                if (!response.isSuccessful || response.body()?.code != 200 || url.isNullOrBlank()) {
                    throw java.io.IOException("图片上传失败，菜谱尚未保存")
                }
                uploadedUrls.add(url)
                val index=selectedImages.indexOf(uri)
                if(index>=0) {
                    photoMeta[url]=(photoMeta.remove(uri.toString()) ?: RecipeImage(url)).copy(url=url)
                    selectedImages[index]=Uri.parse(url)
                    stepDrafts.forEach{if(it.image==uri.toString())it.image=url}
                    saveDraft()
                }
            } finally { file.delete() }
        }
        return uploadedUrls
    }

    private fun uriToFile(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri)
        val tempFile = File(cacheDir, "temp_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(tempFile)

        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }

        return tempFile
    }

    /**
     * 解析tag输入
     * 支持#开头的tag，用空格分隔
     * 例如：#午餐 #家常菜 #快手 -> ["午餐", "家常菜", "快手"]
     */
    private fun parseTags(input: String): List<String> {
        if (input.isBlank()) return emptyList()

        // 按空格分割
        return input.trim()
            .split(Regex("\\s+"))
            .mapNotNull { tag ->
                // 移除#前缀，过滤空字符串
                val cleaned = tag.trim().removePrefix("#")
                if (cleaned.isNotEmpty()) cleaned else null
            }
            .distinct() // 去重
    }

    /**
     * 加载现有菜谱数据
     */
    private fun loadExistingRecipe(id: Long) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.getService().getRecipeDetail(id)
                if (response.isSuccessful && response.body()?.code == 200) {
                    existingRecipe = response.body()?.data
                    displayExistingRecipe(existingRecipe!!)
                    draftReady=true
                    setEditingEnabled(true)
                    binding.btnPublish.isEnabled=true
                    binding.etRecipeTitle.isEnabled=true;binding.etRecipeContent.isEnabled=true;binding.etTags.isEnabled=true
                    saveDraft()
                } else {
                    android.widget.Toast.makeText(
                        this@EditRecipeActivity,
                        "加载菜谱失败",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    this@EditRecipeActivity,
                    "加载菜谱失败：${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                e.printStackTrace()
                finish()
            }
        }
    }

    /**
     * 显示现有菜谱数据
     */
    private fun displayExistingRecipe(recipe: Recipe) {
        ingredientField.setText(recipe.ingredients.orEmpty().joinToString("\n"){"${it.ingredient?.name.orEmpty()} | ${it.amount.orEmpty()}"});timeField.setText(recipe.cookingTime?.toString().orEmpty());tasteField.setText(recipe.tags.orEmpty().filter{it.tagType=="taste"}.joinToString("、"){it.tagValue});stepDrafts.clear();stepDrafts.addAll(recipe.steps.orEmpty().map{StepDraft(it.description,it.image)});drawSteps()
        // 填充菜名
        binding.etRecipeTitle.setText(recipe.name)

        // 填充内容描述
        binding.etRecipeContent.setText(recipe.description)

        // 填充tags（确保带#号显示）
        val tagsText = recipe.tags?.filter{it.tagType!="taste"}?.joinToString(" ") {
            if (it.tagValue.startsWith("#")) {
                it.tagValue
            } else {
                "#${it.tagValue}"
            }
        } ?: ""
        binding.etTags.setText(tagsText)

        // 加载外部链接
        recipe.externalRecipes?.forEach { externalRecipe ->
            val link = ParsedExternalLink(
                title = externalRecipe.title,
                url = externalRecipe.url,
                source = externalRecipe.source ?: "网络",
                thumbnail = externalRecipe.thumbnail
            )
            externalLinks.add(link)
        }
        linkAdapter.notifyDataSetChanged()

        // 加载权限信息
        selectedVisibility = recipe.visibility ?: "group"
        recipe.sharedGroupIds?.let { json ->
            try {
                val groupIds = com.google.gson.Gson().fromJson(json, Array<Long>::class.java)
                selectedGroupIds.clear()
                selectedGroupIds.addAll(groupIds)
            } catch (e: kotlinx.coroutines.CancellationException) { throw e
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        updatePermissionDisplay()

        val photos=recipe.images?.takeIf { it.isNotEmpty() } ?: listOfNotNull(recipe.coverImage?.let { RecipeImage(it,"cover") })
        selectedImages.clear();selectedImages.addAll(photos.map { Uri.parse(it.url) })
        photoMeta.clear();photos.forEach { photoMeta[it.url]=it }
        imageAdapter.notifyDataSetChanged()
    }
}

/**
 * 图片网格适配器
 */
class ImageGridAdapter(
    private val images: MutableList<Uri>,
    private val onChanged: () -> Unit,
    private val onEdit: (Int) -> Unit,
    private val onAddClick: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_IMAGE = 0
        const val TYPE_ADD = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < images.size) TYPE_IMAGE else TYPE_ADD
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_IMAGE) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_image_preview, parent, false)
            ImageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_add_image, parent, false)
            AddImageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ImageViewHolder -> {
                com.bumptech.glide.Glide.with(holder.itemView).load(com.familyrecipes.android.util.MediaImages.model(images[position].toString()))
                    .signature(com.bumptech.glide.signature.ObjectKey(PreferenceManager.userId)).into(holder.imageView)
                holder.imageView.setOnClickListener { onEdit(holder.bindingAdapterPosition) }
                holder.btnDelete.setOnClickListener {
                    images.removeAt(position)
                    onChanged()
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, itemCount)
                }
            }
            is AddImageViewHolder -> {
                holder.itemView.setOnClickListener {
                    onAddClick()
                }
            }
        }
    }

    override fun getItemCount(): Int = images.size + 1 // +1 for add button

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.iv_image)
        val btnDelete: View = view.findViewById(R.id.btn_delete)
    }

    class AddImageViewHolder(view: View) : RecyclerView.ViewHolder(view)
}

/**
 * 外部链接适配器
 */
class ExternalLinkAdapter(
    private val links: MutableList<EditRecipeActivity.ParsedExternalLink>,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<ExternalLinkAdapter.LinkViewHolder>() {

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): LinkViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_external_link, parent, false)
        return LinkViewHolder(view)
    }

    override fun onBindViewHolder(holder: LinkViewHolder, position: Int) {
        val link = links[position]

        holder.tvTitle.text = link.title
        holder.tvSource.text = "来源：${link.source}"
        holder.tvUrl.text = link.url

        // 加载缩略图
        if (link.thumbnail != null) {
            com.bumptech.glide.Glide.with(holder.itemView.context)
                .load(com.familyrecipes.android.util.MediaImages.model(link.thumbnail))
                .signature(com.bumptech.glide.signature.ObjectKey(com.familyrecipes.android.data.local.PreferenceManager.userId))
                .placeholder(R.drawable.ic_food)
                .error(R.drawable.ic_food)
                .into(holder.ivThumbnail)
        } else {
            holder.ivThumbnail.setImageResource(R.drawable.ic_food)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(position)
        }
    }

    override fun getItemCount(): Int = links.size

    class LinkViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivThumbnail: ImageView = view.findViewById(R.id.iv_thumbnail)
        val tvTitle: android.widget.TextView = view.findViewById(R.id.tv_link_title)
        val tvSource: android.widget.TextView = view.findViewById(R.id.tv_link_source)
        val tvUrl: android.widget.TextView = view.findViewById(R.id.tv_link_url)
        val btnDelete: View = view.findViewById(R.id.btn_delete_link)
    }
}

