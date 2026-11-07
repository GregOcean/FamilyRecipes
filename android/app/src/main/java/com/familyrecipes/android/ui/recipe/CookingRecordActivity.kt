package com.familyrecipes.android.ui.recipe

import com.familyrecipes.android.ui.design.UiStrings
import android.net.Uri
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
import com.google.gson.Gson
import java.io.File
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

class CookingRecordActivity : AppCompatActivity() {
    private lateinit var note: EditText
    private lateinit var date: EditText
    private lateinit var photos: LinearLayout
    private var urls = mutableListOf<String>()
    private val account = LocalStore.account
    private var ready = false
    private var saved = false
    private var busy = false
    private val recipeId
        get() = intent.getLongExtra("recipe_id", 0)

    private val key
        get() = "cooking:$recipeId"

    private val pick =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            lifecycleScope.launch {
                try {
                    for (uri in uris.take(20 - urls.size)) {
                        val file =
                            withContext(Dispatchers.IO) {
                                File(filesDir, "draft-images").mkdirs()
                                File(filesDir, "draft-images/${java.util.UUID.randomUUID()}")
                                    .also { f ->
                                        contentResolver.openInputStream(uri)!!.use { input ->
                                            f.outputStream().use { input.copyTo(it) }
                                        }
                                    }
                            }
                        urls.add(Uri.fromFile(file).toString())
                    }
                    drawPhotos()
                    draft()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Ui.toast(this@CookingRecordActivity, e)
                }
            }
        }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val box = Ui.box(this)
        box.addView(Ui.button(this, UiStrings.s(com.familyrecipes.android.R.string.ui_5960795a3e)) { finish() })
        box.addView(Ui.text(this, UiStrings.s(com.familyrecipes.android.R.string.ui_00f9d79879), 24f, true))
        date = Ui.field(this, UiStrings.s(com.familyrecipes.android.R.string.ui_a24fefda6e), java.time.LocalDate.now().toString())
        box.addView(date)
        note = Ui.field(this, UiStrings.s(com.familyrecipes.android.R.string.ui_930ee7d242), multi = true)
        box.addView(note)
        photos = Ui.box(this, 0)
        box.addView(photos)
        box.addView(Ui.button(this, UiStrings.s(com.familyrecipes.android.R.string.ui_df7b52aa75)) { if (!busy) pick.launch("image/*") })
        box.addView(Ui.button(this, UiStrings.s(com.familyrecipes.android.R.string.ui_c76ca04b2c), true) { save() })
        setContentView(Ui.scroll(this, box))
        listOf(note, date).forEach { it.doAfterTextChanged { draft() } }
        lifecycleScope.launch {
            DraftWrites.flush()
            LocalStore.instance.records().get(account, "draft", key)?.let {
                val r = Gson().fromJson(it, CookingRecord::class.java)
                note.setText(r.notes)
                date.setText(r.cookedAt.take(10))
                urls = Gson().fromJson(r.images, Array<String>::class.java).toMutableList()
                drawPhotos()
            }
            ready = true
        }
    }

    private fun draft() {
        if (ready && !saved && (note.text.isNotBlank() || urls.isNotEmpty()))
            DraftWrites.put(
                account,
                key,
                Gson()
                    .toJson(
                        CookingRecord(
                            cookedAt = date.text.toString() + "T12:00:00",
                            notes = note.text.toString(),
                            images = Gson().toJson(urls))))
    }

    override fun onStop() {
        draft()
        super.onStop()
    }

    private fun drawPhotos() {
        photos.removeAllViews()
        urls.toList().forEach { url ->
            val image = ImageView(this).apply { adjustViewBounds = true }
            photos.addView(image, LinearLayout.LayoutParams(-1, 240))
            com.bumptech.glide.Glide.with(this)
                .load(com.familyrecipes.android.util.MediaImages.model(url))
                .into(image)
            photos.addView(
                Ui.button(this, UiStrings.s(com.familyrecipes.android.R.string.ui_6328552cc5)) {
                    if (!busy) {
                        urls.remove(url)
                        drawPhotos()
                        draft()
                    }
                })
        }
    }

    private fun save() {
        if (busy) return
        if (note.text.isBlank()) {
            note.error = UiStrings.s(com.familyrecipes.android.R.string.ui_7d0c2258af)
            return
        }
        if (runCatching { java.time.LocalDate.parse(date.text.toString()) }.isFailure) {
            date.error = UiStrings.s(com.familyrecipes.android.R.string.ui_1986f96841)
            return
        }
        busy = true
        note.isEnabled = false
        date.isEnabled = false
        lifecycleScope.launch {
            try {
                for (i in urls.indices) {
                    if (urls[i].startsWith("file:")) {
                        val file = File(Uri.parse(urls[i]).path!!)
                        val part =
                            MultipartBody.Part.createFormData(
                                "file", file.name, file.asRequestBody("image/*".toMediaType()))
                        urls[i] =
                            Ui.data(ApiClient.getService().uploadImage(part, "cooking_record"))
                        draft()
                    }
                }
                Ui.ok(
                    ApiClient.getService()
                        .saveCookingRecord(
                            recipeId,
                            CookingRecord(
                                cookedAt = date.text.toString() + "T12:00:00",
                                notes = note.text.toString(),
                                images = Gson().toJson(urls))))
                saved = true
                DraftWrites.clear(account, key)
                finish()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Ui.toast(this@CookingRecordActivity, e)
            } finally {
                busy = false
                note.isEnabled = true
                date.isEnabled = true
            }
        }
    }
}
