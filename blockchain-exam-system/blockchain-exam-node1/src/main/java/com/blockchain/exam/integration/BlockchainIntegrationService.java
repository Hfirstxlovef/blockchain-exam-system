package com.blockchain.exam.integration;

import com.blockchain.exam.blockchain.entity.BlockData;
import com.blockchain.exam.blockchain.entity.Transaction;
import com.blockchain.exam.blockchain.mapper.TransactionMapper;
import com.blockchain.exam.blockchain.util.HashUtil;
import com.blockchain.exam.p2p.service.P2PTransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 区块链集成服务
 *
 * 将业务功能与区块链功能集成：
 * - 审批记录上链
 * - 试卷哈希上链
 * - 用户权限认证上链
 * - 自动广播交易
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@Slf4j
@Service
public class BlockchainIntegrationService {

    @Autowired
    private TransactionMapper transactionMapper;

    @Autowired
    private P2PTransactionService p2pTransactionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${p2p.node.id}")
    private String currentNodeId;

    /**
     * 将审批记录上链
     *
     * @param paperId    试卷ID
     * @param approverId 审批人ID
     * @param action     操作类型（approve/reject）
     * @param signature  数字签名
     * @return 交易ID
     */
    @Transactional
    public Long submitApprovalToChain(Long paperId, Long approverId, String action, String signature) {
        try {
            log.info("开始将审批记录上链 - 试卷ID: {}, 审批人ID: {}, 操作: {}",
                    paperId, approverId, action);

            // 创建区块数据
            BlockData blockData = BlockData.createApprovalRecord(paperId, approverId, action, signature);

            // 创建交易
            Transaction transaction = new Transaction();
            transaction.setTransactionType("APPROVAL_RECORD");
            transaction.setTransactionData(objectMapper.writeValueAsString(blockData));
            transaction.setCreatorNode(currentNodeId);
            transaction.setStatus("PENDING");

            // 保存到本地交易池
            transactionMapper.insert(transaction);

            log.info("审批记录交易已创建 - 交易ID: {}", transaction.getId());

            // 广播到其他节点
            int broadcastCount = p2pTransactionService.broadcastTransaction(transaction);

            log.info("审批记录已广播 - 交易ID: {}, 成功广播到 {} 个节点",
                    transaction.getId(), broadcastCount);

            return transaction.getId();
        } catch (Exception e) {
            log.error("审批记录上链失败", e);
            throw new RuntimeException("审批记录上链失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将试卷哈希上链
     *
     * @param paperId      试卷ID
     * @param paperContent 试卷内容
     * @return 交易ID
     */
    @Transactional
    public Long submitPaperHashToChain(Long paperId, String paperContent) {
        try {
            log.info("开始将试卷哈希上链 - 试卷ID: {}", paperId);

            // 计算试卷内容哈希
            String paperHash = HashUtil.sha256(paperContent);

            // 创建区块数据
            BlockData blockData = BlockData.createPaperHash(paperId, paperHash);

            // 创建交易
            Transaction transaction = new Transaction();
            transaction.setTransactionType("PAPER_HASH");
            transaction.setTransactionData(objectMapper.writeValueAsString(blockData));
            transaction.setCreatorNode(currentNodeId);
            transaction.setStatus("PENDING");

            // 保存到本地交易池
            transactionMapper.insert(transaction);

            log.info("试卷哈希交易已创建 - 交易ID: {}, 哈希: {}",
                    transaction.getId(), paperHash);

            // 广播到其他节点
            int broadcastCount = p2pTransactionService.broadcastTransaction(transaction);

            log.info("试卷哈希已广播 - 交易ID: {}, 成功广播到 {} 个节点",
                    transaction.getId(), broadcastCount);

            return transaction.getId();
        } catch (Exception e) {
            log.error("试卷哈希上链失败", e);
            throw new RuntimeException("试卷哈希上链失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将加密的试卷内容上链
     *
     * @param paperId          试卷ID
     * @param encryptedContent 加密后的试卷内容
     * @param contentHash      试卷内容哈希
     * @param creatorId        创建者ID
     * @param creatorName      创建者姓名
     * @param title            试卷标题
     * @return 交易ID
     */
    @Transactional
    public Long submitEncryptedPaperToChain(Long paperId, String encryptedContent, String contentHash,
                                            Long creatorId, String creatorName, String title) {
        try {
            log.info("开始将加密试卷上链 - 试卷ID: {}, 创建者: {}", paperId, creatorName);

            // 创建区块数据（包含创建者信息）
            BlockData blockData = BlockData.createPaperContent(paperId, encryptedContent, contentHash,
                                                               creatorId, creatorName, title);

            // 创建交易
            Transaction transaction = new Transaction();
            transaction.setTransactionType("PAPER_CONTENT");
            transaction.setTransactionData(objectMapper.writeValueAsString(blockData));
            transaction.setCreatorNode(currentNodeId);
            transaction.setStatus("PENDING");

            // 保存到本地交易池
            transactionMapper.insert(transaction);

            log.info("加密试卷交易已创建 - 交易ID: {}", transaction.getId());

            // 广播到其他节点
            int broadcastCount = p2pTransactionService.broadcastTransaction(transaction);

            log.info("加密试卷已广播 - 交易ID: {}, 成功广播到 {} 个节点",
                    transaction.getId(), broadcastCount);

            return transaction.getId();
        } catch (Exception e) {
            log.error("加密试卷上链失败", e);
            throw new RuntimeException("加密试卷上链失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将用户权限认证上链
     *
     * @param userId 用户ID
     * @param role   角色
     * @param action 操作
     * @return 交易ID
     */
    @Transactional
    public Long submitUserAuthToChain(Long userId, String role, String action) {
        try {
            log.info("开始将用户权限认证上链 - 用户ID: {}, 角色: {}, 操作: {}",
                    userId, role, action);

            // 创建区块数据
            BlockData blockData = BlockData.createUserAuth(userId, role, action);

            // 创建交易
            Transaction transaction = new Transaction();
            transaction.setTransactionType("USER_AUTH");
            transaction.setTransactionData(objectMapper.writeValueAsString(blockData));
            transaction.setCreatorNode(currentNodeId);
            transaction.setStatus("PENDING");

            // 保存到本地交易池
            transactionMapper.insert(transaction);

            log.info("用户权限认证交易已创建 - 交易ID: {}", transaction.getId());

            // 广播到其他节点
            int broadcastCount = p2pTransactionService.broadcastTransaction(transaction);

            log.info("用户权限认证已广播 - 交易ID: {}, 成功广播到 {} 个节点",
                    transaction.getId(), broadcastCount);

            return transaction.getId();
        } catch (Exception e) {
            log.error("用户权限认证上链失败", e);
            throw new RuntimeException("用户权限认证上链失败: " + e.getMessage(), e);
        }
    }

    /**
     * 记录解密操作到区块链
     *
     * @param paperId     试卷ID
     * @param userId      解密用户ID
     * @param userName    解密用户名
     * @param userRole    用户角色
     * @param contentHash 试卷内容哈希
     * @param signature   用户签名
     * @return 交易ID
     */
    @Transactional
    public Long recordDecryptAction(Long paperId, Long userId, String userName,
                                    String userRole, String contentHash, String signature) {
        try {
            log.info("开始将解密记录上链 - 试卷ID: {}, 用户ID: {}, 角色: {}",
                    paperId, userId, userRole);

            // 创建区块数据
            BlockData blockData = BlockData.createDecryptRecord(
                    paperId, userId, userName, userRole, contentHash, signature);

            // 创建交易
            Transaction transaction = new Transaction();
            transaction.setTransactionType("DECRYPT_RECORD");
            transaction.setTransactionData(objectMapper.writeValueAsString(blockData));
            transaction.setCreatorNode(currentNodeId);
            transaction.setStatus("PENDING");

            // 保存到本地交易池
            transactionMapper.insert(transaction);

            log.info("解密记录交易已创建 - 交易ID: {}", transaction.getId());

            // 广播到其他节点
            int broadcastCount = p2pTransactionService.broadcastTransaction(transaction);

            log.info("解密记录已广播 - 交易ID: {}, 成功广播到 {} 个节点",
                    transaction.getId(), broadcastCount);

            return transaction.getId();
        } catch (Exception e) {
            log.error("解密记录上链失败", e);
            throw new RuntimeException("解密记录上链失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证试卷内容是否被篡改
     *
     * @param paperId      试卷ID
     * @param paperContent 当前试卷内容
     * @param storedHash   存储的哈希值
     * @return 是否一致
     */
    public boolean verifyPaperIntegrity(Long paperId, String paperContent, String storedHash) {
        try {
            String currentHash = HashUtil.sha256(paperContent);
            boolean isValid = currentHash.equals(storedHash);

            if (isValid) {
                log.info("试卷内容验证通过 - 试卷ID: {}", paperId);
            } else {
                log.warn("试卷内容验证失败 - 试卷ID: {}, 当前哈希: {}, 存储哈希: {}",
                        paperId, currentHash, storedHash);
            }

            return isValid;
        } catch (Exception e) {
            log.error("试卷内容验证异常", e);
            return false;
        }
    }

    /**
     * 获取试卷的区块链记录统计
     *
     * @param paperId 试卷ID
     * @return 统计信息
     */
    public java.util.Map<String, Object> getPaperBlockchainStats(Long paperId) {
        try {
            // 统计该试卷相关的交易数量
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Transaction> wrapper =
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            wrapper.like("transaction_data", "\"paperId\":" + paperId);

            Long totalCount = transactionMapper.selectCount(wrapper);

            wrapper.eq("status", "MINED");
            Long minedCount = transactionMapper.selectCount(wrapper);

            java.util.Map<String, Object> stats = new java.util.HashMap<>();
            stats.put("paperId", paperId);
            stats.put("totalTransactions", totalCount);
            stats.put("minedTransactions", minedCount);
            stats.put("pendingTransactions", totalCount - minedCount);

            return stats;
        } catch (Exception e) {
            log.error("获取试卷区块链统计失败", e);
            return new java.util.HashMap<>();
        }
    }
}
