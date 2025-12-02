package com.exam.approval.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.approval.entity.PaperDecryptRecord;
import com.exam.approval.mapper.PaperDecryptRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 试卷解密记录Service
 *
 * 提供解密记录的CRUD操作，支持：
 * - 记录解密操作
 * - 查询试卷的解密记录
 * - 统计解密记录数量
 *
 * @author 网络信息安全大作业
 * @date 2025-11-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaperDecryptRecordService extends ServiceImpl<PaperDecryptRecordMapper, PaperDecryptRecord> {

    /**
     * 保存解密记录
     *
     * @param record 解密记录
     * @return 是否保存成功
     */
    public boolean saveRecord(PaperDecryptRecord record) {
        boolean result = this.save(record);
        if (result) {
            log.info("解密记录保存成功 - 试卷ID: {}, 用户: {} ({})",
                    record.getPaperId(), record.getUserName(), record.getUserRole());
        }
        return result;
    }

    /**
     * 根据试卷ID查询解密记录
     *
     * @param paperId 试卷ID
     * @return 解密记录列表
     */
    public List<PaperDecryptRecord> findByPaperId(Long paperId) {
        return baseMapper.findByPaperId(paperId);
    }

    /**
     * 根据用户ID查询解密记录
     *
     * @param userId 用户ID
     * @return 解密记录列表
     */
    public List<PaperDecryptRecord> findByUserId(Long userId) {
        return baseMapper.findByUserId(userId);
    }

    /**
     * 统计解密记录总数
     *
     * @return 记录总数
     */
    public Long countAll() {
        return baseMapper.countAll();
    }

    /**
     * 分页查询解密记录
     *
     * @param page 页码（从1开始）
     * @param size 每页数量
     * @return 解密记录列表
     */
    public List<PaperDecryptRecord> findAllPaged(int page, int size) {
        int offset = (page - 1) * size;
        return baseMapper.findAllPaged(offset, size);
    }

    /**
     * 更新区块链交易ID
     *
     * @param recordId 记录ID
     * @param txId 区块链交易ID
     * @return 是否更新成功
     */
    public boolean updateBlockchainTxId(Long recordId, Long txId) {
        PaperDecryptRecord record = this.getById(recordId);
        if (record == null) {
            log.warn("更新区块链交易ID失败 - 记录不存在: {}", recordId);
            return false;
        }
        record.setBlockchainTxId(txId);
        record.setChainTime(java.time.LocalDateTime.now());
        boolean result = this.updateById(record);
        if (result) {
            log.info("解密记录上链成功 - 记录ID: {}, 交易ID: {}", recordId, txId);
        }
        return result;
    }
}
