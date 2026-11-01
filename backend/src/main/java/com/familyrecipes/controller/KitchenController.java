package com.familyrecipes.controller;
import com.familyrecipes.common.Result;
import com.familyrecipes.entity.Kitchen;
import com.familyrecipes.service.KitchenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/kitchens")
public class KitchenController {
    @Autowired private KitchenService service;
    @GetMapping public Result<List<Kitchen>> list(HttpServletRequest r) {
        return Result.success(service.list((Long)r.getAttribute("userId")));
    }
    @PostMapping public Result<Kitchen> create(@RequestBody Kitchen k,HttpServletRequest r) {
        return Result.success(service.create(k.getGroupId(),k.getName(),(Long)r.getAttribute("userId")));
    }
    @PutMapping("/{id}") public Result<Kitchen> rename(@PathVariable Long id,@RequestBody Kitchen k,HttpServletRequest r) {
        return Result.success(service.rename(id,k.getName(),(Long)r.getAttribute("userId")));
    }
}
