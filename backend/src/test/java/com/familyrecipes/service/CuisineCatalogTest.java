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
  assertEquals("北美",CuisineCatalog.label("美式",false));
  assertFalse(CuisineCatalog.labels().contains("region:china"));
  assertEquals(java.util.List.of("custom"),CuisineCatalog.expand("custom"));
 }
}
