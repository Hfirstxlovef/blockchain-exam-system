package com.blockchain.exam.p2p.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * P2P节点实体类
 *
 * 存储P2P网络中的节点信息
 * 用于节点发现、心跳检测、状态管理
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@Data
@TableName("p2p_node")
public class P2PNode implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 节点ID（数据库主键）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 节点标识（node1/node2/node3）
     */
    private String nodeId;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 主机地址
     */
    private String host;

    /**
     * 端口号
     */
    private Integer port;

    /**
     * 节点公钥（用于加密通信）
     */
    private String publicKey;

    /**
     * 节点状态
     * ONLINE - 在线
     * OFFLINE - 离线
     */
    private String status;

    /**
     * 最后在线时间
     */
    private LocalDateTime lastSeenTime;

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

    /**
     * 判断节点是否在线
     */
    public boolean isOnline() {
        return "ONLINE".equals(status);
    }

    /**
     * 判断节点是否离线
     */
    public boolean isOffline() {
        return "OFFLINE".equals(status);
    }

    /**
     * 标记节点为在线
     */
    public void markAsOnline() {
        this.status = "ONLINE";
        this.lastSeenTime = LocalDateTime.now();
    }

    /**
     * 标记节点为离线
     */
    public void markAsOffline() {
        this.status = "OFFLINE";
    }

    /**
     * 获取节点URL
     * 格式：http://host:port/api
     */
    public String getNodeUrl() {
        return String.format("http://%s:%d/api", host, port);
    }

    /**
     * 判断节点是否超时（超过指定时间未见）
     *
     * @param timeoutMinutes 超时时间（分钟）
     * @return 是否超时
     */
    public boolean isTimeout(int timeoutMinutes) {
        if (lastSeenTime == null) {
            return true;
        }
        LocalDateTime timeout = LocalDateTime.now().minusMinutes(timeoutMinutes);
        return lastSeenTime.isBefore(timeout);
    }

    @Override
    public String toString() {
        return "P2PNode{" +
                "id=" + id +
                ", nodeId='" + nodeId + '\'' +
                ", nodeName='" + nodeName + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", status='" + status + '\'' +
                ", lastSeenTime=" + lastSeenTime +
                '}';
    }
}
