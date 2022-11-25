package com.zyk.raft.kv.entity;

import com.zyk.raft.kv.LifeCycle;
import com.zyk.raft.kv.constants.StateMachineSaveType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * 节点配置
 */
@Getter
@Setter
@ToString
public class NodeConfig {
    /**
     * 自身的port
     */
    private int selfPort;
    /**
     * 所有节点地址
     */
    private List<String> peerAddrs;
    /**
     * 状态机快照存储类型
     */
    public StateMachineSaveType stateMachineSaveType;
}
