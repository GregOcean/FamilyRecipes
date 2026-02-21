package com.familyrecipes.service;

import com.familyrecipes.mapper.InventoryCategoryMapper;
import com.familyrecipes.model.InventoryCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 库存分类服务
 */
@Slf4j
@Service
public class InventoryCategoryService {
    
    @Autowired
    private InventoryCategoryMapper inventoryCategoryMapper;
    
    /**
     * 获取分类树（一级分类及其子分类）
     */
    public List<InventoryCategory> getCategoryTree() {
        // 获取所有一级分类
        List<InventoryCategory> topLevelCategories = inventoryCategoryMapper.getTopLevelCategories();
        
        // 为每个一级分类加载子分类
        for (InventoryCategory category : topLevelCategories) {
            List<InventoryCategory> children = inventoryCategoryMapper.getChildCategories(category.getId());
            category.setChildren(children);
        }
        
        return topLevelCategories;
    }
    
    /**
     * 获取所有分类（扁平列表）
     */
    public List<InventoryCategory> getAllCategories() {
        return inventoryCategoryMapper.getAllCategories();
    }
    
    /**
     * 根据ID获取分类
     */
    public InventoryCategory getCategoryById(Long id) {
        return inventoryCategoryMapper.getCategoryById(id);
    }
    
    /**
     * 创建分类
     */
    public InventoryCategory createCategory(InventoryCategory category) {
        // 如果没有指定排序，自动设置
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        
        inventoryCategoryMapper.insertCategory(category);
        return category;
    }
    
    /**
     * 更新分类
     */
    public void updateCategory(InventoryCategory category) {
        inventoryCategoryMapper.updateCategory(category);
    }
    
    /**
     * 删除分类
     */
    public void deleteCategory(Long id) {
        // 检查是否有子分类
        List<InventoryCategory> children = inventoryCategoryMapper.getChildCategories(id);
        if (!children.isEmpty()) {
            throw new RuntimeException("该分类下有子分类，无法删除");
        }
        
        inventoryCategoryMapper.deleteCategory(id);
    }
    
