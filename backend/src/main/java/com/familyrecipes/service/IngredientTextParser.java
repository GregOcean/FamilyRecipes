package com.familyrecipes.service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Locale;
import java.util.regex.Pattern;

/** Lossless, deterministic syntax parsing. Food identity is resolved separately. */
public final class IngredientTextParser {
    private IngredientTextParser() {}
    public record Parsed(String name,String amount,LocalDate expiryDate,Integer days) {}
    private static final Pattern DATE=Pattern.compile("(?<!\\d)(20\\d{2})[年/.-](\\d{1,2})[月/.-](\\d{1,2})日?(?!\\d)");
    private static final Pattern RELATIVE=Pattern.compile("(?i)(\\d+|一|二|两|三|四|五|六|七|八|九|十)\\s*(?:个)?(个月|月|周|星期|天|days?|weeks?|months?)");
    private static final Pattern AMOUNT=Pattern.compile("(?i)(?<![\\p{L}\\d.])((?:\\d+\\s+)?\\d+[／/⁄]\\d+|\\d+(?:\\.\\d+)?|[一二两三四五六七八九十])\\s*(千克|公斤|毫升|kg|ml|litres?|liters?|cartons?|bottles?|packs?|pieces?|tablespoons?|teaspoons?|cups?|tbsp|tsp|oz|lbs?|汤匙|茶匙|个|只|条|根|片|块|颗|粒|枚|袋|盒|包|瓶|罐|桶|箱|斤|两|克|升|杯|勺|把|瓣|g|l)(?![a-z])");
    // Chinese ingredients have no word boundary before their amount.
    private static final Pattern ZH_AMOUNT=Pattern.compile("(?i)((?:\\d+\\s+)?\\d+[／/⁄]\\d+|\\d+(?:\\.\\d+)?|[一二两三四五六七八九十])\\s*(千克|公斤|毫升|汤匙|茶匙|kg|ml|个|只|条|根|片|块|颗|粒|枚|袋|盒|包|瓶|罐|桶|箱|斤|两|克|升|杯|勺|把|瓣|g|l)(?![a-z])");
    public static Parsed parse(String input,LocalDate today) {
        String text=Normalizer.normalize(input==null?"":input,Normalizer.Form.NFKC).trim().replaceFirst("^(?:[-*•]\\s*|\\d+[.、)]\\s+)","");
        if(text.contains("|")){String[] p=text.split("\\|",2);return new Parsed(p[0].trim(),blank(p[1]),null,null);}
        LocalDate date=null;Integer days=null;
        var absolute=DATE.matcher(text);
        if(absolute.find()) {
            try{date=LocalDate.of(Integer.parseInt(absolute.group(1)),Integer.parseInt(absolute.group(2)),Integer.parseInt(absolute.group(3)));}
            catch(java.time.DateTimeException ignored){}
            text=withoutTime(text,absolute.start(),absolute.end());
        } else {
            var relative=RELATIVE.matcher(text);
            if(relative.find()) {
                int number=number(relative.group(1));String unit=relative.group(2).toLowerCase(Locale.ROOT);
                long total=(long)number*(unit.contains("月")||unit.startsWith("month")?30:unit.equals("周")||unit.equals("星期")||unit.startsWith("week")?7:1);
                if(total>=0 && total<=3650){days=(int)total;date=today.plusDays(days);}
                text=withoutTime(text,relative.start(),relative.end());
            }
        }
        var amountMatcher=(text.matches(".*[\\p{IsHan}].*")?ZH_AMOUNT:AMOUNT).matcher(text);
        String amount=null,name=text;
        if(amountMatcher.find()) {
            int start=amountMatcher.start(),end=amountMatcher.end();String first=amountMatcher.group();
            // Ambiguous multi-food text must remain intact rather than silently retaining one food.
            if(!amountMatcher.find()){amount=first;name=(text.substring(0,start)+text.substring(end)).trim();}
        }
        if(amount==null) {
            var optional=Pattern.compile("(?i)\\s*(适量|少许|to taste)\\s*$").matcher(name);
            if(optional.find()){amount=optional.group(1);name=name.substring(0,optional.start()).trim();}
        }
        name=name.replaceAll("^[,，;；\\s]+|[,，;；\\s]+$","");
        return new Parsed(name,amount,date,days);
    }
    private static String withoutTime(String text,int start,int end) {
        String before=text.substring(0,start).replaceFirst("(?i)(?:还有|保质期|expires?\\s*(?:in|on)?|best before)\\s*$","");
        String after=text.substring(end).replaceFirst("(?i)^\\s*(?:后|内要)?\\s*(?:过期|到期|吃完|用完)?","");
        return (before+" "+after).trim();
    }
    private static String blank(String s){return s.isBlank()?null:s.trim();}
    private static int number(String s){try{return Integer.parseInt(s);}catch(NumberFormatException e){return switch(s){case "一"->1;case "二","两"->2;case "三"->3;case "四"->4;case "五"->5;case "六"->6;case "七"->7;case "八"->8;case "九"->9;case "十"->10;default->0;};}}
}
