package com.exam.approval.service;

import com.exam.approval.common.exception.BusinessException;
import com.exam.approval.security.util.AESUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

/**
 * 文件服务 - 加密文件上传下载
 *
 * 功能：
 * 1. 文件上传（自动AES加密存储）
 * 2. 文件下载（自动解密）
 * 3. 文件删除
 * 4. 支持多种文件类型（PDF、Word、图片等）
 *
 * 安全特性：
 * - 所有文件使用AES-256-GCM加密存储
 * - 文件名随机化，防止路径遍历
 * - 按日期分目录存储
 * - 文件大小限制
 *
 * @author 网络信息安全大作业
 * @date 2025-11-05
 */
@Slf4j
@Service
public class FileService {

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    @Value("${file.upload.max-size:10485760}")  // 默认10MB
    private long maxFileSize;

    /**
     * AES加密密钥（实际应用中应从配置中心获取）
     */
    private static final String ENCRYPTION_KEY = AESUtil.generateKey();

    /**
     * 允许的文件扩展名
     */
    private static final String[] ALLOWED_EXTENSIONS = {
            ".pdf", ".doc", ".docx", ".xls", ".xlsx",
            ".ppt", ".pptx", ".txt", ".jpg", ".jpeg",
            ".png", ".gif", ".zip", ".rar"
    };

    /**
     * 初始化上传目录
     */
    @PostConstruct
    public void init() {
        try {
            Path path = Paths.get(uploadPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("创建文件上传目录: {}", uploadPath);
            }
        } catch (IOException e) {
            log.error("创建上传目录失败", e);
            throw new BusinessException("文件服务初始化失败");
        }
    }

    /**
     * 上传文件（加密存储）
     *
     * @param file 上传的文件
     * @return 加密后的文件路径
     */
    public String uploadFile(MultipartFile file) {
        // 1. 验证文件
        validateFile(file);

        // 2. 生成文件名和路径
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String randomFilename = UUID.randomUUID().toString() + extension;
        String relativePath = dateDir + "/" + randomFilename;

        try {
            // 3. 创建日期目录
            Path datePath = Paths.get(uploadPath, dateDir);
            if (!Files.exists(datePath)) {
                Files.createDirectories(datePath);
            }

            // 4. 读取文件内容
            byte[] fileBytes = file.getBytes();

            // 5. 加密文件内容
            String encryptedContent = AESUtil.encrypt(
                    Base64.getEncoder().encodeToString(fileBytes),
                    ENCRYPTION_KEY
            );

            // 6. 保存加密文件
            Path targetPath = Paths.get(uploadPath, relativePath);
            try (FileOutputStream fos = new FileOutputStream(targetPath.toFile())) {
                fos.write(encryptedContent.getBytes());
            }

            log.info("文件上传成功（已加密）: originalName={}, encryptedPath={}",
                    originalFilename, relativePath);

            return relativePath;

        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 下载文件（解密）
     *
     * @param filePath 文件路径
     * @return 解密后的文件内容
     */
    public byte[] downloadFile(String filePath) {
        try {
            // 1. 验证文件路径（防止路径遍历攻击）
            Path path = Paths.get(uploadPath, filePath).normalize();
            if (!path.startsWith(Paths.get(uploadPath).toAbsolutePath())) {
                throw new BusinessException("非法的文件路径");
            }

            // 2. 检查文件是否存在
            File file = path.toFile();
            if (!file.exists() || !file.isFile()) {
                throw new BusinessException("文件不存在");
            }

            // 3. 读取加密文件
            byte[] encryptedBytes;
            try (FileInputStream fis = new FileInputStream(file)) {
                encryptedBytes = fis.readAllBytes();
            }

            // 4. 解密文件内容
            String encryptedContent = new String(encryptedBytes);
            String decryptedContent = AESUtil.decrypt(encryptedContent, ENCRYPTION_KEY);

            // 5. Base64解码得到原始文件
            byte[] fileBytes = Base64.getDecoder().decode(decryptedContent);

            log.info("文件下载成功（已解密）: path={}, size={}", filePath, fileBytes.length);

            return fileBytes;

        } catch (Exception e) {
            log.error("文件下载失败: path={}", filePath, e);
            throw new BusinessException("文件下载失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件
     *
     * @param filePath 文件路径
     */
    public void deleteFile(String filePath) {
        try {
            // 验证文件路径
            Path path = Paths.get(uploadPath, filePath).normalize();
            if (!path.startsWith(Paths.get(uploadPath).toAbsolutePath())) {
                throw new BusinessException("非法的文件路径");
            }

            // 删除文件
            Files.deleteIfExists(path);

            log.info("文件删除成功: path={}", filePath);

        } catch (Exception e) {
            log.error("文件删除失败: path={}", filePath, e);
            throw new BusinessException("文件删除失败: " + e.getMessage());
        }
    }

    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        // 检查文件是否为空
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }

        // 检查文件大小
        if (file.getSize() > maxFileSize) {
            throw new BusinessException("文件大小超过限制（最大" + (maxFileSize / 1024 / 1024) + "MB）");
        }

        // 检查文件扩展名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new BusinessException("文件名不能为空");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        boolean allowed = false;
        for (String allowedExt : ALLOWED_EXTENSIONS) {
            if (extension.equals(allowedExt)) {
                allowed = true;
                break;
            }
        }

        if (!allowed) {
            throw new BusinessException("不支持的文件类型: " + extension);
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }

        int dotIndex = filename.lastIndexOf(".");
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex);
        }

        return "";
    }

    /**
     * 获取文件MIME类型
     */
    public String getContentType(String filePath) {
        String extension = getFileExtension(filePath).toLowerCase();

        switch (extension) {
            case ".pdf":
                return "application/pdf";
            case ".doc":
            case ".docx":
                return "application/msword";
            case ".xls":
            case ".xlsx":
                return "application/vnd.ms-excel";
            case ".ppt":
            case ".pptx":
                return "application/vnd.ms-powerpoint";
            case ".txt":
                return "text/plain";
            case ".jpg":
            case ".jpeg":
                return "image/jpeg";
            case ".png":
                return "image/png";
            case ".gif":
                return "image/gif";
            case ".zip":
                return "application/zip";
            case ".rar":
                return "application/x-rar-compressed";
            default:
                return "application/octet-stream";
        }
    }
}
