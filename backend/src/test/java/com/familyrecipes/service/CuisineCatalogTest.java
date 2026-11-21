package com.familyrecipes.service;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CuisineCatalogTest {
 @Test void indiaIsSeparateWithoutGuessingBroadSouthAsianTags() {
  assertTrue(CuisineCatalog.expand("印度菜").containsAll(java.util.List.of("IN","印度","India","Indian")));
  assertFalse(CuisineCatalog.expand("印度菜").contains("南亚"));
  assertFalse(CuisineCatalog.expand("南亚").contains("印度菜"));
  assertFalse(CuisineCatalog.expand("南亚").contains("IN"));
  assertTrue(CuisineCatalog.labels().contains("印度菜"));
  assertEquals("India",CuisineCatalog.label("IN",true));
 }
 @Test void keepsRegionalAndLegacyLabelsConsistent() {
  assertTrue(CuisineCatalog.expand("region:china").contains("云南菜"));
  assertTrue(CuisineCatalog.expand("region:china-eight").contains("四川菜"));
  assertFalse(CuisineCatalog.expand("region:china-eight").contains("云南菜"));
  assertTrue(CuisineCatalog.expand("北美").contains("墨西哥菜"));
  assertTrue(CuisineCatalog.expand("region:europe").contains("RU"));
  assertEquals("Japan",CuisineCatalog.label("JP",true));
  assertEquals("美洲",CuisineCatalog.label("美式",false));
  assertFalse(CuisineCatalog.labels().contains("region:china"));
  assertEquals(java.util.List.of("custom"),CuisineCatalog.expand("custom"));
 }
 @Test void v3GroupsOldLabelsAndKeepsHomeStyleIndependent() {
  assertTrue(CuisineCatalog.expand("东北菜").containsAll(java.util.List.of("吉林菜","辽宁菜","黑龙江菜")));
  assertTrue(CuisineCatalog.expand("西北菜").containsAll(java.util.List.of("甘肃菜","青海菜","宁夏菜")));
  assertTrue(CuisineCatalog.expand("region:china-provinces").containsAll(java.util.List.of("港式","澳门菜","台湾菜")));
  assertTrue(CuisineCatalog.expand("西欧").contains("法国菜"));
  assertTrue(CuisineCatalog.expand("东欧").contains("RU"));
  assertTrue(CuisineCatalog.expand("大洋洲").contains("澳洲"));
  assertFalse(CuisineCatalog.expand("region:china").contains("家常菜"));
  assertEquals("家常菜",CuisineCatalog.tree(false).get(0).get("value"));
 }
}
