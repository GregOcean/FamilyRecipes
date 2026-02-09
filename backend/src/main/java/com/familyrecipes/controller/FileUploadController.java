package com.familyrecipes.controller;

import com.familyrecipes.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 文件上传控制器
 */
@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.base-url}")
    private String baseUrl;

    /**
     * 上传图片
     */
    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            // 验证文件
            if (file.isEmpty()) {
                return Result.error("文件为空");
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                return Result.error("文件名无效");
            }

            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return Result.error("只能上传图片文件");
            }

            // 创建上传目录
            File uploadDirectory = new File(uploadDir);
            if (!uploadDirectory.exists()) {
                uploadDirectory.mkdirs();
            }

            // 生成唯一文件名
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String filename = UUID.randomUUID().toString() + extension;
            Path filePath = Paths.get(uploadDir, filename);

            // 保存文件
            Files.copy(file.getInputStream(), filePath);

            // 返回文件URL
            String fileUrl = baseUrl + filename;
            return Result.success(fileUrl);

        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 批量上传图片
     */
    @PostMapping("/images")
    public Result<String[]> uploadImages(@RequestParam("files") MultipartFile[] files) {
        try {
            String[] urls = new String[files.length];
            
            for (int i = 0; i < files.length; i++) {
                Result<String> result = uploadImage(files[i]);
                if (result.getCode() != 200) {
                    return Result.error("第" + (i + 1) + "个文件上传失败：" + result.getMessage());
                }
                urls[i] = result.getData();
            }
            
            return Result.success(urls);
        } catch (Exception e) {
            return Result.error("批量上传失败：" + e.getMessage());
        }
    }
}

