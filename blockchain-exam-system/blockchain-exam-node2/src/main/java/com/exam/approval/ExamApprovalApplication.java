package com.exam.approval;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 试卷审批系统主应用类
 * 网络信息安全大作业
 *
 * @author
 * @date 2025-11-05
 */
@SpringBootApplication(scanBasePackages = {
    "com.exam.approval",
    "com.blockchain.exam"
})
@MapperScan({
    "com.exam.approval.mapper",
    "com.blockchain.exam.blockchain.mapper",
    "com.blockchain.exam.p2p.mapper"
})
public class ExamApprovalApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamApprovalApplication.class, args);
    }

}