    /**
     * 智能匹配分类（基于关键词匹配）
     * 未来可以替换为 LLM
     */
    public Long smartMatchCategory(String itemName) {
        if (itemName == null || itemName.trim().isEmpty()) {
            return null;
        }
        
        String name = itemName.toLowerCase();
        
        // 蔬菜关键词
        String[] vegetables = {"白菜", "菠菜", "生菜", "番茄", "西红柿", "土豆", "萝卜", "黄瓜", "茄子", 
                              "青椒", "辣椒", "葱", "姜", "蒜", "芹菜", "韭菜", "香菜", "豆角", "豌豆"};
        if (containsAny(name, vegetables)) {
            return getCategoryIdByPath("原料", "蔬菜");
        }
        
        // 肉类关键词
        String[] meats = {"猪肉", "牛肉", "羊肉", "鸡肉", "鸭肉", "肉", "排骨", "鸡蛋", "鸡翅", "五花肉"};
        if (containsAny(name, meats)) {
            return getCategoryIdByPath("原料", "肉类");
        }
        
        // 海鲜关键词
        String[] seafood = {"鱼", "虾", "蟹", "贝", "海鲜", "三文鱼", "鲈鱼", "带鱼", "墨鱼", "章鱼", "扇贝"};
        if (containsAny(name, seafood)) {
            return getCategoryIdByPath("原料", "海鲜");
        }
        
        // 水果关键词
        String[] fruits = {"苹果", "香蕉", "橙", "橘", "梨", "葡萄", "西瓜", "草莓", "芒果", "樱桃", "桃子", "柚子"};
        if (containsAny(name, fruits)) {
            return getCategoryIdByPath("原料", "水果");
        }
        
        // 饮品关键词
        String[] drinks = {"奶茶", "可乐", "雪碧", "啤酒", "红酒", "白酒", "咖啡", "茶", "果汁", "牛奶", "酸奶", "豆奶"};
        if (containsAny(name, drinks)) {
            // 进一步细分
            if (containsAny(name, new String[]{"啤酒", "红酒", "白酒", "伏特加", "威士忌"})) {
                return getCategoryIdByPath("饮品", "酒精");
            } else if (containsAny(name, new String[]{"茶", "绿茶", "红茶", "奶茶"})) {
                return getCategoryIdByPath("饮品", "茶饮");
            } else if (containsAny(name, new String[]{"可乐", "雪碧", "芬达", "汽水"})) {
                return getCategoryIdByPath("饮品", "汽水");
            } else if (containsAny(name, new String[]{"牛奶", "酸奶", "豆奶"})) {
                return getCategoryIdByPath("饮品", "乳制品");
            }
            // 默认返回饮品一级分类
            return getCategoryIdByName("饮品", null);
        }
        
        // 面点关键词
        String[] noodles = {"面", "包", "饺子", "馒头", "面包", "蛋糕", "饼干", "馄饨", "面条", "拉面", "米粉"};
        if (containsAny(name, noodles)) {
            if (containsAny(name, new String[]{"面包", "蛋糕", "饼干", "曲奇", "派"})) {
                return getCategoryIdByPath("面点", "西式");
            } else {
                return getCategoryIdByPath("面点", "中式");
            }
        }
        
        // 调味料关键词
        String[] condiments = {"盐", "糖", "酱油", "醋", "油", "味精", "鸡精", "料酒", "蚝油", "辣椒酱", "番茄酱"};
        if (containsAny(name, condiments)) {
            return getCategoryIdByPath("调味料", "酱料");
        }
        
        // 零食关键词
        String[] snacks = {"薯片", "爆米花", "糖果", "巧克力", "饼干", "坚果", "瓜子", "花生", "核桃"};
        if (containsAny(name, snacks)) {
            if (containsAny(name, new String[]{"坚果", "瓜子", "花生", "核桃", "杏仁", "腰果"})) {
                return getCategoryIdByPath("零食", "坚果");
            } else if (containsAny(name, new String[]{"糖", "巧克力", "软糖"})) {
                return getCategoryIdByPath("零食", "糖果");
            } else {
                return getCategoryIdByPath("零食", "膨化");
            }
        }
        
        // 默认返回"其他"分类
        return getCategoryIdByName("其他", null);
    }
    
    /**
     * 检查字符串是否包含任一关键词
     */
    private boolean containsAny(String text, String[] keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 根据分类路径获取分类ID
     */
    private Long getCategoryIdByPath(String parentName, String childName) {
        List<InventoryCategory> allCategories = inventoryCategoryMapper.getAllCategories();
        
        // 先找父分类
        InventoryCategory parent = allCategories.stream()
                .filter(c -> c.getParentId() == null && c.getName().equals(parentName))
                .findFirst()
                .orElse(null);
        
        if (parent == null) {
            return null;
        }
        
        // 如果没有指定子分类，返回父分类ID
        if (childName == null) {
            return parent.getId();
        }
        
        // 找子分类
        InventoryCategory child = allCategories.stream()
                .filter(c -> c.getParentId() != null && 
                            c.getParentId().equals(parent.getId()) && 
                            c.getName().equals(childName))
                .findFirst()
                .orElse(null);
        
        return child != null ? child.getId() : parent.getId();
    }
    
    /**
     * 根据名称获取分类ID
     */
    private Long getCategoryIdByName(String name, Long parentId) {
        List<InventoryCategory> categories = inventoryCategoryMapper.getAllCategories();
        return categories.stream()
                .filter(c -> c.getName().equals(name) && 
                            (parentId == null ? c.getParentId() == null : c.getParentId().equals(parentId)))
                .findFirst()
                .map(InventoryCategory::getId)
                .orElse(null);
    }
}

