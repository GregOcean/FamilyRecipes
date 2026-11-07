package com.familyrecipes.android.ui.social

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.util.Linkify
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import com.familyrecipes.android.R
import com.familyrecipes.android.data.local.PreferenceManager
import com.familyrecipes.android.data.model.*
import com.familyrecipes.android.ui.design.UserAvatar
import com.familyrecipes.android.ui.design.Ui
import com.familyrecipes.android.ui.meal.MealEditorActivity
import com.familyrecipes.android.ui.recipe.RecipeDetailActivity
import com.familyrecipes.android.util.MediaImages


class MessageAdapter(private val messages:List<Message>, private val currentUserId:Long,
    private val pendingAction:(Message)->Unit):RecyclerView.Adapter<MessageAdapter.Holder>() {
    var members:List<GroupMember> = emptyList()
    class Holder(val root:LinearLayout):RecyclerView.ViewHolder(root)
    override fun onCreateViewHolder(parent:ViewGroup,type:Int)=Holder(Ui.row(parent.context).apply {
        layoutParams=RecyclerView.LayoutParams(-1,-2)
    })
    override fun getItemCount()=messages.size
    override fun onBindViewHolder(holder:Holder,position:Int) {
        val m=messages[position];val c=holder.root.context
        fun dp(n:Int)=(c.resources.displayMetrics.density*n).toInt()
        val sent=m.senderId==currentUserId
        val member=members.find{it.userId==m.senderId}
        val name=member?.nickname?.takeIf{it.isNotBlank()} ?: m.sender?.username ?: member?.user?.username ?:
            (if(sent) PreferenceManager.userName else null) ?: c.getString(R.string.chat_member_fallback,m.senderId ?: 0)
        val root=holder.root;root.removeAllViews();root.setPadding(dp(12),dp(7),dp(12),dp(7));root.gravity=if(sent) Gravity.END or Gravity.TOP else Gravity.START or Gravity.TOP
        val avatar=UserAvatar.create(c,m.sender?.username ?: member?.user?.username ?: name,m.sender?.avatar ?: member?.user?.avatar)
        val column=Ui.box(c,0).apply {setBackgroundColor(Color.TRANSPARENT);gravity=if(sent) Gravity.END else Gravity.START}
        val maxWidth=(c.resources.displayMetrics.widthPixels-dp(112)).coerceAtLeast(dp(140))
        val nameView=Ui.text(c,name,14f,true).apply {setTextColor(Ui.muted);setPadding(0,0,0,dp(5));this.maxWidth=maxWidth}
        column.addView(nameView)
        val bubble=Ui.box(c,12).apply {
            background=GradientDrawable().apply {setColor(if(sent) Ui.blue else Ui.surface);cornerRadius=dp(18).toFloat()}
        }
        val link=ChatContent.link(m.content)
        if(m.messageType=="sticker") {
            val image=ImageView(c).apply {contentDescription=c.getString(R.string.chat_sticker);adjustViewBounds=true}
            bubble.addView(image,LinearLayout.LayoutParams(dp(136),dp(136)))
            Glide.with(image).load(MediaImages.model(m.content)).signature(ObjectKey(currentUserId)).fitCenter().into(image)
        } else {
            val content=if(link!=null) {
                val kind=if(link.kind=="recipe") R.string.chat_recipe else if(m.content.startsWith("备菜")) R.string.chat_prep else R.string.chat_order
                val heading=Ui.text(c,c.getString(kind),14f,true).apply {setTextColor(if(sent) Ui.onBlue else Ui.blue);setPadding(0,0,0,dp(4))}
                bubble.addView(heading)
                m.content.replace(ChatContent.linkPattern, "").trim().removePrefix("菜谱：").removePrefix("点菜：").removePrefix("备菜：")
            } else m.content
            val text=Ui.text(c,"",18f).apply {
                this.maxWidth=maxWidth-dp(24);setPadding(0,0,0,0)
                setTextColor(if(sent) Ui.onBlue else Ui.ink)
                setLinkTextColor(if(sent) Ui.onBlue else Ui.blue)
            }
            val styled=ChatMentions.decode(content)
            styled.getSpans(0,styled.length,MentionSpan::class.java).forEach { span ->
                val start=styled.getSpanStart(span);val end=styled.getSpanEnd(span)
                styled.setSpan(ForegroundColorSpan(if(sent) Ui.onBlue else Ui.blue),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            text.text=styled
            if(link==null) {
                Linkify.addLinks(text,Linkify.WEB_URLS)
                text.movementMethod=LinkMovementMethod.getInstance()
            }
            bubble.addView(text)
            if(link!=null) {
                bubble.addView(Ui.text(c,c.getString(R.string.chat_open),14f,true).apply{setTextColor(if(sent) Ui.onBlue else Ui.blue);setPadding(0,dp(10),0,0)})
                bubble.setOnClickListener {
                    if(m.id==null) pendingAction(m)
                    else if(link.kind=="meal") c.startActivity(Intent(c,MealEditorActivity::class.java).putExtra("meal_id",link.id))
                    else c.startActivity(Intent(c,RecipeDetailActivity::class.java).putExtra("recipe_id",link.id))
                }
            }
        }
        column.addView(bubble)
        if(ChatContent.mentions(m.content,currentUserId) && !sent) column.addView(Ui.text(c,c.getString(R.string.chat_mentioned),13f,true).apply{setTextColor(Ui.blue)})
        val status=Ui.text(c,if(m.id==null) c.getString(if(m.localError==null) R.string.chat_pending else R.string.chat_retry) else
            m.createdAt.orEmpty().replace('T',' ').let{if(it.length>=16)it.substring(11,16) else ""},12f).apply {
            setTextColor(Ui.muted);setPadding(0,dp(5),0,0)
        }
        if(m.id==null) {
            status.minimumHeight=dp(48);status.setOnClickListener{pendingAction(m)}
            bubble.setOnLongClickListener{pendingAction(m);true}
        }
        column.addView(status)
        val columnParams=LinearLayout.LayoutParams(-2,-2).apply {marginStart=dp(8);marginEnd=dp(8)}
        if(!sent)root.addView(avatar,LinearLayout.LayoutParams(dp(40),dp(40)))
        root.addView(column,columnParams)
        if(sent)root.addView(avatar,LinearLayout.LayoutParams(dp(40),dp(40)))
    }
}
