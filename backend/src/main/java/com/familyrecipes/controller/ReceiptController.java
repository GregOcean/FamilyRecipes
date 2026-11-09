package com.familyrecipes.controller;
import com.familyrecipes.ai.ReceiptService;
import com.familyrecipes.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/receipts")
public class ReceiptController {
    private final ReceiptService receipts;
    public ReceiptController(ReceiptService receipts){this.receipts=receipts;}
    @PostMapping(value="/recognize",consumes="multipart/form-data")
    public Result<List<ReceiptService.Item>> recognize(@RequestParam Long kitchenId,@RequestPart("image") MultipartFile image,HttpServletRequest r){
        return Result.success(receipts.recognize((Long)r.getAttribute("userId"),kitchenId,image));
    }
    @PostMapping("/import")
    public Result<Integer> save(@RequestBody ReceiptService.SaveRequest request,HttpServletRequest r)throws Exception{
        return Result.success(receipts.save((Long)r.getAttribute("userId"),request));
    }
}
