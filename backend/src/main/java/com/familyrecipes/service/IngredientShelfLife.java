package com.familyrecipes.service;

import java.util.Map;

/** Approximate unopened storage planning, not a replacement for package expiry dates. */
public final class IngredientShelfLife {
    private IngredientShelfLife() {}
    public static String storage(String s) {
        if(s==null)return null;String v=s.toLowerCase(java.util.Locale.ROOT);
        if(v.contains("冷冻")||v.contains("freez")||v.contains("frozen"))return "冷冻";
        if(v.contains("冷藏")||v.contains("冰箱")||v.contains("fridge")||v.contains("refrigerat"))return "冷藏";
        if(v.contains("常温")||v.contains("room")||v.contains("pantry"))return "常温";
        return null;
    }
    public static Integer estimate(String name,String location) {
        String storage=storage(location);if(name==null||storage==null)return null;
        if(storage.equals("冷藏")) return Map.ofEntries(
            Map.entry("猪肉",3),Map.entry("牛肉",3),Map.entry("鸡肉",3),Map.entry("鸡胸肉",3),Map.entry("鸡腿",3),Map.entry("鱼",2),Map.entry("虾",2),
            Map.entry("鸡蛋",21),Map.entry("牛奶",7),Map.entry("酸奶",7),Map.entry("豆腐",3),Map.entry("番茄",7),Map.entry("胡萝卜",10),Map.entry("生菜",3),Map.entry("菠菜",3)).get(name);
        if(storage.equals("冷冻") && java.util.Set.of("猪肉","牛肉","鸡肉","鸡胸肉","鸡腿","鱼","虾").contains(name))return 30;
        if(storage.equals("常温"))return Map.of("大米",180,"面粉",90,"土豆",14,"洋葱",14).get(name);
        return null;
    }
}
