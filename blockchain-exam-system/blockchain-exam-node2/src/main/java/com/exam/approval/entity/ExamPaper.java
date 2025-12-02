package com.exam.approval.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.exam.approval.common.annotation.Encrypted;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 试卷实体
 *
 * @author 网络信息安全大作业
 * @date 2025-11-05
 */
@Data
@TableName("exam_paper")
public class ExamPaper implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 试卷ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 试卷标题
     */
    private String title;

    /**
     * 科目
     */
    private String subject;

    /**
     * 年级
     */
    private String grade;

    /**
     * 试卷内容（PKI加密存储：AES加密内容，RSA加密AES密钥）
     * 不使用@Encrypted注解，因为使用用户专属RSA密钥进行混合加密
     */
    private String content;

    /**
     * 附件路径（加密存储）
     */
    @Encrypted
    private String filePath;

    /**
     * 附件原始文件名
     */
    private String fileName;

    /**
     * 试卷内容哈希（SHA256，用于区块链验证）
     */
    private String contentHash;

    /**
     * 用用户RSA公钥加密的AES密钥（Base64编码）
     * 用户需要用自己的私钥解密此AES密钥，再用AES密钥解密content
     */
    private String encryptedAesKey;

    /**
     * 用系统RSA公钥加密的AES密钥（Base64编码）
     * @deprecated 已废弃，改用 encryptedAesKeys 多方加密
     */
    @Deprecated
    private String systemEncryptedAesKey;

    /**
     * 多方加密的AES密钥列表（JSON格式）
     * 格式: [{"userId": 1, "encryptedKey": "xxx"}, {"userId": 2, "encryptedKey": "yyy"}]
     * 每个有权解密的用户都有一份用其公钥加密的AES密钥
     */
    private String encryptedAesKeys;

    /**
     * 区块链交易ID
     */
    private Long blockchainTxId;

    /**
     * 上链时间
     */
    private LocalDateTime chainTime;

    /**
     * 创建者数字签名（RSA签名，Base64编码）
     * 用于验证试卷创建者身份，确保不可抵赖
     */
    private String creatorSignature;

    /**
     * 状态：draft-草稿, pending-待审批, dept_approved-系已审批,
     * college_approved-院已审批, rejected-已驳回
     */
    private String status;

    /**
     * 创建人ID
     */
    @TableField("creator_id")
    private Long creatorId;

    /**
     * 创建人姓名
     */
    @TableField("creator_name")
    private String creatorName;

    /**
     * 院系
     */
    private String department;

    // ============ 字段别名（为了兼容Controller中的命名） ============

    /**
     * 教师ID（别名，映射到creatorId）
     */
    @TableField(exist = false)
    private Long teacherId;

    /**
     * 教师姓名（别名，映射到creatorName）
     */
    @TableField(exist = false)
    private String teacherName;

    /**
     * 课程名称（非持久化字段）
     */
    @TableField(exist = false)
    private String courseName;

    /**
     * 考试类型（非持久化字段）
     */
    @TableField(exist = false)
    private String examType;

    /**
     * 学期（非持久化字段）
     */
    @TableField(exist = false)
    private String semester;

    // ============ 系统字段 ============

    /**
     * 逻辑删除：0-未删除, 1-已删除
     */
    @TableLogic
    private Integer deleted;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
