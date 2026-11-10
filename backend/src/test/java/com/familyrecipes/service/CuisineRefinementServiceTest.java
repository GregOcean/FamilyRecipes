package com.familyrecipes.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class CuisineRefinementServiceTest {
 @Test void onlyCoarseOrUnmappedTagsNeedModelCalls() {
  assertTrue(CuisineRefinementService.eligible(List.of("东北菜")));
  assertTrue(CuisineRefinementService.eligible(List.of("unknown-country")));
  assertFalse(CuisineRefinementService.eligible(List.of("待确认")));
  assertFalse(CuisineRefinementService.eligible(List.of("IN")));
  assertFalse(CuisineRefinementService.eligible(List.of("东北菜","辽宁菜")));
  assertFalse(CuisineRefinementService.eligible(List.of("美式")));
 }
 @Test void refinementCannotContradictKnownRegion() {
  assertTrue(CuisineRefinementService.compatible(List.of("东北菜"),"辽宁菜"));
  assertFalse(CuisineRefinementService.compatible(List.of("东北菜"),"粤菜"));
  assertTrue(CuisineRefinementService.compatible(List.of("南亚"),"印度菜"));
  assertFalse(CuisineRefinementService.compatible(List.of("南亚"),"泰式"));
  assertFalse(CuisineRefinementService.compatible(List.of("其他"),"其他"));
 }
 @Test void outputIsBoundedToCurrentTaxonomy() throws Exception {
  var json=new ObjectMapper();
  assertEquals("印度菜",CuisineRefinementService.parse("{\"cuisine\":[\"印度菜\"],\"evidence\":\"test\"}",json).path("cuisine").get(0).asText());
  assertThrows(Exception.class,()->CuisineRefinementService.parse("{\"cuisine\":[\"IN\"]}",json));
  assertThrows(Exception.class,()->CuisineRefinementService.parse("{\"cuisine\":[\"印度菜\",\"印度菜\"]}",json));
 }
}
