package com.exam.approval.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.exam.approval.common.annotation.Encrypted;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体
 *
 * @author 网络信息安全大作业
 * @date 2025-11-05
 */
@Data
@TableName("sys_user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String realName;

    private String role;

    private String department;

    private String email;

    private String phone;

    /**
     * 用户RSA公钥（Base64编码）
     */
    private String rsaPublicKey;

    /**
     * 密钥生成时间
     */
    private LocalDateTime keyGeneratedTime;

    private Integer status;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
