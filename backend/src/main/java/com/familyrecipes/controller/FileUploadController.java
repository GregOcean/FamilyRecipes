package com.familyrecipes.controller;

import com.familyrecipes.common.Result;
import com.familyrecipes.common.ApiException;
import com.familyrecipes.service.MediaService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {
    @Autowired private MediaService media;
    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file, @RequestParam(defaultValue="other") String category, HttpServletRequest request) throws IOException {
        return Result.success(media.upload(file, (Long) request.getAttribute("userId"), category));
    }
    @PostMapping("/images")
    public Result<String[]> uploadImages(@RequestParam("files") MultipartFile[] files, HttpServletRequest request) throws IOException {
        if (files.length < 1 || files.length > 9) throw new ApiException(400, "每次上传1至9张图片");
        String[] urls = new String[files.length];
        for (int i = 0; i < files.length; i++) urls[i] = media.upload(files[i], (Long) request.getAttribute("userId"));
        return Result.success(urls);
    }
    @GetMapping("/files/{filename}")
    public ResponseEntity<Resource> image(@PathVariable String filename, HttpServletRequest request) {
        Resource resource = new FileSystemResource(media.read(filename, (Long) request.getAttribute("userId")));
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG)
            .header("X-Content-Type-Options", "nosniff")
            .cacheControl(CacheControl.noStore()).body(resource);
    }

    @GetMapping("/trash") public Result<java.util.List<java.util.Map<String,Object>>> trash(HttpServletRequest r) {
        return Result.success(media.trash((Long)r.getAttribute("userId")));
    }
    @DeleteMapping("/files/{filename}") public Result<Void> trashFile(@PathVariable String filename,HttpServletRequest r) {
        media.changeTrash(filename,(Long)r.getAttribute("userId"),false);return Result.success();
    }
    @PostMapping("/files/{filename}/restore") public Result<Void> restore(@PathVariable String filename,HttpServletRequest r) {
        media.changeTrash(filename,(Long)r.getAttribute("userId"),true);return Result.success();
    }
}
