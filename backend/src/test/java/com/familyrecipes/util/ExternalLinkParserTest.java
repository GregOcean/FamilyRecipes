package com.familyrecipes.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ExternalLinkParser测试类
 */
public class ExternalLinkParserTest {
    
    @Test
    public void testParseXiaohongshu() {
        String input = "这种包子是泡打粉发的面吗？ 自家做的包子，面皮永远... http://xhslink.com/o/42iB8crbRgI \n" +
                      "复制后打开【小红书】查看笔记！";
        
        ExternalLinkParser.ParseResult result = ExternalLinkParser.parse(input);
        
        assertNotNull(result);
        assertEquals("https://xhslink.com/o/42iB8crbRgI", result.getUrl());
        assertEquals("小红书", result.getSource());
        assertTrue(result.getTitle().contains("包子") || result.getTitle().contains("泡打粉"));
    }
    
    @Test
    public void testParseXiachufang() {
        String input = "https://www.xiachufang.com/recipe/107661996/";
        
        ExternalLinkParser.ParseResult result = ExternalLinkParser.parse(input);
        
        assertNotNull(result);
        assertEquals("https://www.xiachufang.com/recipe/107661996/", result.getUrl());
        assertEquals("下厨房", result.getSource());
    }
    
    @Test
    public void testParseDouyin() {
        String input = "3.35 复制打开抖音，看看【四全丈夫的作品】保姆级卤牛肉的教程，一步不拉，酱味入心，口感不柴。... https://v.douyin.com/rE4SXmayYJA/ 07/19 EHv:/ O@x.FH";
        
        ExternalLinkParser.ParseResult result = ExternalLinkParser.parse(input);
        
        assertNotNull(result);
        assertEquals("https://v.douyin.com/rE4SXmayYJA/", result.getUrl());
        assertEquals("抖音", result.getSource());
        assertTrue(result.getTitle().contains("卤牛肉") || result.getTitle().contains("保姆级"));
    }
    
    @Test
    public void testParseBilibili() {
        String input = "【隋卞一做 |嚯！好烂的牛肉！-哔哩哔哩】 https://b23.tv/T12xxOU";
        
        ExternalLinkParser.ParseResult result = ExternalLinkParser.parse(input);
        
        assertNotNull(result);
        assertEquals("https://b23.tv/T12xxOU", result.getUrl());
        assertEquals("哔哩哔哩", result.getSource());
        assertTrue(result.getTitle().contains("隋卞一做") || result.getTitle().contains("牛肉"));
    }
    
    @Test
    public void testParseInvalidInput() {
        String input = "这是一段没有链接的文本";
        
        ExternalLinkParser.ParseResult result = ExternalLinkParser.parse(input);
        
        assertNull(result);
    }
    
    @Test
    public void testParseEmptyInput() {
        ExternalLinkParser.ParseResult result = ExternalLinkParser.parse("");
        assertNull(result);
        
        result = ExternalLinkParser.parse(null);
        assertNull(result);
    }
}

