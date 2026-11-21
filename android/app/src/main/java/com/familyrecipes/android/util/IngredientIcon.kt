package com.familyrecipes.android.util
import android.graphics.*
import android.graphics.drawable.Drawable

object IngredientIcon {
    fun drawable(name:String,category:String):Drawable {
        val value=name.lowercase()
        fun has(vararg terms:String)=terms.any{term->if(term.any{it.code>127})value.contains(term) else Regex("\\b"+Regex.escape(term)+"s?\\b").containsMatchIn(value)}
        val emoji=when {
            has("菠萝","凤梨","pineapple")->"🍍"
            has("茄子","eggplant","aubergine")->"🍆"
            has("牛奶","milk","酸奶","yogurt")->"🥛"
            has("鸡蛋","egg")->"🥚"
            has("胡萝卜","carrot")->"🥕"
            has("番茄","西红柿","tomato")->"🍅"
            has("土豆","potato")->"🥔"
            has("苹果","apple")->"🍎"
            has("米饭","大米","rice")->"🍚"
            category.contains("肉")||category.contains("Meat",true)->"🥩"
            category.contains("水产")||category.contains("Seafood",true)->"🐟"
            category.contains("水果")||category.contains("Fruit",true)->"🍊"
            category.contains("调味")||category.contains("Condiment",true)->"🧂"
            category.contains("饮品")||category.contains("Beverage",true)->"🧃"
            category.contains("蔬菜")||category.contains("Vegetable",true)->"🥬"
            category.contains("米面")||category.contains("Grain",true)->"🍞"
            category.contains("蛋奶")||category.contains("Dairy",true)->"🧀"
            else->"🥣"
        }
        return object:Drawable(){val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply{textAlign=Paint.Align.CENTER}
            override fun draw(canvas:Canvas){paint.textSize=bounds.height()*0.78f;val y=bounds.exactCenterY()-(paint.ascent()+paint.descent())/2;canvas.drawText(emoji,bounds.exactCenterX(),y,paint)}
            override fun setAlpha(alpha:Int){paint.alpha=alpha};override fun setColorFilter(filter:ColorFilter?){paint.colorFilter=filter};@Deprecated("Deprecated in Java") override fun getOpacity()=PixelFormat.TRANSLUCENT
        }
    }
}
