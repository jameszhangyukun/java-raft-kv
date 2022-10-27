package com.zyk.raft.kv.entity;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 请求投票的RPC参数
 */
@Setter
@Getter
@Data
@Builder
public class VoteParam implements Serializable {
    /**
     * 候选人的任期编号
     */
    private long term;

    /**
     * 接收选票的Id
     */
    private String serverId;

    /**
     * 请求选票的候选人Id
     */
    private String candidateId;
    /**
     * 候选人的最后日志条目的索引值
     */
    private long lastLogIndex;
    /**
     * 候选人的最后日志条目的任期号
     */
    private long lastLogTerm;
}
