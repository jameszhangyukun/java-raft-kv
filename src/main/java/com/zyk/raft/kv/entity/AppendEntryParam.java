package com.zyk.raft.kv.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * 追加日志的RPC参数
 */
@Getter
@Setter
@ToString
@Builder
public class AppendEntryParam implements Serializable {
    /**
     * 候选人的任期号
     */
    private long term;
    /**
     * 被请求者的ID
     */
    private String serverId;

    /**
     * leader的Id，以便于follower重新定向请求
     */
    private String leaderId;

    /**
     * 新的日志条目紧随之前
     */
    private long prevLogIndex;
    /**
     * prevLogIndex任期的条目号
     */
    private long prevLogTerm;
    /**
     * 正准备存储的日志条目（表示心跳时为空，一次性发送是为了提高效率）
     */
    private LogEntry[] logEntries;
    /**
     * leader已经提交的日志索引
     */
    private long leaderCommit;
}
