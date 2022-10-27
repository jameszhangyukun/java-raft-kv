package com.zyk.raft.kv.constants;

import com.zyk.raft.kv.StateMachine;
import com.zyk.raft.kv.impl.RedisStateMachine;

/**
 * 快找存储类型
 */
public enum StateMachineSaveType {

    REDIS("redis", "Redis存储", new RedisStateMachine()),
    ROCKS_DB("RocksDB", "RocksDB存储", new RedisStateMachine());

    public StateMachine getStateMachine() {
        return this.stateMachine;
    }

    public String typeName;

    public String desc;

    public StateMachine stateMachine;

    StateMachineSaveType(String typeName, String desc, StateMachine stateMachine) {
        this.typeName = typeName;
        this.desc = desc;
        this.stateMachine = stateMachine;
    }


}
