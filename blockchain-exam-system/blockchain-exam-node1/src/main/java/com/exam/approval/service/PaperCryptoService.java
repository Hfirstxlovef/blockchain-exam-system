package com.exam.approval.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.exam.approval.dto.PaperEncryptedRequest;
import com.exam.approval.entity.ExamPaper;
import com.exam.approval.entity.PaperDecryptRecord;
import com.exam.approval.entity.User;
import com.exam.approval.security.util.AESUtil;
import com.exam.approval.security.util.RSAUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 试卷加密解密服务
 * 处理试卷创建时的加密数据解密
 *
 * @author 网络信息安全大作业
 * @date 2025-11-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaperCryptoService {

    private final NonceService nonceService;
    private final UserService userService;

    /**
     * 系统RSA私钥（用于解密）
     */
    @Value("${crypto.system-rsa.private-key}")
    private String systemPrivateKey;

    /**
     * 系统RSA公钥（用于加密）
     */
    @Value("${crypto.system-rsa.public-key}")
    private String systemPublicKey;

    /**
     * 解密试卷请求数据
     *
     * @param encryptedRequest 加密的请求数据
     * @return 解密后的试卷对象
     * @throws RuntimeException 解密失败或验证失败时抛出异常
     */
    public ExamPaper decryptPaperRequest(PaperEncryptedRequest encryptedRequest) {
        try {
            // 1. 验证必要字段
            validateRequest(encryptedRequest);

            // 2. 验证时间戳和nonce（防重放攻击）
            if (!nonceService.validate(encryptedRequest.getTimestamp(), encryptedRequest.getNonce())) {
                log.warn("重放攻击检测 - nonce: {}, timestamp: {}",
                        encryptedRequest.getNonce(), encryptedRequest.getTimestamp());
                throw new RuntimeException("请求已过期或被拒绝，请重新提交");
            }

            // 3. 解密AES密钥
            String aesKey = RSAUtil.decryptByPrivateKeyPKCS1(
                    encryptedRequest.getEncryptedAesKey(), systemPrivateKey);
            log.debug("AES密钥解密成功");

            // 4. 验证HMAC签名（完整性验证）- 使用SHA256-HMAC与前端保持一致
            String expectedHmac = HmacUtils.hmacSha256Hex(aesKey, encryptedRequest.toSignData());
            if (!expectedHmac.equals(encryptedRequest.getHmac())) {
                log.warn("HMAC验证失败 - 预期: {}, 实际: {}",
                        expectedHmac.substring(0, 20), encryptedRequest.getHmac().substring(0, 20));
                throw new RuntimeException("数据完整性校验失败");
            }
            log.debug("HMAC签名验证通过");

            // 5. 解密小字段（RSA）
            String courseName = RSAUtil.decryptByPrivateKeyPKCS1(
                    encryptedRequest.getCourseName(), systemPrivateKey);
            String examType = RSAUtil.decryptByPrivateKeyPKCS1(
                    encryptedRequest.getExamType(), systemPrivateKey);
            String semester = RSAUtil.decryptByPrivateKeyPKCS1(
                    encryptedRequest.getSemester(), systemPrivateKey);
            String department = RSAUtil.decryptByPrivateKeyPKCS1(
                    encryptedRequest.getDepartment(), systemPrivateKey);

            // 6. 解密大字段（AES-CBC，与前端CryptoJS兼容）
            String content = AESUtil.decryptCBC(encryptedRequest.getContent(), aesKey);

            // 7. 解密文件路径（如果有）
            String filePath = "";
            if (encryptedRequest.getFilePath() != null && !encryptedRequest.getFilePath().isEmpty()) {
                filePath = RSAUtil.decryptByPrivateKeyPKCS1(
                        encryptedRequest.getFilePath(), systemPrivateKey);
            }

            // 8. 构建ExamPaper对象
            ExamPaper paper = new ExamPaper();
            paper.setCourseName(courseName);
            paper.setExamType(examType);
            paper.setSemester(semester);
            paper.setDepartment(department);
            paper.setContent(content);
            paper.setFilePath(filePath);

            log.info("试卷数据解密成功 - 课程: {}", courseName);
            return paper;

        } catch (Exception e) {
            log.error("试卷数据解密失败", e);
            throw new RuntimeException("试卷数据解密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证加密请求的必要字段
     *
     * @param request 加密请求
     */
    private void validateRequest(PaperEncryptedRequest request) {
        if (request == null) {
            throw new RuntimeException("请求数据不能为空");
        }
        if (request.getCourseName() == null || request.getCourseName().isEmpty()) {
            throw new RuntimeException("课程名称不能为空");
        }
        if (request.getExamType() == null || request.getExamType().isEmpty()) {
            throw new RuntimeException("考试类型不能为空");
        }
        if (request.getSemester() == null || request.getSemester().isEmpty()) {
            throw new RuntimeException("学期不能为空");
        }
        if (request.getDepartment() == null || request.getDepartment().isEmpty()) {
            throw new RuntimeException("院系不能为空");
        }
        if (request.getContent() == null || request.getContent().isEmpty()) {
            throw new RuntimeException("试卷内容不能为空");
        }
        if (request.getEncryptedAesKey() == null || request.getEncryptedAesKey().isEmpty()) {
            throw new RuntimeException("AES密钥不能为空");
        }
        if (request.getTimestamp() == null) {
            throw new RuntimeException("时间戳不能为空");
        }
        if (request.getNonce() == null || request.getNonce().isEmpty()) {
            throw new RuntimeException("Nonce不能为空");
        }
        if (request.getHmac() == null || request.getHmac().isEmpty()) {
            throw new RuntimeException("HMAC签名不能为空");
        }
    }

    /**
     * 使用用户专属RSA公钥加密试卷内容
     * 实现混合加密：AES加密内容 + RSA加密AES密钥
     *
     * @param paper     试卷对象（包含明文content）
     * @param creatorId 创建者用户ID
     * @return 加密后的试卷对象（content被AES加密，encryptedAesKey被RSA加密）
     * @deprecated 请使用 {@link #encryptPaperWithMultiPartyKeys(ExamPaper, Long)} 替代
     */
    @Deprecated
    public ExamPaper encryptPaperWithUserKey(ExamPaper paper, Long creatorId) {
        try {
            // 1. 获取用户RSA公钥
            User user = userService.getUserById(creatorId);
            if (user == null) {
                throw new RuntimeException("用户不存在: " + creatorId);
            }

            String userPublicKey = user.getRsaPublicKey();
            if (userPublicKey == null || userPublicKey.isEmpty()) {
                log.warn("用户 {} 未生成RSA密钥，使用默认加密方式", creatorId);
                // 用户没有RSA密钥，计算hash后直接返回（使用系统默认AES加密）
                String contentHash = DigestUtils.sha256Hex(paper.getContent());
                paper.setContentHash(contentHash);
                return paper;
            }

            // 2. 计算内容哈希（用于区块链验证）
            String plainContent = paper.getContent();
            String contentHash = DigestUtils.sha256Hex(plainContent);
            paper.setContentHash(contentHash);
            log.debug("内容哈希计算完成: {}", contentHash.substring(0, 16) + "...");

            // 3. 生成随机AES密钥
            String aesKey = AESUtil.generateKey();
            log.debug("AES密钥生成完成");

            // 4. 用AES密钥加密试卷内容
            String encryptedContent = AESUtil.encrypt(plainContent, aesKey);
            paper.setContent(encryptedContent);
            log.debug("试卷内容AES加密完成");

            // 5. 用用户RSA公钥加密AES密钥
            String encryptedAesKey = RSAUtil.encryptByPublicKey(aesKey, userPublicKey);
            paper.setEncryptedAesKey(encryptedAesKey);
            log.debug("AES密钥用户RSA加密完成");

            // 6. 用系统RSA公钥加密AES密钥（供审批人服务端解密）
            String systemEncryptedAesKey = RSAUtil.encryptByPublicKey(aesKey, systemPublicKey);
            paper.setSystemEncryptedAesKey(systemEncryptedAesKey);
            log.debug("AES密钥系统RSA加密完成");

            log.info("试卷PKI加密成功 - 用户ID: {}, 内容哈希: {}", creatorId, contentHash.substring(0, 16) + "...");
            return paper;

        } catch (Exception e) {
            log.error("试卷PKI加密失败 - 用户ID: {}", creatorId, e);
            throw new RuntimeException("试卷加密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解密试卷内容（用于管理员或拥有私钥的用户查看）
     * 需要用户提供私钥来解密AES密钥，再用AES密钥解密内容
     *
     * @param paper         加密的试卷对象
     * @param userPrivateKey 用户RSA私钥（Base64编码）
     * @return 解密后的内容
     */
    public String decryptPaperContent(ExamPaper paper, String userPrivateKey) {
        try {
            String encryptedAesKey = paper.getEncryptedAesKey();
            if (encryptedAesKey == null || encryptedAesKey.isEmpty()) {
                // 没有PKI加密，直接返回内容（可能是系统默认AES加密）
                return paper.getContent();
            }

            // 1. 用用户私钥解密AES密钥
            String aesKey = RSAUtil.decryptByPrivateKey(encryptedAesKey, userPrivateKey);
            log.debug("AES密钥解密成功");

            // 2. 用AES密钥解密内容
            String decryptedContent = AESUtil.decrypt(paper.getContent(), aesKey);
            log.debug("试卷内容解密成功");

            // 3. 验证内容哈希
            String computedHash = DigestUtils.sha256Hex(decryptedContent);
            if (paper.getContentHash() != null && !paper.getContentHash().equals(computedHash)) {
                log.warn("内容哈希验证失败！可能被篡改 - 预期: {}, 实际: {}",
                        paper.getContentHash(), computedHash);
                throw new RuntimeException("试卷内容完整性验证失败，可能已被篡改");
            }

            log.info("试卷解密并验证成功 - 试卷ID: {}", paper.getId());
            return decryptedContent;

        } catch (Exception e) {
            log.error("试卷解密失败 - 试卷ID: {}", paper.getId(), e);
            throw new RuntimeException("试卷解密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 使用系统私钥解密试卷内容（供审批人等授权用户使用）
     * 无需用户提供私钥，服务端直接解密
     *
     * @param paper 加密的试卷对象
     * @return 解密后的内容
     * @deprecated 不符合区块链透明理念，请使用 {@link #decryptPaperWithMultiPartyKey(ExamPaper, String, Long)} 替代
     */
    @Deprecated
    public String decryptPaperBySystemKey(ExamPaper paper) {
        try {
            String systemEncryptedAesKey = paper.getSystemEncryptedAesKey();
            if (systemEncryptedAesKey == null || systemEncryptedAesKey.isEmpty()) {
                // 没有系统加密的AES密钥，可能是旧数据，直接返回内容
                log.warn("试卷 {} 没有系统加密的AES密钥，返回原始内容", paper.getId());
                return paper.getContent();
            }

            // 1. 用系统私钥解密AES密钥
            String aesKey = RSAUtil.decryptByPrivateKey(systemEncryptedAesKey, systemPrivateKey);
            log.debug("系统私钥解密AES密钥成功");

            // 2. 用AES密钥解密内容
            String decryptedContent = AESUtil.decrypt(paper.getContent(), aesKey);
            log.debug("试卷内容解密成功");

            // 3. 验证内容哈希
            String computedHash = DigestUtils.sha256Hex(decryptedContent);
            if (paper.getContentHash() != null && !paper.getContentHash().equals(computedHash)) {
                log.warn("内容哈希验证失败！可能被篡改 - 预期: {}, 实际: {}",
                        paper.getContentHash(), computedHash);
                throw new RuntimeException("试卷内容完整性验证失败，可能已被篡改");
            }

            log.info("服务端解密试卷成功 - 试卷ID: {}", paper.getId());
            return decryptedContent;

        } catch (Exception e) {
            log.error("服务端解密试卷失败 - 试卷ID: {}", paper.getId(), e);
            throw new RuntimeException("服务端解密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证试卷内容完整性
     * 对比存储的哈希值与重新计算的哈希值
     *
     * @param content     明文内容
     * @param storedHash  存储的哈希值
     * @return 是否一致
     */
    public boolean verifyContentIntegrity(String content, String storedHash) {
        if (storedHash == null || storedHash.isEmpty()) {
            return true; // 没有存储哈希，跳过验证
        }
        String computedHash = DigestUtils.sha256Hex(content);
        boolean isValid = storedHash.equals(computedHash);
        if (!isValid) {
            log.warn("内容完整性验证失败 - 存储哈希: {}, 计算哈希: {}", storedHash, computedHash);
        }
        return isValid;
    }

    // ============ 多方加密相关方法（符合区块链理念）============

    /**
     * 获取试卷相关的所有有权解密的用户ID列表
     *
     * 规则：本系教师 → 本系主任 → 本学院院长
     *
     * @param creatorId 创建者ID（教师）
     * @param department 院系（如：计算机系、软件工程系）
     * @return 有权解密的用户ID列表
     */
    public List<Long> getRelatedUserIds(Long creatorId, String department) {
        List<Long> userIds = new ArrayList<>();

        // 1. 添加创建者（本系教师）
        userIds.add(creatorId);

        // 2. 添加本系系主任（同一department的dept_admin）
        User deptAdmin = userService.findByDepartmentAndRole(department, "dept_admin");
        if (deptAdmin != null && deptAdmin.getRsaPublicKey() != null) {
            userIds.add(deptAdmin.getId());
            log.debug("添加本系主任: {} (ID: {})", deptAdmin.getRealName(), deptAdmin.getId());
        }

        // 3. 添加本学院院长（所有college_admin角色）
        List<User> collegeAdmins = userService.findByRole("college_admin");
        for (User admin : collegeAdmins) {
            if (admin.getRsaPublicKey() != null && !userIds.contains(admin.getId())) {
                userIds.add(admin.getId());
                log.debug("添加学院院长: {} (ID: {})", admin.getRealName(), admin.getId());
            }
        }

        log.info("获取相关用户列表完成 - 创建者: {}, 院系: {}, 用户数: {}",
                creatorId, department, userIds.size());
        return userIds;
    }

    /**
     * 为多个用户加密AES密钥
     *
     * @param aesKey 原始AES密钥
     * @param userIds 需要加密的用户ID列表
     * @return JSON格式的加密密钥列表
     */
    public String encryptAesKeyForMultipleUsers(String aesKey, List<Long> userIds) {
        List<Map<String, Object>> encryptedKeys = new ArrayList<>();

        for (Long userId : userIds) {
            try {
                User user = userService.getUserById(userId);
                if (user != null && user.getRsaPublicKey() != null && !user.getRsaPublicKey().isEmpty()) {
                    String encryptedKey = RSAUtil.encryptByPublicKey(aesKey, user.getRsaPublicKey());
                    Map<String, Object> keyEntry = new HashMap<>();
                    keyEntry.put("userId", userId);
                    keyEntry.put("encryptedKey", encryptedKey);
                    encryptedKeys.add(keyEntry);
                    log.debug("为用户 {} 加密AES密钥成功", userId);
                } else {
                    log.warn("用户 {} 没有RSA公钥，跳过加密", userId);
                }
            } catch (Exception e) {
                log.error("为用户 {} 加密AES密钥失败: {}", userId, e.getMessage());
            }
        }

        String json = JSON.toJSONString(encryptedKeys);
        log.info("多方加密完成 - 成功加密用户数: {}/{}", encryptedKeys.size(), userIds.size());
        return json;
    }

    /**
     * 从JSON中查找指定用户的加密AES密钥
     *
     * @param encryptedAesKeys JSON格式的加密密钥列表
     * @param userId 用户ID
     * @return 该用户的加密AES密钥，如果不存在返回null
     */
    public String findUserEncryptedKey(String encryptedAesKeys, Long userId) {
        if (encryptedAesKeys == null || encryptedAesKeys.isEmpty()) {
            return null;
        }

        try {
            JSONArray keysArray = JSON.parseArray(encryptedAesKeys);
            for (int i = 0; i < keysArray.size(); i++) {
                JSONObject keyObj = keysArray.getJSONObject(i);
                Long keyUserId = keyObj.getLong("userId");
                if (keyUserId != null && keyUserId.equals(userId)) {
                    return keyObj.getString("encryptedKey");
                }
            }
        } catch (Exception e) {
            log.error("解析加密密钥JSON失败: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 使用多方加密存储试卷
     * 为教师、系主任、院长分别加密AES密钥
     *
     * @param paper 试卷对象（包含明文content）
     * @param creatorId 创建者用户ID
     * @return 加密后的试卷对象
     */
    public ExamPaper encryptPaperWithMultiPartyKeys(ExamPaper paper, Long creatorId) {
        try {
            // 1. 获取创建者信息
            User creator = userService.getUserById(creatorId);
            if (creator == null) {
                throw new RuntimeException("用户不存在: " + creatorId);
            }

            String department = creator.getDepartment();
            if (department == null || department.isEmpty()) {
                department = paper.getDepartment();
            }

            // 2. 计算内容哈希（用于区块链验证）
            String plainContent = paper.getContent();
            String contentHash = DigestUtils.sha256Hex(plainContent);
            paper.setContentHash(contentHash);
            log.debug("内容哈希计算完成: {}", contentHash.substring(0, 16) + "...");

            // 3. 生成随机AES密钥
            String aesKey = AESUtil.generateKey();
            log.debug("AES密钥生成完成");

            // 4. 用AES密钥加密试卷内容
            String encryptedContent = AESUtil.encrypt(plainContent, aesKey);
            paper.setContent(encryptedContent);
            log.debug("试卷内容AES加密完成");

            // 5. 获取所有相关用户ID
            List<Long> relatedUserIds = getRelatedUserIds(creatorId, department);

            // 6. 为每个相关用户加密AES密钥
            String encryptedAesKeys = encryptAesKeyForMultipleUsers(aesKey, relatedUserIds);
            paper.setEncryptedAesKeys(encryptedAesKeys);

            // 7. 保留单一用户的加密密钥（向后兼容）
            if (creator.getRsaPublicKey() != null && !creator.getRsaPublicKey().isEmpty()) {
                String encryptedAesKey = RSAUtil.encryptByPublicKey(aesKey, creator.getRsaPublicKey());
                paper.setEncryptedAesKey(encryptedAesKey);
            }

            log.info("试卷多方加密成功 - 创建者ID: {}, 院系: {}, 授权用户数: {}",
                    creatorId, department, relatedUserIds.size());
            return paper;

        } catch (Exception e) {
            log.error("试卷多方加密失败 - 创建者ID: {}", creatorId, e);
            throw new RuntimeException("试卷加密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 使用用户私钥解密试卷（多方加密版本）
     *
     * @param paper 加密的试卷对象
     * @param userPrivateKey 用户RSA私钥
     * @param userId 用户ID
     * @return 解密后的内容
     */
    public String decryptPaperWithMultiPartyKey(ExamPaper paper, String userPrivateKey, Long userId) {
        try {
            // 1. 从多方加密密钥中查找该用户的加密密钥
            String userEncryptedKey = findUserEncryptedKey(paper.getEncryptedAesKeys(), userId);

            if (userEncryptedKey == null) {
                // 尝试使用旧版单用户加密密钥
                userEncryptedKey = paper.getEncryptedAesKey();
                if (userEncryptedKey == null || userEncryptedKey.isEmpty()) {
                    throw new RuntimeException("您没有权限解密此试卷");
                }
                log.debug("使用旧版单用户加密密钥");
            }

            // 2. 用私钥解密AES密钥
            String aesKey = RSAUtil.decryptByPrivateKey(userEncryptedKey, userPrivateKey);
            log.debug("AES密钥解密成功");

            // 3. 解密内容
            String decryptedContent = AESUtil.decrypt(paper.getContent(), aesKey);
            log.debug("试卷内容解密成功");

            // 4. 验证内容哈希
            String computedHash = DigestUtils.sha256Hex(decryptedContent);
            if (paper.getContentHash() != null && !paper.getContentHash().equals(computedHash)) {
                log.warn("内容哈希验证失败！可能被篡改 - 预期: {}, 实际: {}",
                        paper.getContentHash(), computedHash);
                throw new RuntimeException("试卷内容完整性验证失败，可能已被篡改");
            }

            log.info("多方加密解密成功 - 试卷ID: {}, 用户ID: {}", paper.getId(), userId);
            return decryptedContent;

        } catch (Exception e) {
            log.error("多方加密解密失败 - 试卷ID: {}, 用户ID: {}", paper.getId(), userId, e);
            throw new RuntimeException("解密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成解密签名数据
     *
     * @param paperId 试卷ID
     * @param userId 用户ID
     * @param timestamp 时间戳
     * @return 签名数据字符串
     */
    public String buildDecryptSignData(Long paperId, Long userId, long timestamp) {
        return String.format("DECRYPT:%d:%d:%d", paperId, userId, timestamp);
    }

    /**
     * 验证解密签名
     *
     * @param record 解密记录
     * @param userPublicKey 用户公钥
     * @return 是否验证通过
     */
    public boolean verifyDecryptSignature(PaperDecryptRecord record, String userPublicKey) {
        if (record.getSignature() == null || record.getSignature().isEmpty()) {
            return false;
        }

        try {
            // 优先使用保存的毫秒时间戳（避免LocalDateTime转换时的精度损失）
            Long timestamp = record.getDecryptTimeMs();
            if (timestamp == null) {
                // 兼容旧数据：从 LocalDateTime 转换（但可能因精度损失导致验证失败）
                timestamp = record.getDecryptTime()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli();
                log.warn("解密记录 {} 没有 decryptTimeMs 字段，使用 LocalDateTime 转换（可能因精度损失导致验证失败）", record.getId());
            }
            String signData = buildDecryptSignData(record.getPaperId(), record.getUserId(), timestamp);
            return RSAUtil.verify(signData, record.getSignature(), userPublicKey);
        } catch (Exception e) {
            log.error("验证解密签名失败: {}", e.getMessage());
            return false;
        }
    }
}
