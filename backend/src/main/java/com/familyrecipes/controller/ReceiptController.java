package com.familyrecipes.controller;
import com.familyrecipes.ai.ReceiptService;
import com.familyrecipes.common.ApiException;
import com.familyrecipes.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/receipts")
public class ReceiptController {
    public record TextRequest(Long kitchenId,String text,String mode) {}
    private final ReceiptService receipts;
    public ReceiptController(ReceiptService receipts){this.receipts=receipts;}
    @PostMapping(value="/recognize",consumes="multipart/form-data")
    public Result<List<ReceiptService.Item>> recognize(@RequestParam Long kitchenId,@RequestPart("image") MultipartFile image,@RequestParam(defaultValue="receipt") String mode,HttpServletRequest r){
        return Result.success(receipts.recognize((Long)r.getAttribute("userId"),kitchenId,image,mode));
    }
    @PostMapping("/recognize-text")
    public Result<List<ReceiptService.Item>> recognizeText(@RequestBody TextRequest request,HttpServletRequest r){
        if(request==null)throw new ApiException(400,"请求不能为空 / Request is required");
        return Result.success(receipts.recognizeText((Long)r.getAttribute("userId"),request.kitchenId(),request.text(),request.mode()==null?"receipt":request.mode()));
    }
    @PostMapping("/import")
    public Result<Integer> save(@RequestBody ReceiptService.SaveRequest request,HttpServletRequest r)throws Exception{
        return Result.success(receipts.save((Long)r.getAttribute("userId"),request));
    }
}
