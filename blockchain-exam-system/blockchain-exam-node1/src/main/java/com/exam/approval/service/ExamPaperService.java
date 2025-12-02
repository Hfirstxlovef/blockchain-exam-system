package com.exam.approval.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.approval.common.exception.BusinessException;
import com.exam.approval.entity.ApprovalWorkflow;
import com.exam.approval.entity.ExamPaper;
import com.exam.approval.mapper.ExamPaperMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 试卷Service
 *
 * @author 网络信息安全大作业
 * @date 2025-11-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamPaperService extends ServiceImpl<ExamPaperMapper, ExamPaper> {

    private final ApprovalWorkflowService workflowService;

    /**
     * 根据创建人ID查询试卷列表
     *
     * @param creatorId 创建人ID
     * @return 试卷列表
     */
    public List<ExamPaper> getByCreatorId(Long creatorId) {
        return this.list(new LambdaQueryWrapper<ExamPaper>()
                .eq(ExamPaper::getCreatorId, creatorId)
                .orderByDesc(ExamPaper::getCreateTime));
    }

    /**
     * 创建试卷
     *
     * @param paper 试卷对象
     * @return 试卷ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createPaper(ExamPaper paper) {
        // 设置默认状态为草稿
        paper.setStatus("draft");

        // 保存试卷
        this.save(paper);
        log.info("创建试卷成功，ID: {}, 创建人: {}", paper.getId(), paper.getCreatorName());

        return paper.getId();
    }

    /**
     * 提交审批
     *
     * @param paperId 试卷ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void submitForApproval(Long paperId) {
        ExamPaper paper = this.getById(paperId);
        if (paper == null) {
            throw new BusinessException("试卷不存在");
        }

        if (!"draft".equals(paper.getStatus())) {
            throw new BusinessException("只有草稿状态的试卷才能提交审批");
        }

        // 更新试卷状态
        paper.setStatus("pending");
        this.updateById(paper);

        // 创建审批工作流
        ApprovalWorkflow workflow = new ApprovalWorkflow();
        workflow.setPaperId(paperId);
        workflow.setCurrentStep(1); // 第一步：系审批
        workflow.setStatus("pending");
        workflowService.save(workflow);

        log.info("提交审批成功，试卷ID: {}", paperId);
    }

    /**
     * 更新试卷状态
     *
     * @param paperId 试卷ID
     * @param status  新状态
     */
    public void updateStatus(Long paperId, String status) {
        ExamPaper paper = this.getById(paperId);
        if (paper != null) {
            paper.setStatus(status);
            this.updateById(paper);
            log.info("更新试卷状态，ID: {}, 状态: {}", paperId, status);
        }
    }

    /**
     * 根据状态查询试卷列表
     *
     * @param status 状态
     * @return 试卷列表
     */
    public List<ExamPaper> getByStatus(String status) {
        List<ExamPaper> papers = this.list(new LambdaQueryWrapper<ExamPaper>()
                .eq(ExamPaper::getStatus, status)
                .orderByDesc(ExamPaper::getCreateTime));

        // 字段映射：将持久化字段映射到前端期望的非持久化字段
        papers.forEach(paper -> {
            paper.setCourseName(paper.getSubject());
            // 从title中提取examType
            if (paper.getTitle() != null && paper.getTitle().contains(" ")) {
                String[] parts = paper.getTitle().split(" ", 2);
                if (parts.length > 1) {
                    paper.setExamType(parts[1]);
                }
            }
            paper.setSemester(paper.getGrade());
            paper.setTeacherName(paper.getCreatorName());
            paper.setTeacherId(paper.getCreatorId());
        });

        return papers;
    }

    /**
     * 根据状态和部门查询试卷列表
     *
     * @param status     状态
     * @param department 部门
     * @return 试卷列表
     */
    public List<ExamPaper> getByStatusAndDepartment(String status, String department) {
        List<ExamPaper> papers = this.list(new LambdaQueryWrapper<ExamPaper>()
                .eq(ExamPaper::getStatus, status)
                .eq(ExamPaper::getDepartment, department)
                .orderByDesc(ExamPaper::getCreateTime));

        // 字段映射：将持久化字段映射到前端期望的非持久化字段
        papers.forEach(paper -> {
            paper.setCourseName(paper.getSubject());
            if (paper.getTitle() != null && paper.getTitle().contains(" ")) {
                String[] parts = paper.getTitle().split(" ", 2);
                if (parts.length > 1) {
                    paper.setExamType(parts[1]);
                }
            }
            paper.setSemester(paper.getGrade());
            paper.setTeacherName(paper.getCreatorName());
            paper.setTeacherId(paper.getCreatorId());
        });

        return papers;
    }

    /**
     * 根据教师ID查询试卷列表
     *
     * @param teacherId 教师ID
     * @param status    状态（可选）
     * @return 试卷列表
     */
    public List<ExamPaper> getByTeacherId(Long teacherId, String status) {
        LambdaQueryWrapper<ExamPaper> wrapper = new LambdaQueryWrapper<ExamPaper>()
                .eq(ExamPaper::getCreatorId, teacherId);

        if (status != null && !status.isEmpty()) {
            wrapper.eq(ExamPaper::getStatus, status);
        }

        List<ExamPaper> papers = this.list(wrapper.orderByDesc(ExamPaper::getCreateTime));

        // 字段映射：将持久化字段映射到前端期望的非持久化字段
        papers.forEach(paper -> {
            paper.setCourseName(paper.getSubject());
            // 从title中提取examType（title格式: "课程名称 考试类型"）
            if (paper.getTitle() != null && paper.getTitle().contains(" ")) {
                String[] parts = paper.getTitle().split(" ", 2);
                if (parts.length > 1) {
                    paper.setExamType(parts[1]);
                }
            }
            paper.setSemester(paper.getGrade());
            paper.setTeacherName(paper.getCreatorName());
            paper.setTeacherId(paper.getCreatorId());
            // department字段已是持久化字段，无需额外映射
        });

        return papers;
    }

    /**
     * 查询所有试卷（管理员）
     *
     * @param status     状态（可选）
     * @param department 院系（可选）
     * @return 试卷列表
     */
    public List<ExamPaper> getAll(String status, String department) {
        LambdaQueryWrapper<ExamPaper> wrapper = new LambdaQueryWrapper<>();

        if (status != null && !status.isEmpty()) {
            wrapper.eq(ExamPaper::getStatus, status);
        }

        if (department != null && !department.isEmpty()) {
            wrapper.eq(ExamPaper::getDepartment, department);
        }

        List<ExamPaper> papers = this.list(wrapper.orderByDesc(ExamPaper::getCreateTime));

        // 字段映射：将持久化字段映射到前端期望的非持久化字段
        papers.forEach(paper -> {
            paper.setCourseName(paper.getSubject());
            // 从title中提取examType
            if (paper.getTitle() != null && paper.getTitle().contains(" ")) {
                String[] parts = paper.getTitle().split(" ", 2);
                if (parts.length > 1) {
                    paper.setExamType(parts[1]);
                }
            }
            paper.setSemester(paper.getGrade());
            paper.setTeacherName(paper.getCreatorName());
            paper.setTeacherId(paper.getCreatorId());
        });

        return papers;
    }

    /**
     * 更新试卷
     *
     * @param paper 试卷对象
     */
    public void updatePaper(ExamPaper paper) {
        ExamPaper existingPaper = this.getById(paper.getId());
        if (existingPaper == null) {
            throw new BusinessException("试卷不存在");
        }

        // 更新持久化字段
        existingPaper.setTitle(paper.getTitle());
        existingPaper.setSubject(paper.getSubject());
        existingPaper.setGrade(paper.getGrade());
        existingPaper.setDepartment(paper.getDepartment());
        existingPaper.setContent(paper.getContent());
        existingPaper.setFilePath(paper.getFilePath());

        this.updateById(existingPaper);
        log.info("更新试卷成功，ID: {}", paper.getId());
    }

    /**
     * 删除试卷
     *
     * @param paperId 试卷ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deletePaper(Long paperId) {
        ExamPaper paper = this.getById(paperId);
        if (paper == null) {
            throw new BusinessException("试卷不存在");
        }

        // 删除试卷
        this.removeById(paperId);

        // 如果有工作流，也删除工作流
        ApprovalWorkflow workflow = workflowService.getByPaperId(paperId);
        if (workflow != null) {
            workflowService.removeById(workflow.getId());
        }

        log.info("删除试卷成功，ID: {}", paperId);
    }

    /**
     * 统计试卷数量（按状态）
     * 教师：统计自己的试卷
     * 管理员：统计全局试卷
     *
     * @param role 用户角色
     * @param userId 用户ID
     * @return 统计数据
     */
    public Map<String, Long> getStatistics(String role, Long userId) {
        Map<String, Long> statistics = new HashMap<>();

        // 是否是教师（教师只统计自己的试卷）
        boolean isTeacher = "teacher".equals(role);

        // 总数
        LambdaQueryWrapper<ExamPaper> totalWrapper = new LambdaQueryWrapper<>();
        if (isTeacher) {
            totalWrapper.eq(ExamPaper::getCreatorId, userId);
        }
        long total = this.count(totalWrapper);
        statistics.put("total", total);

        // 草稿
        LambdaQueryWrapper<ExamPaper> draftWrapper = new LambdaQueryWrapper<ExamPaper>()
                .eq(ExamPaper::getStatus, "draft");
        if (isTeacher) {
            draftWrapper.eq(ExamPaper::getCreatorId, userId);
        }
        long draft = this.count(draftWrapper);
        statistics.put("draft", draft);

        // 待系审批
        LambdaQueryWrapper<ExamPaper> pendingWrapper = new LambdaQueryWrapper<ExamPaper>()
                .eq(ExamPaper::getStatus, "pending");
        if (isTeacher) {
            pendingWrapper.eq(ExamPaper::getCreatorId, userId);
        }
        long pending = this.count(pendingWrapper);
        statistics.put("pending", pending);

        // 系已审批
        LambdaQueryWrapper<ExamPaper> deptApprovedWrapper = new LambdaQueryWrapper<ExamPaper>()
                .eq(ExamPaper::getStatus, "dept_approved");
        if (isTeacher) {
            deptApprovedWrapper.eq(ExamPaper::getCreatorId, userId);
        }
        long deptApproved = this.count(deptApprovedWrapper);
        statistics.put("deptApproved", deptApproved);

        // 院已审批
        LambdaQueryWrapper<ExamPaper> collegeApprovedWrapper = new LambdaQueryWrapper<ExamPaper>()
                .eq(ExamPaper::getStatus, "college_approved");
        if (isTeacher) {
            collegeApprovedWrapper.eq(ExamPaper::getCreatorId, userId);
        }
        long collegeApproved = this.count(collegeApprovedWrapper);
        statistics.put("collegeApproved", collegeApproved);

        // 已驳回
        LambdaQueryWrapper<ExamPaper> rejectedWrapper = new LambdaQueryWrapper<ExamPaper>()
                .eq(ExamPaper::getStatus, "rejected");
        if (isTeacher) {
            rejectedWrapper.eq(ExamPaper::getCreatorId, userId);
        }
        long rejected = this.count(rejectedWrapper);
        statistics.put("rejected", rejected);

        return statistics;
    }
}
