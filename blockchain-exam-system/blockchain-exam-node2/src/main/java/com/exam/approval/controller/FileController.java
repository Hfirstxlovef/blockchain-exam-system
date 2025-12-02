package com.exam.approval.controller;

import com.exam.approval.common.result.Result;
import com.exam.approval.service.FileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 文件管理Controller
 *
 * 功能：
 * 1. 文件上传（自动加密）
 * 2. 文件下载（自动解密）
 * 3. 支持试卷附件、审批材料等
 *
 * @author 网络信息安全大作业
 * @date 2025-11-05
 */
@Slf4j
@Api(tags = "文件管理接口")
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 上传文件
     */
    @ApiOperation("上传文件")
    @PostMapping("/upload")
    public Result<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String filePath = fileService.uploadFile(file);

            log.info("文件上传成功: originalName={}, path={}",
                    file.getOriginalFilename(), filePath);

            return Result.success("上传成功", filePath);

        } catch (Exception e) {
            log.error("文件上传失败", e);
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    /**
     * 下载文件
     */
    @ApiOperation("下载文件")
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadFile(@RequestParam("path") String path,
                                               @RequestParam(value = "filename", required = false) String filename) {
        try {
            // 下载解密后的文件
            byte[] fileBytes = fileService.downloadFile(path);

            // 设置文件名（如果未指定，使用路径中的文件名）
            if (filename == null || filename.isEmpty()) {
                int lastSlash = path.lastIndexOf("/");
                filename = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
            }

            // URL编码文件名（支持中文）
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(fileService.getContentType(path)));
            headers.setContentDispositionFormData("attachment", encodedFilename);
            headers.setContentLength(fileBytes.length);

            log.info("文件下载成功: path={}, filename={}", path, filename);

            return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("文件下载失败: path={}", path, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 删除文件
     */
    @ApiOperation("删除文件")
    @DeleteMapping("/delete")
    public Result<?> deleteFile(@RequestParam("path") String path) {
        try {
            fileService.deleteFile(path);

            log.info("文件删除成功: path={}", path);

            return Result.success("删除成功");

        } catch (Exception e) {
            log.error("文件删除失败: path={}", path, e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }
}
