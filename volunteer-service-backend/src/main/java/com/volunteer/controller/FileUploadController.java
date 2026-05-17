package com.volunteer.controller;

import com.volunteer.utils.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传控制器
 * 支持两种路径：/api/upload 和 /upload（兼容旧版本）
 */
@RestController
public class FileUploadController {

    @Value("${file.upload-path:/uploads/}")
    private String uploadPath;

    /**
     * 通用文件上传接口 - /api/upload
     */
    @PostMapping("/api/upload")
    public Result uploadFile(@RequestParam("file") MultipartFile file) {
        return doUpload(file);
    }

    /**
     * 通用文件上传接口 - /upload（兼容旧版本）
     */
    @PostMapping("/upload")
    public Result uploadFileLegacy(@RequestParam("file") MultipartFile file) {
        return doUpload(file);
    }

    /**
     * 文件上传核心逻辑
     */
    private Result doUpload(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return Result.error("文件不能为空");
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                return Result.error("文件名无效");
            }

            // 提取扩展名
            int dotIdx = originalFilename.lastIndexOf(".");
            if (dotIdx < 0) {
                return Result.error("无法识别的文件类型");
            }
            String extension = originalFilename.substring(dotIdx).toLowerCase();

            // 只允许图片
            String[] allowed = {".jpg", ".jpeg", ".png", ".gif", ".webp"};
            boolean ok = false;
            for (String ext : allowed) { if (extension.equals(ext)) { ok = true; break; } }
            if (!ok) {
                return Result.error("不支持的文件类型，仅支持 JPG、PNG、GIF、WEBP");
            }

            // 限制 5MB
            if (file.getSize() > 5 * 1024 * 1024) {
                return Result.error("文件大小不能超过 5MB");
            }

            // 生成唯一文件名
            String newFileName = UUID.randomUUID().toString().replace("-", "") + extension;

            // 确保上传目录存在（使用绝对路径）
            File uploadDir = new File(uploadPath);
            if (!uploadDir.isAbsolute()) {
                uploadDir = new File(System.getProperty("user.dir"), uploadPath);
            }
            if (!uploadDir.exists() && !uploadDir.mkdirs()) {
                return Result.error("创建上传目录失败: " + uploadDir.getAbsolutePath());
            }

            // 保存文件
            File destFile = new File(uploadDir, newFileName);
            file.transferTo(destFile);
            System.out.println("文件已上传至: " + destFile.getAbsolutePath());

            // 返回访问路径
            Map<String, Object> result = new HashMap<>();
            result.put("url", "/uploads/" + newFileName);
            result.put("filename", newFileName);
            result.put("originalName", originalFilename);
            result.put("size", file.getSize());

            return Result.success("上传成功", result);

        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 批量文件上传
     */
    @PostMapping("/upload/batch")
    public Result uploadFiles(@RequestParam("files") MultipartFile[] files) {
        try {
            if (files == null || files.length == 0) {
                return Result.error("文件不能为空");
            }

            if (files.length > 10) {
                return Result.error("一次最多上传 10 个文件");
            }

            Map<String, Object>[] results = new Map[files.length];
            int successCount = 0;

            for (int i = 0; i < files.length; i++) {
                MultipartFile file = files[i];
                if (!file.isEmpty()) {
                    String originalFilename = file.getOriginalFilename();
                    String extension = originalFilename != null ? 
                        originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
                    String newFileName = UUID.randomUUID().toString().replace("-", "") + extension;

                    File uploadDir = new File(uploadPath);
                    if (!uploadDir.exists()) {
                        uploadDir.mkdirs();
                    }

                    File destFile = new File(uploadPath + newFileName);
                    file.transferTo(destFile);

                    Map<String, Object> fileInfo = new HashMap<>();
                    fileInfo.put("url", "/uploads/" + newFileName);
                    fileInfo.put("filename", newFileName);
                    fileInfo.put("originalName", originalFilename);
                    fileInfo.put("size", file.getSize());

                    results[i] = fileInfo;
                    successCount++;
                }
            }

            Map<String, Object> resultData = new HashMap<>();
            resultData.put("files", results);
            resultData.put("successCount", successCount);
            resultData.put("totalCount", files.length);

            return Result.success("成功上传 " + successCount + " 个文件", resultData);

        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }
}
