package com.familyrecipes.service;

/** Food type, independent of refrigeration, purchase source or sugar content. */
public final class IngredientCategoryRules {
    private IngredientCategoryRules() { }
    public static String suggest(String raw) {
        String name = raw == null ? "" : raw.trim();
        // Compound products take precedence over the raw ingredients in their names.
        if (has(name,"酱油","蚝油","香油","辣椒酱","番茄酱","沙拉酱","花生酱","芝麻酱","料酒","食盐","白糖","味精","鸡精","食用油","橄榄油","醋","胡椒粉","咖喱","豆豉","酵母","泡打粉")) return "调味料";
        if (has(name,"饼干","巧克力","糖果","薯片","爆米花","坚果","核桃","腰果","杏仁","花生","葡萄干")) return "零食坚果";
        if (has(name,"饺子皮","馄饨皮","春卷皮")) return "米面主食";
        if (has(name,"水饺","饺子","馄饨","汤圆","包子","方便面","速食","剩菜","外卖","熟食","午餐肉","鱼丸","牛肉丸","火腿肠","猪肉丸","鸡肉丸","熟火腿")) return "熟食速食";
        if (has(name,"面包","馒头","面条","面粉","米粉","大米","米饭","意面","挂面","年糕","乌冬面","饺子皮","馄饨皮","面筋")) return "米面主食";
        if (has(name,"奶茶","豆奶","豆浆","果汁","可乐","汽水","咖啡","啤酒","红酒","白酒","矿泉水","绿茶","红茶")) return "饮品";
        if (has(name,"牛奶","酸奶","奶酪","奶油","奶粉","炼乳","鸡蛋","鸭蛋","鹌鹑蛋","皮蛋","咸蛋")) return "蛋奶";
        if (has(name,"豆腐","豆干","腐竹","千张","豆皮","素鸡")) return "豆制品";
        if (has(name,"干香菇","干木耳","干银耳","笋干","红豆","绿豆","黄豆","黑豆","鹰嘴豆","燕麦","小米","红枣","枸杞","莲子")) return "干货杂粮";
        if (has(name,"猪肉","牛肉","羊肉","鸡肉","鸭肉","排骨","五花肉","鸡翅","鸡腿","牛腩","里脊","腊肉","培根")) return "肉禽";
        if (has(name,"三文鱼","鲈鱼","带鱼","鲫鱼","鳕鱼","金枪鱼","虾","蟹","扇贝","蛤蜊","鱿鱼","章鱼")) return "水产";
        if (has(name,"白菜","菠菜","生菜","番茄","西红柿","土豆","萝卜","黄瓜","茄子","青椒","芹菜","韭菜","香菇","蘑菇","金针菇","西兰花")) return "蔬菜菌菇";
        if (has(name,"苹果","香蕉","橙子","橘子","草莓","芒果","樱桃","西瓜","葡萄","柚子","桃子")) return "水果";
        return "待分类";
    }
    private static boolean has(String name, String... words) {
        for (String word : words) if (name.contains(word)) return true;
        return false;
    }
}
