package com.familyrecipes.android.ui.social

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.Spanned
import android.text.method.LinkMovementMethod
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
import com.familyrecipes.android.util.ChatTime

class MessageAdapter(private val messages:List<Message>,private val currentUserId:Long,
    private val pendingAction:(Message)->Unit):RecyclerView.Adapter<MessageAdapter.Holder>(){
    var members:List<GroupMember> = emptyList()
    class Holder(val root:LinearLayout):RecyclerView.ViewHolder(root)
    override fun onCreateViewHolder(parent:ViewGroup,type:Int)=Holder(Ui.box(parent.context,0).apply{layoutParams=RecyclerView.LayoutParams(-1,-2)})
    override fun getItemCount()=messages.size
    override fun onBindViewHolder(holder:Holder,position:Int){
        val message=messages[position];val c=holder.root.context
        fun dp(n:Int)=(c.resources.displayMetrics.density*n).toInt()
        val previous=messages.getOrNull(position-1)
        val timestamp=ChatTime.label(message.createdAt)
        val dateChanged=ChatTime.differentDay(previous?.createdAt,message.createdAt)
        val separate=position==0 || ChatTime.separate(previous?.createdAt,message.createdAt)
        val root=holder.root;root.removeAllViews();root.setPadding(dp(10),dp(if(separate)8 else 3),dp(10),dp(3))
        if(separate && timestamp.isNotEmpty())root.addView(Ui.text(c,ChatTime.label(message.createdAt,dateChanged || position==0),12f).apply{
            gravity=Gravity.CENTER;setTextColor(Ui.muted);setPadding(0,dp(8),0,dp(8))
        })
        if(message.messageType=="system"){
            root.addView(Ui.text(c,ChatSystemText.display(c,ChatContent.plain(message.content)),13f).apply{gravity=Gravity.CENTER;setTextColor(Ui.muted);setPadding(dp(24),dp(6),dp(24),dp(6))});return
        }
        val sent=message.senderId==currentUserId
        val sameSender=!separate && previous!=null && previous.senderId==message.senderId && previous.messageType!="system"
        val member=members.find{it.userId==message.senderId}
        val name=member?.nickname?.takeIf{it.isNotBlank()}?:message.sender?.username?:member?.user?.username?:
            (if(sent)PreferenceManager.userName else null)?:c.getString(R.string.chat_member_fallback,message.senderId?:0)
        val row=Ui.row(c).apply{gravity=Gravity.TOP;layoutParams=LinearLayout.LayoutParams(-1,-2)}
        val avatar=if(sameSender)Space(c) else UserAvatar.create(c,name,message.sender?.avatar?:member?.user?.avatar)
        val column=Ui.box(c,0).apply{setBackgroundColor(Color.TRANSPARENT);gravity=if(sent)Gravity.END else Gravity.START}
        if(!sameSender && !sent)column.addView(Ui.text(c,name,13f).apply{setTextColor(Ui.muted);setPadding(dp(2),0,0,dp(4));maxLines=1;ellipsize=android.text.TextUtils.TruncateAt.END})
        val bubble=ChatBubbleLayout(c).apply{
            setPadding(dp(12),dp(10),dp(12),dp(10))
            background=GradientDrawable().apply{setColor(if(sent)Ui.blue else Ui.surface);cornerRadius=dp(16).toFloat()}
        }
        val foreground=if(sent)Ui.onBlue else Ui.ink
        val link=ChatContent.link(message.content)
        if(message.messageType=="sticker"){
            val image=ImageView(c).apply{contentDescription=c.getString(R.string.chat_sticker);adjustViewBounds=true}
            bubble.addView(image,LinearLayout.LayoutParams(dp(128),dp(128)))
            Glide.with(image).load(MediaImages.model(message.content)).signature(ObjectKey(currentUserId)).fitCenter().into(image)
        }else if(link!=null){
            val prep=message.content.trimStart().startsWith("备菜")
            val kind=if(link.kind=="recipe")R.string.chat_recipe else if(prep)R.string.chat_prep else R.string.chat_order
            val icon=if(link.kind=="recipe")R.drawable.ic_book_outline else if(prep)R.drawable.ic_ui_prep else R.drawable.ic_ui_order
            val accent=if(link.kind=="recipe")Ui.blue else c.getColor(if(prep)R.color.message_group_icon else R.color.message_reminder_icon)
            val tint=if(link.kind=="recipe")Ui.pale else c.getColor(if(prep)R.color.message_group_bg else R.color.message_reminder_bg)
            bubble.background=GradientDrawable().apply{setColor(Ui.surface);cornerRadius=dp(16).toFloat();setStroke(dp(1),c.getColor(R.color.chat_card_outline))}
            val header=Ui.row(c).apply{
                setPadding(dp(8),dp(4),dp(8),dp(4))
                background=GradientDrawable().apply{setColor(tint);cornerRadius=dp(8).toFloat()}
            }
            header.addView(ImageView(c).apply{setImageResource(icon);imageTintList=ColorStateList.valueOf(accent);importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO},LinearLayout.LayoutParams(dp(18),dp(18)).apply{marginEnd=dp(5)})
            header.addView(Ui.text(c,c.getString(kind),12f,true).apply{setTextColor(accent);setPadding(0,0,0,0)},LinearLayout.LayoutParams(-2,-2))
            bubble.addView(header,LinearLayout.LayoutParams(-2,-2))
            val lines=message.content.replace(ChatContent.linkPattern,"").trim().lines().filter{it.isNotBlank()}
            val title=lines.firstOrNull().orEmpty().removePrefix("菜谱：").removePrefix("点菜：").removePrefix("备菜：").ifBlank{c.getString(kind)}
            bubble.addView(Ui.text(c,title,17f,true).apply{maxLines=2;ellipsize=android.text.TextUtils.TruncateAt.END;setPadding(0,dp(8),0,dp(6))},LinearLayout.LayoutParams(-1,-2))
            if(link.kind=="meal" && lines.size>1){
                if(lines.size>2)bubble.addView(Ui.text(c,lines[1],12f).apply{setTextColor(Ui.muted);maxLines=2;ellipsize=android.text.TextUtils.TruncateAt.END;setPadding(0,0,0,dp(6))},LinearLayout.LayoutParams(-1,-2))
                val dishes=lines.last().split('、').filter{it.isNotBlank()}
                dishes.take(3).forEachIndexed{index,dish->
                    val dishRow=Ui.row(c).apply{gravity=Gravity.TOP}
                    dishRow.addView(Ui.text(c,"${index+1}.",13f).apply{setTextColor(accent);setPadding(0,dp(3),dp(5),0)},LinearLayout.LayoutParams(-2,-2))
                    dishRow.addView(Ui.text(c,dish,15f).apply{maxLines=2;ellipsize=android.text.TextUtils.TruncateAt.END;setPadding(0,dp(2),0,dp(2))},LinearLayout.LayoutParams(0,-2,1f))
                    bubble.addView(dishRow,LinearLayout.LayoutParams(-1,-2))
                }
                if(dishes.size>3)bubble.addView(Ui.text(c,c.getString(R.string.social_dish_total,dishes.size),12f).apply{setTextColor(Ui.muted);setPadding(0,dp(4),0,0)},LinearLayout.LayoutParams(-1,-2))
            }
            val divider=View(c).apply{setBackgroundColor(Ui.outline);alpha=0.5f}
            bubble.addView(divider,LinearLayout.LayoutParams(-1,dp(1)).apply{topMargin=dp(8);bottomMargin=dp(6)})
            val footer=Ui.row(c)
            footer.addView(Ui.text(c,c.getString(if(link.kind=="meal")R.string.social_open_meal else R.string.social_open_recipe),13f,true).apply{setTextColor(accent);setPadding(0,0,dp(4),0)},LinearLayout.LayoutParams(0,-2,1f))
            footer.addView(Ui.text(c,"›",20f).apply{setTextColor(accent);setPadding(0,0,0,0)})
            bubble.addView(footer,LinearLayout.LayoutParams(-1,-2))
            bubble.isFocusable=true
            bubble.setOnClickListener{
                if(message.id==null)pendingAction(message)
                else if(link.kind=="meal")c.startActivity(Intent(c,MealEditorActivity::class.java).putExtra("meal_id",link.id))
                else c.startActivity(Intent(c,RecipeDetailActivity::class.java).putExtra("recipe_id",link.id))
            }
        }else{
            val styled=ChatMentions.decode(message.content)
            styled.getSpans(0,styled.length,MentionSpan::class.java).forEach{span->styled.setSpan(ForegroundColorSpan(if(sent)Ui.onBlue else Ui.blue),styled.getSpanStart(span),styled.getSpanEnd(span),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)}
            val text=Ui.text(c,"",17f).apply{setPadding(0,0,0,0);setTextColor(foreground);setLinkTextColor(if(sent)Ui.onBlue else Ui.blue);setLineSpacing(dp(3).toFloat(),1f);this.text=styled}
            Linkify.addLinks(text,Linkify.WEB_URLS);text.movementMethod=LinkMovementMethod.getInstance();bubble.addView(text)
            text.setOnLongClickListener{copyOrRetry(c,message);true}
        }
        bubble.setOnLongClickListener{copyOrRetry(c,message);true};column.addView(bubble,LinearLayout.LayoutParams(if(link!=null)-1 else -2,-2))
        message.translation?.takeIf{it.status=="ready" && it.language==com.familyrecipes.android.data.remote.ApiClient.contentLanguage()}?.text?.takeIf{it.isNotBlank()}?.let{translation->
            column.addView(Ui.text(c,translation,14f).apply{text=ChatMentions.decode(translation);setTextColor(Ui.muted);setPadding(dp(3),dp(5),dp(3),0)})
        }
        if(ChatContent.mentions(message.content,currentUserId) && !sent)column.addView(Ui.text(c,c.getString(R.string.chat_mentioned),13f,true).apply{setTextColor(Ui.blue);setPadding(0,dp(4),0,0)})
        if(message.id==null){
            column.addView(Ui.text(c,(if(message.localError==null)"◷ " else "❗ ")+c.getString(if(message.localError==null)R.string.chat_pending else R.string.chat_retry),13f).apply{
                setTextColor(Ui.muted);minimumHeight=dp(40);setPadding(0,dp(4),0,0);setOnClickListener{pendingAction(message)}
            })
        }
        if(!sent)row.addView(avatar,LinearLayout.LayoutParams(dp(36),dp(36)))
        // Reserve the avatar first; content receives only the remaining real viewport width.
        row.addView(column,LinearLayout.LayoutParams(0,-2,1f).apply{marginStart=dp(7);marginEnd=dp(7)})
        if(sent)row.addView(avatar,LinearLayout.LayoutParams(dp(36),dp(36)))
        root.addView(row)
    }
    private fun copyOrRetry(c:Context,message:Message){
        if(message.id==null){pendingAction(message);return}
        androidx.appcompat.app.AlertDialog.Builder(c).setItems(arrayOf(c.getString(R.string.social_copy))){_,_->
            (c.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("",ChatContent.plain(message.content)))
        }.show()
    }
}
