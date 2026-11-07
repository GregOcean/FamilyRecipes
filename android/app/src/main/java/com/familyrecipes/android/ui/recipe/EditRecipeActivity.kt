package com.familyrecipes.android.ui.recipe

import com.familyrecipes.android.ui.design.Ui
import com.familyrecipes.android.ui.design.UiStrings
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
        box.addView(com.familyrecipes.android.ui.design.Ui.text(this,UiStrings.s(com.familyrecipes.android.R.string.ui_c45c4747bb),20f,true))
        ingredientField=com.familyrecipes.android.ui.design.Ui.field(this,UiStrings.s(com.familyrecipes.android.R.string.ui_93b21972a4),multi=true);box.addView(ingredientField)
        timeField=com.familyrecipes.android.ui.design.Ui.field(this,UiStrings.s(com.familyrecipes.android.R.string.ui_1c0e1e271a));timeField.inputType=android.text.InputType.TYPE_CLASS_NUMBER;box.addView(timeField)
        tasteField=com.familyrecipes.android.ui.design.Ui.field(this,UiStrings.s(com.familyrecipes.android.R.string.ui_64eafb8239));box.addView(tasteField)
        box.addView(com.familyrecipes.android.ui.design.Ui.text(this,UiStrings.s(com.familyrecipes.android.R.string.ui_fea10d57e2),20f,true));stepBox=com.familyrecipes.android.ui.design.Ui.box(this,0);box.addView(stepBox)
        box.addView(com.familyrecipes.android.ui.design.Ui.button(this,UiStrings.s(com.familyrecipes.android.R.string.ui_dd24a90cd5)){stepDrafts.add(StepDraft());drawSteps();saveDraft()})
        parent.addView(box,parent.indexOfChild(binding.etRecipeContent.parent.parent as View)+1)
        listOf(ingredientField,timeField,tasteField).forEach{it.doAfterTextChanged{saveDraft()}}
    }
    private fun drawSteps(){
        stepBox.removeAllViews()
        stepDrafts.forEachIndexed{index,step->
            val box=com.familyrecipes.android.ui.design.Ui.card(this)
            box.addView(com.familyrecipes.android.ui.design.Ui.text(this,UiStrings.f(com.familyrecipes.android.R.string.step_number, index+1),18f,true))
            val field=com.familyrecipes.android.ui.design.Ui.field(this,UiStrings.s(com.familyrecipes.android.R.string.ui_6b939b6382),step.text,true);box.addView(field);field.doAfterTextChanged{step.text=it.toString();saveDraft()}
            step.image?.let{url->val preview=ImageView(this).apply{adjustViewBounds=true};box.addView(preview,android.widget.LinearLayout.LayoutParams(-1,180));com.bumptech.glide.Glide.with(this).load(com.familyrecipes.android.util.MediaImages.model(url)).into(preview)}
            box.addView(com.familyrecipes.android.ui.design.Ui.button(this,if(step.image==null)UiStrings.s(com.familyrecipes.android.R.string.ui_69f5326297) else UiStrings.s(com.familyrecipes.android.R.string.ui_0e8168f0dd)){pendingStep=index;pickImageLauncher.launch("image/*")})
            val actions=com.familyrecipes.android.ui.design.Ui.row(this)
            actions.addView(com.familyrecipes.android.ui.design.Ui.button(this,UiStrings.s(com.familyrecipes.android.R.string.ui_315eacd193)){if(index>0){java.util.Collections.swap(stepDrafts,index,index-1);drawSteps();saveDraft()}}.apply{isEnabled=index>0})
            actions.addView(com.familyrecipes.android.ui.design.Ui.button(this,UiStrings.s(com.familyrecipes.android.R.string.ui_407fd4f2fb)){stepDrafts.removeAt(index);drawSteps();saveDraft()})
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
    private var selectedVisibility: String = "public" // public, private, group
    private val selectedGroupIds = mutableListOf<Long>() // 分享的群组ID列表

    /**
     * 解析后的外部链接数据
     */
    data class ParsedExternalLink(
        val title: String,
        val url: String,
        val source: String,
        val thumbnail: String?,
        val notes: String? = null
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
              catch(_: Exception) { android.widget.Toast.makeText(this@EditRecipeActivity,UiStrings.s(com.familyrecipes.android.R.string.ui_7b3a4d421f),android.widget.Toast.LENGTH_SHORT).show() }
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
        (binding.etRecipeContent.parent.parent as com.google.android.material.textfield.TextInputLayout).hint = UiStrings.s(com.familyrecipes.android.R.string.ui_87531402ee)
        binding.etRecipeContent.hint=null
        binding.btnPublish.text=if(recipeId==null)UiStrings.s(com.familyrecipes.android.R.string.ui_65a5713f64) else UiStrings.s(com.familyrecipes.android.R.string.ui_be5fbbe34c)
        setEditingEnabled(false)
        lifecycleScope.launch {
            val stored=LocalStore.instance.records().get(account,"draft",draftKey)
            if(stored!=null) {
                try { restoreDraft(com.google.gson.Gson().fromJson(stored,EditorDraft::class.java)) }
                catch(_: Exception) { android.widget.Toast.makeText(this@EditRecipeActivity,UiStrings.s(com.familyrecipes.android.R.string.ui_674593ff88),android.widget.Toast.LENGTH_SHORT).show() }
                draftReady=true
                setEditingEnabled(true)
            } else if(recipeId!=null) {
                binding.btnPublish.isEnabled=false
                binding.etRecipeTitle.isEnabled=false;binding.etRecipeContent.isEnabled=false;binding.etTags.isEnabled=false
                loadExistingRecipe(recipeId!!)
            } else { draftReady=true;setEditingEnabled(true) }
        }
        if(recipeId!=null) { binding.toolbar.title=UiStrings.s(com.familyrecipes.android.R.string.ui_55a6506c29);binding.btnPublish.text=UiStrings.s(com.familyrecipes.android.R.string.ui_be5fbbe34c) }
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
        val labels=arrayOf(UiStrings.s(com.familyrecipes.android.R.string.ui_412310615b),UiStrings.s(com.familyrecipes.android.R.string.ui_0d5635dea1),UiStrings.s(com.familyrecipes.android.R.string.ui_a334f6e2d9),UiStrings.s(com.familyrecipes.android.R.string.ui_b107c6ae6b),"其他")
        val box=android.widget.LinearLayout(this).apply { orientation=android.widget.LinearLayout.VERTICAL }
        val caption=EditText(this).apply { hint=UiStrings.s(com.familyrecipes.android.R.string.ui_cea52e2a12);setText(current.caption) }
        val spinner=android.widget.Spinner(this).apply {
            adapter=android.widget.ArrayAdapter(this@EditRecipeActivity,android.R.layout.simple_spinner_dropdown_item,labels)
            setSelection(categories.indexOf(current.category).coerceAtLeast(0))
        }
        box.addView(spinner);box.addView(caption)
        AlertDialog.Builder(this).setTitle(UiStrings.s(com.familyrecipes.android.R.string.ui_04eefad0c1)).setView(box).setPositiveButton(UiStrings.s(com.familyrecipes.android.R.string.ui_be5fbbe34c)) { _,_ ->
            photoMeta[key]=current.copy(category=categories[spinner.selectedItemPosition],caption=caption.text.toString())
            saveDraft()
        }.setNegativeButton(UiStrings.s(com.familyrecipes.android.R.string.ui_625fb26b4b),null).show()
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
        linkAdapter = ExternalLinkAdapter(externalLinks, { position ->
            externalLinks.removeAt(position);saveDraft();linkAdapter.notifyDataSetChanged()
        }, { position -> editExternalLink(position) }, { position, delta ->
            val target=position+delta
            if(target in externalLinks.indices) {
                java.util.Collections.swap(externalLinks,position,target)
                saveDraft();linkAdapter.notifyDataSetChanged()
            }
        })

        binding.recyclerExternalLinks.apply {
            layoutManager = LinearLayoutManager(this@EditRecipeActivity)
            adapter = linkAdapter
        }

        // 添加外部链接按钮
        binding.btnAddExternalLink.setOnClickListener {
            showAddLinkDialog()
        }
    }

    private fun editExternalLink(position: Int) {
        val old=externalLinks[position]
        val box=Ui.box(this)
        val title=Ui.field(this,getString(R.string.link_title),old.title)
        val notes=Ui.field(this,getString(R.string.link_notes),old.notes.orEmpty(),true).apply {
            filters=arrayOf(android.text.InputFilter.LengthFilter(2000))
        }
        box.addView(Ui.text(this,getString(R.string.link_title)));box.addView(title)
        box.addView(Ui.text(this,getString(R.string.link_notes)));box.addView(notes)
        val dialog=AlertDialog.Builder(this).setTitle(R.string.link_edit).setView(Ui.scroll(this,box))
            .setNegativeButton(android.R.string.cancel,null).setPositiveButton(android.R.string.ok,null).create()
        dialog.setOnShowListener { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if(title.text.isNullOrBlank() || title.text.length>200) {title.error=getString(R.string.link_title_required);return@setOnClickListener}
            externalLinks[position]=old.copy(title=title.text.toString().trim(),notes=notes.text.toString())
            saveDraft();linkAdapter.notifyItemChanged(position);dialog.dismiss()
        } };dialog.show()
    }

    private fun showAddLinkDialog() {
        if (externalLinks.size >= 30) {
            android.widget.Toast.makeText(this, getString(R.string.link_limit), android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_link, null)
        val etLinkUrl = dialogView.findViewById<EditText>(R.id.et_link_url)
        val progressParsing = dialogView.findViewById<ProgressBar>(R.id.progress_parsing)
        val tvParsingStatus = dialogView.findViewById<TextView>(R.id.tv_parsing_status)

        val dialog = AlertDialog.Builder(this)
            .setTitle(UiStrings.s(com.familyrecipes.android.R.string.ui_95aa68d84d))
            .setView(dialogView)
            .setPositiveButton(UiStrings.s(com.familyrecipes.android.R.string.ui_b58c7549c0), null)  // 先设置为null，稍后手动处理
            .setNegativeButton(UiStrings.s(com.familyrecipes.android.R.string.ui_625fb26b4b), null)
            .create()

        dialog.show()

        // 手动处理添加按钮点击
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val pastedText = etLinkUrl.text.toString().trim()

            if (pastedText.isEmpty()) {
                android.widget.Toast.makeText(this, UiStrings.s(com.familyrecipes.android.R.string.ui_64088b2054), android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 禁用按钮，显示解析进度
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = false
            etLinkUrl.isEnabled = false
            progressParsing.visibility = View.VISIBLE
            tvParsingStatus.visibility = View.VISIBLE
            tvParsingStatus.text = UiStrings.s(com.familyrecipes.android.R.string.ui_0e5444c7ee)

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
                                source = parsed.source ?: UiStrings.s(com.familyrecipes.android.R.string.ui_7ddbe15c84),
                                thumbnail = parsed.thumbnail
                            )
                            externalLinks.add(link)
                            saveDraft()
                            linkAdapter.notifyDataSetChanged()

                            android.widget.Toast.makeText(
                                this@EditRecipeActivity,
                                "添加成功：${parsed.title}",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            dialog.dismiss()
                        } else {
                            // 后端返回null
                            tvParsingStatus.text = UiStrings.s(com.familyrecipes.android.R.string.ui_15152fee56)
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = true
                            etLinkUrl.isEnabled = true
                            progressParsing.visibility = View.GONE
                        }
                    } else {
                        // API返回错误
                        val errorMsg = response.body()?.message ?: UiStrings.s(com.familyrecipes.android.R.string.ui_5348dcac29)
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
                    tvParsingStatus.text = UiStrings.s(com.familyrecipes.android.R.string.ui_7ec65389b4)
                    android.widget.Toast.makeText(
                        this@EditRecipeActivity,
                        UiStrings.f(com.familyrecipes.android.R.string.network_error_value, e.message),
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
            "public" -> UiStrings.s(com.familyrecipes.android.R.string.ui_034929bf43)
            "private" -> UiStrings.s(com.familyrecipes.android.R.string.ui_4d4d119aab)
            "group" -> {
                if (selectedGroupIds.isEmpty()) {
                    // 显示当前厨房名称
                    val currentKitchenName = com.familyrecipes.android.data.local.PreferenceManager.currentKitchenName
                    currentKitchenName ?: UiStrings.s(com.familyrecipes.android.R.string.ui_c0b80e2244)
                } else {
                    UiStrings.f(com.familyrecipes.android.R.string.shared_groups_count, selectedGroupIds.size)
                }
            }
            else -> UiStrings.s(com.familyrecipes.android.R.string.ui_c0b80e2244)
        }

        binding.tvPermissionSubtitle.text = subtitle
        saveDraft()
    }

    /**
     * 显示权限选择对话框
     */
    private fun showPermissionDialog() {
        val options = arrayOf(UiStrings.s(com.familyrecipes.android.R.string.ui_034929bf43), UiStrings.s(com.familyrecipes.android.R.string.ui_4d4d119aab), UiStrings.s(com.familyrecipes.android.R.string.ui_012b318667))

        AlertDialog.Builder(this)
            .setTitle(UiStrings.s(com.familyrecipes.android.R.string.ui_f964950720))
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
                            UiStrings.s(com.familyrecipes.android.R.string.ui_1a576128dc),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }

                    showGroupCheckboxDialog(groups)
                } else {
                    android.widget.Toast.makeText(
                        this@EditRecipeActivity,
                        UiStrings.s(com.familyrecipes.android.R.string.ui_6020ca1779),
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
            .setTitle(UiStrings.s(com.familyrecipes.android.R.string.ui_8194d1e012))
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
            .setPositiveButton(UiStrings.s(com.familyrecipes.android.R.string.ui_38cf16f220)) { _, _ ->
                // 如果没有选择任何群组，默认为当前厨房
                if (selectedGroupIds.isEmpty()) {
                    val currentKitchenId = com.familyrecipes.android.data.local.PreferenceManager.currentGroupId
                    if (currentKitchenId != null) {
                        selectedGroupIds.add(currentKitchenId)
                    }
                }
                updatePermissionDisplay()
            }
            .setNegativeButton(UiStrings.s(com.familyrecipes.android.R.string.ui_625fb26b4b), null)
            .show()
    }

    private fun setupPublishButton() {
        binding.btnPublish.setOnClickListener {
            val title = binding.etRecipeTitle.text.toString().trim()

            if (title.isEmpty()) {
                binding.etRecipeTitle.error = UiStrings.s(com.familyrecipes.android.R.string.ui_9b90f55e19)
                return@setOnClickListener
            }

            if(stepDrafts.any{it.text.isBlank()}){android.widget.Toast.makeText(this,UiStrings.s(com.familyrecipes.android.R.string.ui_c2c9ef4b52),android.widget.Toast.LENGTH_LONG).show();return@setOnClickListener}
            if(timeField.text.isNotBlank() && (timeField.text.toString().toIntOrNull()?.let{it in 1..10080}!=true)){timeField.error=UiStrings.s(com.familyrecipes.android.R.string.ui_f94651d354);return@setOnClickListener}
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
        binding.btnPublish.text = UiStrings.s(com.familyrecipes.android.R.string.ui_27d8a2fa6b)

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
                val externalRecipeObjects = externalLinks.mapIndexed { index, link ->
                    ExternalRecipe(
                        id = null,
                        recipeId = null,
                        title = link.title,
                        url = link.url,
                        source = link.source,
                        thumbnail = link.thumbnail,
                        notes = link.notes,
                        sortOrder = index,
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
                    android.widget.Toast.makeText(this@EditRecipeActivity, UiStrings.s(com.familyrecipes.android.R.string.ui_6c6eb30524), android.widget.Toast.LENGTH_SHORT).show()
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
                binding.btnPublish.text = UiStrings.s(com.familyrecipes.android.R.string.ui_65a5713f64)
            }
        }
    }

    private fun updateRecipe() {
        // 禁用按钮，防止重复提交
        saveDraft()
        setEditingEnabled(false)
        binding.btnPublish.isEnabled = false
        binding.btnPublish.text = UiStrings.s(com.familyrecipes.android.R.string.ui_2a33020e5b)

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
                val externalRecipeObjects = externalLinks.mapIndexed { index, link ->
                    ExternalRecipe(
                        id = null,
                        recipeId = recipeId,
                        title = link.title,
                        url = link.url,
                        source = link.source,
                        thumbnail = link.thumbnail,
                        notes = link.notes,
                        sortOrder = index,
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
                    android.widget.Toast.makeText(this@EditRecipeActivity, UiStrings.s(com.familyrecipes.android.R.string.ui_fbd2495eab), android.widget.Toast.LENGTH_SHORT).show()
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
                binding.btnPublish.text = UiStrings.s(com.familyrecipes.android.R.string.ui_be5fbbe34c)
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
                    throw java.io.IOException(UiStrings.s(com.familyrecipes.android.R.string.ui_aab38be60f))
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
                        UiStrings.s(com.familyrecipes.android.R.string.ui_b38754def3),
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
                source = externalRecipe.source ?: UiStrings.s(com.familyrecipes.android.R.string.ui_7ddbe15c84),
                thumbnail = externalRecipe.thumbnail,
                notes = externalRecipe.notes
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
    private val onDeleteClick: (Int)->Unit,
    private val onEdit: (Int)->Unit,
    private val onMove: (Int,Int)->Unit
): RecyclerView.Adapter<ExternalLinkAdapter.LinkViewHolder>() {
    class LinkViewHolder(val box:android.widget.LinearLayout):RecyclerView.ViewHolder(box)
    override fun getItemCount()=links.size
    override fun onCreateViewHolder(parent:android.view.ViewGroup,type:Int)=LinkViewHolder(Ui.card(parent.context))
    override fun onBindViewHolder(holder:LinkViewHolder,position:Int) {
        val c=holder.box.context;val link=links[position];val box=holder.box;box.removeAllViews()
        link.thumbnail?.takeIf { it.isNotBlank() }?.let { url ->
            val thumbnail=android.widget.ImageView(c).apply {
                scaleType=android.widget.ImageView.ScaleType.CENTER_CROP
                layoutParams=android.widget.LinearLayout.LayoutParams(-1,(120*c.resources.displayMetrics.density).toInt())
            }
            box.addView(thumbnail)
            com.bumptech.glide.Glide.with(thumbnail)
                .load(com.familyrecipes.android.util.MediaImages.model(url))
                .signature(com.bumptech.glide.signature.ObjectKey(PreferenceManager.userId)).into(thumbnail)
        }
        box.addView(Ui.text(c,"${position+1}. ${link.title}",18f,true))
        box.addView(Ui.text(c,link.source,15f))
        box.addView(Ui.text(c,link.url,14f))
        box.addView(Ui.text(c,link.notes?.takeIf{it.isNotBlank()} ?: c.getString(R.string.link_notes_empty),16f))
        val actions=Ui.row(c)
        fun action(label:Int,enabled:Boolean=true,run:(Int)->Unit) {
            actions.addView(Ui.button(c,c.getString(label)) {
                val current=holder.bindingAdapterPosition
                if(current!=RecyclerView.NO_POSITION)run(current)
            }.apply{isEnabled=enabled},android.widget.LinearLayout.LayoutParams(0,-2,1f))
        }
        action(R.string.link_up,position>0){onMove(it,-1)}
        action(R.string.link_down,position<links.lastIndex){onMove(it,1)}
        box.addView(actions)
        val edit=Ui.button(c,c.getString(R.string.link_edit)){val i=holder.bindingAdapterPosition;if(i!=RecyclerView.NO_POSITION)onEdit(i)}
        box.addView(edit)
        box.addView(Ui.button(c,c.getString(R.string.link_delete)){val i=holder.bindingAdapterPosition;if(i!=RecyclerView.NO_POSITION)onDeleteClick(i)})
    }
}
