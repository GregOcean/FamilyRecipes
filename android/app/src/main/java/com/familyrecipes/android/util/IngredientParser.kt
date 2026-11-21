package com.familyrecipes.android.util

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.text.Normalizer

/** Syntax only: never guess an ingredient identity from a substring or truncate its name. */
object IngredientParser {
    data class ParseResult(
        val ingredientName:String?, val amount:String?, val expiryDays:Int,
        val storageLocation:String="冷藏", val notes:String,
        val ownerUserId:Long?=null,val categoryId:Long?=null,
        val ingredientId:Long?=null,val expiryDate:String?=null,
        val explicitExpiry:Boolean=false
    )
    fun parse(input:String):ParseResult = parse(input,LocalDate.now())
    fun parse(input:String,today:LocalDate):ParseResult {
        var text=Normalizer.normalize(input,Normalizer.Form.NFKC).trim().replace(Regex("^(?:[-*•]\\s*|\\d+[.、)]\\s+)"),"")
        val absolute=Regex("(?<!\\d)(20\\d{2})[年/.-](\\d{1,2})[月/.-](\\d{1,2})日?(?!\\d)").find(text)
        var date:LocalDate?=null
        if(absolute!=null){
            date=runCatching{LocalDate.of(absolute.groupValues[1].toInt(),absolute.groupValues[2].toInt(),absolute.groupValues[3].toInt())}.getOrNull()
            text=withoutTime(text,absolute.range)
        }else{
            val relative=Regex("(\\d+|一|二|两|三|四|五|六|七|八|九|十)\\s*个?(个月|月|周|星期|天|days?|weeks?|months?)",RegexOption.IGNORE_CASE).find(text)
            if(relative!=null){
                val n=relative.groupValues[1].toIntOrNull()?:mapOf("一" to 1,"二" to 2,"两" to 2,"三" to 3,"四" to 4,"五" to 5,"六" to 6,"七" to 7,"八" to 8,"九" to 9,"十" to 10)[relative.groupValues[1]]?:0
                val unit=relative.groupValues[2].lowercase()
                val days=n.toLong()*when{unit.contains("月")||unit.startsWith("month")->30;unit in listOf("周","星期")||unit.startsWith("week")->7;else->1}
                if(days in 0..3650)date=today.plusDays(days)
                text=withoutTime(text,relative.range)
            }
        }
        var name=text;var amount:String?=null
        if(text.contains('|')){val p=text.split('|',limit=2);name=p[0].trim();amount=p[1].trim().ifBlank{null}}
        else {
            val pattern=Regex("((?:\\d+\\s+)?\\d+[／/⁄]\\d+|\\d+(?:\\.\\d+)?|[一二两三四五六七八九十])\\s*(千克|公斤|毫升|cartons?|bottles?|packs?|pieces?|litres?|liters?|tablespoons?|teaspoons?|cups?|tbsp|tsp|oz|lbs?|汤匙|茶匙|kg|ml|个|只|条|根|片|块|颗|粒|枚|袋|盒|包|瓶|罐|桶|箱|斤|两|克|升|杯|勺|把|瓣|g|l)(?![a-z])",RegexOption.IGNORE_CASE)
            val found=pattern.findAll(text).toList()
            if(found.size==1){amount=found[0].value;name=text.removeRange(found[0].range).trim()}
        }
        if(amount==null){val optional=Regex("\\s*(适量|少许|to taste)\\s*$",RegexOption.IGNORE_CASE).find(name);if(optional!=null){amount=optional.groupValues[1];name=name.substring(0,optional.range.first).trim()}}
        name=name.trim(' ',',','，',';','；')
        return ParseResult(name.ifBlank{null},amount,date?.let{ChronoUnit.DAYS.between(today,it).toInt()}?:0,
            "冷藏",input,expiryDate=date?.toString(),explicitExpiry=date!=null)
    }
    private fun withoutTime(text:String,range:IntRange):String {
        val before=text.substring(0,range.first).replace(Regex("(?:还有|保质期|expires?\\s*(?:in|on)?|best before)\\s*$",RegexOption.IGNORE_CASE),"")
        val after=text.substring(range.last+1).replace(Regex("^\\s*(?:后|内要)?\\s*(?:过期|到期|吃完|用完)?",RegexOption.IGNORE_CASE),"")
        return (before+" "+after).trim()
    }
    fun calculateExpiryDate(days:Int):String=LocalDate.now().plusDays(days.toLong()).toString()
}
